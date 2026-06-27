package cn.zibiren.herlarmay.ui.screens.edit

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.zibiren.herlarmay.core.alarm.NextTriggerCalculator
import cn.zibiren.herlarmay.core.alarm.AlarmScheduler
import cn.zibiren.herlarmay.data.entity.IntervalUnit
import cn.zibiren.herlarmay.data.entity.PeriodUnit
import cn.zibiren.herlarmay.data.entity.RepeatRule
import cn.zibiren.herlarmay.data.entity.RepeatType
import cn.zibiren.herlarmay.data.entity.ReminderEntity
import cn.zibiren.herlarmay.data.repository.ReminderRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class EditReminderUiState(
    val title: String = "",
    val description: String = "",
    val selectedDays: Set<Int> = emptySet(),
    val triggerHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    val triggerMinute: Int = (Calendar.getInstance().get(Calendar.MINUTE) + 1) % 60,
    val repeatType: RepeatType = RepeatType.once,
    val periodUnit: PeriodUnit = PeriodUnit.day,
    val periodInterval: Int = 1,
    val intervalValue: String = "30",
    val intervalUnit: IntervalUnit = IntervalUnit.SECONDS,
    val variableIntervals: List<Int> = emptyList(),
    val variableLoop: Boolean = true,
    val showVariableInterval: Boolean = false,
    val ringtoneUri: String? = null,
    val vibrate: Boolean = true,
    val isEditing: Boolean = false,
    val isLoading: Boolean = false
)

/**
 * 编辑提醒界面的 ViewModel。
 *
 * 负责：
 * - 管理界面状态（EditReminderUiState）
 * - 加载已有提醒数据（编辑模式）
 * - 构建 ReminderEntity 并保存到数据库
 * - 保存后通过 AlarmScheduler 注册系统闹钟
 */
class EditReminderViewModel(
    private val reminderId: Long,
    private val repository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditReminderUiState())
    val uiState: StateFlow<EditReminderUiState> = _uiState.asStateFlow()

    init {
        if (reminderId != -1L) {
            loadReminder(reminderId)  // 编辑模式：加载已有数据
        }
    }

    /** 加载已有提醒数据到界面 */
    private fun loadReminder(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val reminder = repository.getReminder(id) ?: return@launch
            val rule = Gson().fromJson(reminder.repeatRuleJson, RepeatRule::class.java)
            val tod = rule.timeOfDaySeconds ?: 0
            _uiState.update {
                it.copy(
                    title = reminder.title,
                    description = reminder.description,
                    selectedDays = rule.daysOfWeek?.toSet() ?: emptySet(),
                    triggerHour = tod / 3600,
                    triggerMinute = (tod % 3600) / 60,
                    repeatType = rule.type,
                    periodUnit = rule.periodUnit ?: PeriodUnit.day,
                    periodInterval = rule.periodInterval ?: 1,
                    intervalValue = rule.intervalValue?.toString() ?: "30",
                    intervalUnit = rule.intervalUnit ?: IntervalUnit.SECONDS,
                    variableIntervals = rule.variableIntervals ?: emptyList(),
                    variableLoop = rule.variableLoop,
                    ringtoneUri = reminder.ringtoneUri,
                    vibrate = reminder.vibrate,
                    isEditing = true,
                    isLoading = false
                )
            }
        }
    }

    // ---- 状态更新方法 ----
    fun updateTitle(title: String) { _uiState.update { it.copy(title = title) } }
    fun updateDescription(desc: String) { _uiState.update { it.copy(description = desc) } }
    fun updateRepeatType(type: RepeatType) { _uiState.update { it.copy(repeatType = type) } }
    fun updatePeriodUnit(unit: PeriodUnit) { _uiState.update { it.copy(periodUnit = unit) } }
    fun updatePeriodInterval(interval: Int) { _uiState.update { it.copy(periodInterval = interval) } }
    fun updateIntervalUnit(unit: IntervalUnit) { _uiState.update { it.copy(intervalUnit = unit) } }
    fun updateVariableLoop(loop: Boolean) { _uiState.update { it.copy(variableLoop = loop) } }
    fun updateVibrate(v: Boolean) { _uiState.update { it.copy(vibrate = v) } }
    fun updateRingtoneUri(uri: String?) { _uiState.update { it.copy(ringtoneUri = uri) } }
    fun toggleDay(day: Int) {
        _uiState.update {
            val newSet = it.selectedDays.toMutableSet()
            if (day in newSet) newSet.remove(day) else newSet.add(day)
            it.copy(selectedDays = newSet)
        }
    }
    fun updateTriggerHour(hour: Int) { _uiState.update { it.copy(triggerHour = hour) } }
    fun updateTriggerMinute(minute: Int) { _uiState.update { it.copy(triggerMinute = minute) } }
    fun updateIntervalValue(value: String) { _uiState.update { it.copy(intervalValue = value) } }
    fun toggleVariableInterval() { _uiState.update { it.copy(showVariableInterval = !it.showVariableInterval) } }

    /** 添加一个不等间隔项 */
    fun addVariableInterval(minutes: Int) {
        _uiState.update { it.copy(variableIntervals = it.variableIntervals + minutes) }
    }

    /** 删除指定位置的不等间隔项 */
    fun removeVariableInterval(index: Int) {
        _uiState.update {
            it.copy(variableIntervals = it.variableIntervals.toMutableList().also { list ->
                if (index in list.indices) list.removeAt(index)
            })
        }
    }

    private fun buildReminder(): ReminderEntity {
        val state = _uiState.value
        val now = System.currentTimeMillis()
        val rule = RepeatRule(
            type = state.repeatType,
            daysOfWeek = state.selectedDays.toList().ifEmpty { null },
            timeOfDaySeconds = state.triggerHour * 3600 + state.triggerMinute * 60,
            periodUnit = if (state.repeatType == RepeatType.periodic) state.periodUnit else null,
            periodInterval = if (state.repeatType == RepeatType.periodic) state.periodInterval else null,
            intervalValue = if (state.repeatType == RepeatType.interval) state.intervalValue.toIntOrNull() else null,
            intervalUnit = if (state.repeatType == RepeatType.interval) state.intervalUnit else null,
            variableIntervals = if (state.repeatType == RepeatType.interval && state.variableIntervals.isNotEmpty()) state.variableIntervals else null,
            variableLoop = if (state.repeatType == RepeatType.interval) state.variableLoop else true
        )
        return ReminderEntity(
            id = if (state.isEditing) reminderId else 0,
            title = state.title,
            description = state.description,
            startTimeMillis = now,
            repeatRuleJson = Gson().toJson(rule),
            ringtoneUri = state.ringtoneUri,
            vibrate = state.vibrate,
            isEnabled = true,
            nextTriggerTimeMillis = NextTriggerCalculator.calculateFirstTrigger(now, rule),
            currentStepIndex = 0
        )
    }

    /**
     * 保存提醒。
     * 1. 构建 ReminderEntity
     * 2. 写入 Room 数据库
     * 3. 通过 AlarmScheduler 注册系统闹钟
     * 4. 回调 onSaved 以关闭界面
     */
    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            val reminder = buildReminder()
            val saved = repository.save(reminder)  // insert 后拿到真实 ID
            Log.i("EditReminderVM", "save id=${saved.id} enabled=${saved.isEnabled} nextTrigger=${saved.nextTriggerTimeMillis}")
            // 只有启用的提醒才注册系统闹钟
            if (saved.isEnabled && saved.nextTriggerTimeMillis != null) {
                alarmScheduler.schedule(saved)     // 用带真实 ID 的实体注册
            }
            onSaved()
        }
    }

    /** ViewModel 工厂，用于注入依赖 */
    class Factory(
        private val reminderId: Long,
        private val repository: ReminderRepository,
        private val alarmScheduler: AlarmScheduler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditReminderViewModel(reminderId, repository, alarmScheduler) as T
        }
    }
}
