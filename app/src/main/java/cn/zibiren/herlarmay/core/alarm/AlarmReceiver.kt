@file:Suppress("DEPRECATION")

package cn.zibiren.herlarmay.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * 闹钟广播接收器（旧）。
 *
 * 保留作为旧 PendingIntent 的兼容转发器。
 * 新的 AlarmManager 注册直接使用 PendingIntent.getService 启动 AlarmService，
 * 不再经过此 BroadcastReceiver，以提高 MIUI 等系统的兼容性。
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminder_id", -1)
        Log.i("AlarmReceiver", "legacy onReceive reminderId=$reminderId, forwarding to AlarmService")
        if (reminderId == -1L) return

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("reminder_id", reminderId)
            putExtra("action", "trigger")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
