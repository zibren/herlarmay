package cn.zibiren.herlarmay.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import cn.zibiren.herlarmay.data.db.HerlarmayDatabase
import kotlinx.coroutines.runBlocking

/**
 * 系统启动广播接收器。
 *
 * 监听 ACTION_BOOT_COMPLETED，当设备重启后：
 * 1. 从 Room 数据库读取所有已启用的提醒
 * 2. 为每个有下次触发时间的提醒重新注册 AlarmManager 闹钟
 *
 * 使用 goAsync() + 后台线程，避免主线程阻塞导致 ANR。
 * 这是确保重启后提醒依然有效的关键机制。
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.i("BootReceiver", "BOOT_COMPLETED received")

        val pendingResult = goAsync()
        Thread {
            try {
                val db = HerlarmayDatabase.getInstance(context)
                val enabledReminders = runBlocking { db.reminderDao().getAllEnabled() }
                Log.i("BootReceiver", "found ${enabledReminders.size} enabled reminders")
                val scheduler = AlarmScheduler(context)
                enabledReminders.forEach { reminder ->
                    if (reminder.nextTriggerTimeMillis != null) {
                        Log.i("BootReceiver", "rescheduling reminder id=${reminder.id} nextTrigger=${reminder.nextTriggerTimeMillis}")
                        scheduler.schedule(reminder)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }.start()
    }
}
