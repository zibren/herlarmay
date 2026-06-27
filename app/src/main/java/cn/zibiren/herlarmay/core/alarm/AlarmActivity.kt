package cn.zibiren.herlarmay.core.alarm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import cn.zibiren.herlarmay.data.db.HerlarmayDatabase
import cn.zibiren.herlarmay.data.entity.ReminderEntity
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全屏闹钟提醒 Activity。
 *
 * 特点：
 * - 即使锁屏也全屏显示（showWhenLocked + turnScreenOn）
 * - 保持屏幕常亮（KEEP_SCREEN_ON）
 * - 显示提醒标题、描述、当前时间
 * - 提供"确认"和"稍后提醒（5/10/30 分钟）"按钮
 * - Android 13+ 请求 POST_NOTIFICATIONS 权限（用于前台服务通知）
 */
class AlarmActivity : ComponentActivity() {
    private var reminder: ReminderEntity? = null

    // 通知权限请求（Android 13+，前台服务需要）
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("AlarmActivity", "onCreate")

        // 请求通知权限（Android 13+ 需要才能显示前台服务通知）
        requestNotificationPermission()

        // 锁屏显示 + 点亮屏幕 + 保持常亮
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 获取提醒数据
        val reminderId = intent.getLongExtra("reminder_id", -1)
        if (reminderId != -1L) {
            val db = HerlarmayDatabase.getInstance(this)
            reminder = runBlocking { db.reminderDao().getReminder(reminderId) }
        }

        // 使用 Compose 构建全屏提醒界面
        setContent {
            MaterialTheme {
                AlarmScreen(
                    reminder = reminder,
                    onDismiss = { finishAlarm() },
                    onSnooze = { minutes -> snooze(minutes) },
                    onClear = { clearReminder() }
                )
            }
        }
    }

    /** 确认并关闭闹钟 */
    private fun finishAlarm() {
        AlarmService.stop(this)  // 停止前台服务（停止铃声和震动）
        finish()
    }

    /**
     * 稍后提醒。
     * 将下次触发时间设为 N 分钟后，重新注册闹钟，然后关闭当前提醒。
     */
    private fun snooze(minutes: Int) {
        val r = reminder ?: return
        val newTrigger = System.currentTimeMillis() + minutes * 60000L
        val db = HerlarmayDatabase.getInstance(this)
        val resetReminder = r.copy(
            nextTriggerTimeMillis = newTrigger,
            currentStepIndex = 0  // 稍后提醒重置不等间隔序列
        )
        runBlocking {
            db.reminderDao().update(resetReminder)
        }
        AlarmScheduler(this).schedule(resetReminder)
        AlarmService.stop(this)
        finish()
    }

    /**
     * 清除此提醒：停止闹钟、取消所有未来触发、从数据库删除。
     */
    private fun clearReminder() {
        val r = reminder ?: return
        AlarmScheduler(this).cancel(r)  // 取消已注册的闹钟
        val db = HerlarmayDatabase.getInstance(this)
        runBlocking { db.reminderDao().delete(r) }
        AlarmService.stop(this)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        AlarmService.stop(this)  // 确保 Activity 关闭时停止服务
    }

    /** 请求 POST_NOTIFICATIONS 权限（仅 Android 13+） */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

/**
 * 全屏提醒界面的 Composable。
 *
 * 显示：提醒标题、描述、当前时间
 * 按钮：确认、稍后提醒（5/10/30 分钟）、清除任务
 */
@Composable
private fun AlarmScreen(
    reminder: ReminderEntity?,
    onDismiss: () -> Unit,
    onSnooze: (Int) -> Unit,
    onClear: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 提醒标题
        Text(
            text = reminder?.title ?: "闹钟",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        // 提醒描述
        if (!reminder?.description.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = reminder?.description ?: "",
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 当前时间
        Spacer(Modifier.height(8.dp))
        Text(
            text = dateFormat.format(Date()),
            fontSize = 48.sp,
            fontWeight = FontWeight.Light
        )

        Spacer(Modifier.height(48.dp))

        // 确认按钮
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("确认", fontSize = 18.sp)
        }

        Spacer(Modifier.height(16.dp))

        // 稍后提醒按钮组
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(5, 10, 30).forEach { minutes ->
                OutlinedButton(onClick = { onSnooze(minutes) }) {
                    Text("${minutes} 分钟后")
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 清除任务：停止闹钟并从数据库删除该提醒
        TextButton(onClick = onClear) {
            Text("清除此任务", color = Color(0xFFB00020))
        }
    }
}
