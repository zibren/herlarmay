package cn.zibiren.herlarmay.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import cn.zibiren.herlarmay.data.entity.ReminderEntity

/**
 * 闹钟调度器，封装 AlarmManager。
 *
 * 核心逻辑：
 * - 使用 setAlarmClock 作为首选（最高优先级，MIUI 兼容性最好）
 * - Android 12+ 需要 SCHEDULE_EXACT_ALARM 权限才能设置精确闹钟
 * - 如果没有权限，降级为 setAndAllowWhileIdle（非精确但系统会尽量按时触发）
 * - PendingIntent 携带 reminder.id，FLAG_IMMUTABLE 保证 Android 12+ 安全
 */
class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * 注册一个提醒的精确闹钟。
     * 使用 PendingIntent.getService 直接启动 AlarmService，
     * 避免 MIUI 等系统拦截广播（BroadcastReceiver）。
     * 优先级：setAlarmClock → setExactAndAllowWhileIdle → setAndAllowWhileIdle
     * 如果触发时间已过去，自动推迟 1 秒，防止立即重触发形成死循环。
     */
    fun schedule(reminder: ReminderEntity) {
        val triggerTime = maxOf(reminder.nextTriggerTimeMillis ?: return, System.currentTimeMillis() + 1000)
        val exact = hasExactAlarmPermission()

        Log.i("AlarmScheduler", "schedule id=${reminder.id} triggerTime=$triggerTime exact=$exact")

        // 创建指向 AlarmService 的 PendingIntent，以 reminder.id 区分不同的闹钟
        val intent = Intent(context, AlarmService::class.java).apply {
            putExtra("reminder_id", reminder.id)
            putExtra("action", "trigger")
        }
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                context,
                reminder.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            @Suppress("DEPRECATION")
            PendingIntent.getService(
                context,
                reminder.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // 优先级 1：setAlarmClock（API 21+，MIUI 兼容性最好）
        if (exact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                pendingIntent
            )
        } else if (exact) {
            // 优先级 2：setExactAndAllowWhileIdle（API 19+）精确触发
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            // 优先级 3：setAndAllowWhileIdle（API 19+）非精确降级
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    /** 取消指定提醒的已注册闹钟 */
    fun cancel(reminder: ReminderEntity) {
        val intent = Intent(context, AlarmService::class.java)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                context,
                reminder.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            @Suppress("DEPRECATION")
            PendingIntent.getService(
                context,
                reminder.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /** 批量重新注册（设备启动后恢复所有闹钟） */
    fun rescheduleAll(reminders: List<ReminderEntity>) {
        reminders.forEach { schedule(it) }
    }

    /**
     * 检查是否持有精确闹钟权限。
     * Android 12+ 必须使用 AlarmManager.canScheduleExactAlarms()，
     * checkSelfPermission 对特殊权限不适用。
     */
    fun hasExactAlarmPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /** 打开系统精确闹钟权限设置页 */
    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.startActivity(Intent(
                android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
            ).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}

