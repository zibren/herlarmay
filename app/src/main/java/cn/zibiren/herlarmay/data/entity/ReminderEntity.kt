package cn.zibiren.herlarmay.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 提醒数据实体（Room 数据库表）。
 *
 * 对应数据库中的 reminders 表。
 * 所有提醒数据自动持久化到本地 SQLite，退出应用不丢失。
 *
 * @property id 主键，自动生成
 * @property title 提醒标题
 * @property description 提醒详细描述
 * @property startTimeMillis 首次触发日期（时间戳，时间部分固定为 00:00）
 * @property repeatRuleJson 重复规则 JSON（由 RepeatRule 序列化）
 * @property ringtoneUri 自定义铃声 URI（null 表示使用系统默认闹钟铃声）
 * @property vibrate 是否震动
 * @property isEnabled 是否启用（关闭时不触发闹钟）
 * @property nextTriggerTimeMillis 下次触发时间戳（用于 AlarmManager 注册）
 * @property currentStepIndex 不等间隔的当前步进索引（用于序列恢复）
 * @property createdAt 创建时间
 */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val startTimeMillis: Long,
    val repeatRuleJson: String,
    val ringtoneUri: String? = null,
    val vibrate: Boolean = true,
    val isEnabled: Boolean = true,
    val nextTriggerTimeMillis: Long? = null,
    val currentStepIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
