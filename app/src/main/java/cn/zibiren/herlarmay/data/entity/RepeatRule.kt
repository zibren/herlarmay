package cn.zibiren.herlarmay.data.entity

/**
 * 重复规则类型。
 * - once：一次性提醒，触发后不再重复
 * - periodic：按固定周期重复（天/周/月）
 * - interval：按时间间隔重复（固定间隔或不等间隔序列）
 */
enum class RepeatType { once, periodic, interval }

/**
 * 周期性单位。
 * - day：天
 * - week：周
 * - month：月（按 30 天近似计算）
 */
enum class PeriodUnit { day, week, month }
enum class IntervalUnit { SECONDS, MINUTES, HOURS }

/**
 * 提醒重复规则（JSON 序列化）。
 *
 * 使用 JSON 格式存储在 ReminderEntity.repeatRuleJson 字段中，
 * 通过 Gson 进行序列化/反序列化。
 * 这种设计便于未来扩展（如添加智能间隔、随机间隔等新类型）。
 *
 * @property type 重复类型
 * @property periodUnit 周期性单位（periodic 类型时使用）
 * @property periodInterval 周期间隔数（如每 3 天中的 3）
 * @property intervalValue 固定间隔数值（interval 类型时使用，与 intervalUnit 配合）
 * @property intervalUnit 固定间隔单位（interval 类型时使用）
 * @property variableIntervals 不等间隔序列（对应 intervalUnit 的单位）（interval 类型时使用）
 * @property variableLoop 不等间隔是否循环
 * @property endTimeMillis 结束时间（可选，如每日 8:00-22:00 的上限）
 * @property daysOfWeek 每周重复的天数（周日=1，周六=7）
 * @property timeOfDaySeconds 每日提醒时间（秒，当天第几秒）
 */
data class RepeatRule(
    val type: RepeatType = RepeatType.once,
    val periodUnit: PeriodUnit? = null,
    val periodInterval: Int? = null,
    val intervalValue: Int? = null,
    val intervalUnit: IntervalUnit? = null,
    val variableIntervals: List<Int>? = null,
    val variableLoop: Boolean = true,
    val endTimeMillis: Long? = null,
    val daysOfWeek: List<Int>? = null,
    val timeOfDaySeconds: Int? = null
)
