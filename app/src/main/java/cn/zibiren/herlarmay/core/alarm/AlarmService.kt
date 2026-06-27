package cn.zibiren.herlarmay.core.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.Settings
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import cn.zibiren.herlarmay.data.db.HerlarmayDatabase
import cn.zibiren.herlarmay.data.entity.ReminderEntity
import cn.zibiren.herlarmay.data.entity.RepeatRule
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking

/**
 * 闹钟前台服务。
 *
 * 生命周期：
 * - AlarmScheduler 的 PendingIntent.getService() 直接触发 onStartCommand
 * - 创建持续通知（Notification），确保进程存活
 * - 从数据库读取提醒，计算下次触发时间，注册下一个闹钟
 * - 播放铃声（MediaPlayer 闹钟音量通道，循环播放）
 * - 震动（自定义震动模式）
 * - 启动 AlarmActivity 全屏显示
 * - 用户确认或稍后提醒时通过 AlarmService.stop() 停止
 *
 * 使用 startForeground() 提高进程存活率，防止系统在低内存时杀死。
 * 使用 PendingIntent.getService 而非 BroadcastReceiver，提升 MIUI 兼容性。
 */
class AlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null  // 铃声播放器
    private var vibrator: Vibrator? = null        // 震动器
    @Volatile private var isStopping = false      // 防止后台线程在停止后继续播放

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // 获取震动器（Android 12+ 使用 VibratorManager）
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 必须在任何检查之前立即调用 startForeground，
        // 否则系统抛出 ForegroundServiceDidNotStartInTimeException
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("闹钟")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setOngoing(true)
            .build())

        val reminderId = intent?.getLongExtra("reminder_id", -1) ?: -1
        Log.i("AlarmService", "onStartCommand reminderId=$reminderId")
        if (reminderId == -1L) return START_STICKY

        // 在后台线程执行数据库读写 + 下次触发计算
        Thread {
            try {
                val db = HerlarmayDatabase.getInstance(this)
                val reminder = runBlocking { db.reminderDao().getReminder(reminderId) }
                if (reminder == null) {
                    Log.w("AlarmService", "reminder $reminderId not found, stopping")
                    stopSelf()
                    return@Thread
                }
                if (!reminder.isEnabled) {
                    Log.i("AlarmService", "reminder $reminderId disabled, stopping")
                    stopSelf()
                    return@Thread
                }

                Log.i("AlarmService", "firing reminder id=$reminderId title=${reminder.title}")

                // 解析 JSON 重复规则
                val rule = Gson().fromJson(reminder.repeatRuleJson, RepeatRule::class.java)

                // 计算下次触发时间和步进索引
                val baseTime = reminder.nextTriggerTimeMillis ?: reminder.startTimeMillis
                val currentTime = maxOf(baseTime, System.currentTimeMillis())
                val (nextTrigger, nextIndex) = NextTriggerCalculator.calculateNextTrigger(
                    currentTime,
                    rule,
                    reminder.currentStepIndex
                )

                // 更新数据库中的下次触发时间和步进索引
                val updated = reminder.copy(
                    nextTriggerTimeMillis = nextTrigger,
                    currentStepIndex = nextIndex
                )
                runBlocking { db.reminderDao().update(updated) }

                // 如果有下次触发时间，注册新闹钟
                if (nextTrigger != null) {
                    AlarmScheduler(this).schedule(updated)
                }

                // 启动全屏提醒 Activity
                val alarmIntent = Intent(this, AlarmActivity::class.java).apply {
                    this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("reminder_id", reminder.id)
                }
                startActivity(alarmIntent)

                // 切回主线程播放铃声和震动
                Handler(mainLooper).post {
                    if (isStopping) return@post

                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NOTIFICATION_ID, buildNotification(reminder))

                    playRingtone(reminder.ringtoneUri)
                    if (reminder.vibrate) {
                        startVibrating()
                    }
                }
            } catch (e: Exception) {
                Log.e("AlarmService", "error in onStartCommand", e)
                stopSelf()
            }
        }.start()

        return START_STICKY  // 系统会在可用时重启服务
    }

    /** 使用 MediaPlayer 播放闹钟铃声（闹钟音量通道，循环播放） */
    private fun playRingtone(uri: String?) {
        Log.i("AlarmService", "playRingtone uri=$uri")
        try {
            val ringtoneUri = if (uri != null) {
                Uri.parse(uri)
            } else {
                // 默认使用系统闹钟铃声
                Settings.System.DEFAULT_ALARM_ALERT_URI
            }
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmService, ringtoneUri)
                // 使用闹钟音量通道，与媒体音量独立
                setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                isLooping = true  // 持续响铃直到用户关闭
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** 开始震动（交替震动模式） */
    private fun startVibrating() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = VibrationEffect.createWaveform(
                longArrayOf(0, 500, 500, 500),  // 等待、震动、暂停、震动
                intArrayOf(0, 255, 0, 255),      // 振幅
                -1                                // 不重复（用 -1 表示单次模式，实际由 AlarmActivity 控制停止）
            )
            vibrator?.vibrate(pattern)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 500, 500), 0)  // 老版本 API
        }
    }

    /**
     * 构建持续通知。
     * - 显示提醒标题和描述
     * - 点击通知打开 AlarmActivity（全屏提醒界面）
     * - 设置为持续通知（用户无法直接划掉）
     */
    private fun buildNotification(reminder: ReminderEntity): Notification {
        // 点击通知时打开 AlarmActivity
        val alarmIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("reminder_id", reminder.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val alarmPendingIntent = PendingIntent.getActivity(
            this, reminder.id.toInt(), alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(reminder.title)                                    // 标题
            .setContentText(reminder.description.ifBlank { "Reminder" })        // 描述（无描述时显示默认文字）
            .setSmallIcon(android.R.drawable.ic_dialog_alert)                   // 小图标
            .setPriority(NotificationCompat.PRIORITY_MAX)                       // 最高优先级
            .setCategory(NotificationCompat.CATEGORY_ALARM)                     // 闹钟类别
            .setFullScreenIntent(alarmPendingIntent, true)                      // 锁屏全屏显示
            .setContentIntent(alarmPendingIntent)                                // 点击通知打开提醒界面
            .setOngoing(true)                                                   // 持续通知，不可划掉
            .build()
    }

    /** 创建通知渠道（Android 8+ 必须） */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID, "闹钟", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Herlarmay 闹钟通知"
            // 通知本身无声音，声音由 MediaPlayer 直接播放
            setSound(null, null)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** 服务销毁时释放资源 */
    override fun onDestroy() {
        isStopping = true  // 阻止后台线程在销毁后继续播放
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "herlarmay_alarm"
        const val NOTIFICATION_ID = 1001

        /** 停止闹钟服务的静态方法 */
        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmService::class.java))
        }
    }
}
