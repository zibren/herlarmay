package cn.zibiren.herlarmay.core.alarm

import cn.zibiren.herlarmay.data.entity.IntervalUnit
import cn.zibiren.herlarmay.data.entity.PeriodUnit
import cn.zibiren.herlarmay.data.entity.RepeatRule
import cn.zibiren.herlarmay.data.entity.RepeatType
import java.util.Calendar

object NextTriggerCalculator {

    fun calculateFirstTrigger(saveTimeMillis: Long, rule: RepeatRule): Long {
        if (rule.type == RepeatType.interval) {
            return saveTimeMillis
        }
        val timeOfDaySeconds = rule.timeOfDaySeconds ?: 0
        val daysOfWeek = rule.daysOfWeek

        val cal = Calendar.getInstance().apply {
            timeInMillis = saveTimeMillis
            set(Calendar.HOUR_OF_DAY, timeOfDaySeconds / 3600)
            set(Calendar.MINUTE, (timeOfDaySeconds % 3600) / 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (daysOfWeek.isNullOrEmpty()) {
            return if (cal.timeInMillis > saveTimeMillis) cal.timeInMillis
            else cal.apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
        }

        repeat(8) { offset ->
            val testCal = Calendar.getInstance().apply {
                timeInMillis = saveTimeMillis
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, timeOfDaySeconds / 3600)
                set(Calendar.MINUTE, (timeOfDaySeconds % 3600) / 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (offset > 0 || testCal.timeInMillis > saveTimeMillis) {
                if (isoDayOfWeek(testCal) in daysOfWeek) {
                    return testCal.timeInMillis
                }
            }
        }
        return saveTimeMillis + 7 * 24 * 60 * 60 * 1000L
    }

    fun calculateNextTrigger(
        currentTriggerTimeMillis: Long,
        rule: RepeatRule,
        currentStepIndex: Int = 0
    ): Pair<Long?, Int> {
        val (rawTime, nextIndex) = calculateRaw(currentTriggerTimeMillis, rule, currentStepIndex)
        val adjustedTime = rawTime?.let { advanceToSelectedDay(it, rule.daysOfWeek) }
        return Pair(adjustedTime, nextIndex)
    }

    private fun calculateRaw(
        time: Long, rule: RepeatRule, stepIndex: Int
    ): Pair<Long?, Int> = when (rule.type) {
        RepeatType.once -> Pair(null, 0)

        RepeatType.periodic -> {
            val unitMillis = when (rule.periodUnit) {
                PeriodUnit.day -> 86_400_000L
                PeriodUnit.week -> 604_800_000L
                PeriodUnit.month -> 2_592_000_000L
                null -> 86_400_000L
            }
            val interval = (rule.periodInterval ?: 1).toLong()
            Pair(time + unitMillis * interval, 0)
        }

        RepeatType.interval -> {
            if (!rule.variableIntervals.isNullOrEmpty()) {
                val intervals = rule.variableIntervals
                if (stepIndex < intervals.size) {
                    Pair(time + intervals[stepIndex].toLong() * 60_000, stepIndex + 1)
                } else if (rule.variableLoop) {
                    Pair(time + intervals[0].toLong() * 60_000, 1)
                } else {
                    Pair(null, 0)
                }
            } else if (rule.intervalValue != null && rule.intervalUnit != null) {
                val unitMillis = when (rule.intervalUnit) {
                    IntervalUnit.SECONDS -> 1_000L
                    IntervalUnit.MINUTES -> 60_000L
                    IntervalUnit.HOURS -> 3_600_000L
                }
                Pair(time + rule.intervalValue.toLong() * unitMillis, 0)
            } else {
                Pair(null, 0)
            }
        }
    }

    private fun advanceToSelectedDay(time: Long, daysOfWeek: List<Int>?): Long {
        if (daysOfWeek.isNullOrEmpty()) return time
        repeat(8) { offset ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = time
                add(Calendar.DAY_OF_YEAR, offset)
            }
            if (isoDayOfWeek(cal) in daysOfWeek) {
                return cal.timeInMillis
            }
        }
        return time
    }

    private fun isoDayOfWeek(cal: Calendar): Int = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> 7
        else -> cal.get(Calendar.DAY_OF_WEEK) - 1
    }
}
