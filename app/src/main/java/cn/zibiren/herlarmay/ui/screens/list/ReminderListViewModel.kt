package cn.zibiren.herlarmay.ui.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.zibiren.herlarmay.core.alarm.AlarmScheduler
import cn.zibiren.herlarmay.data.entity.ReminderEntity
import cn.zibiren.herlarmay.data.repository.ReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 提醒列表界面的状态定义。
 *
 * @property reminders 当前所有提醒列表
 * @property isLoading 是否正在加载
 */
data class ReminderListUiState(
    val reminders: List<ReminderEntity> = emptyList(),
    val isLoading: Boolean = true,
    val selectedIds: Set<Long> = emptySet()       // 批量选择时选中的提醒 ID
)

/**
 * 提醒列表界面的 ViewModel。
 *
 * 职责：
 * - 通过 Repository 监听 Room 数据库变化（Flow）
 * - 处理启用/禁用切换
 * - 处理滑出删除
 * - 每次操作后自动更新 AlarmManager 闹钟注册
 */
class ReminderListViewModel(
    private val repository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReminderListUiState())
    val uiState: StateFlow<ReminderListUiState> = _uiState.asStateFlow()

    init {
        // 监听数据库变化，自动更新 UI
        viewModelScope.launch {
            repository.getAllReminders().collect { reminders ->
                _uiState.update { it.copy(reminders = reminders, isLoading = false) }
            }
        }
    }

    /** 切换提醒的启用/禁用状态，同时注册或取消系统闹钟 */
    fun toggleEnabled(reminder: ReminderEntity) {
        viewModelScope.launch {
            val updated = reminder.copy(isEnabled = !reminder.isEnabled)
            val saved = repository.save(updated)
            if (saved.isEnabled && saved.nextTriggerTimeMillis != null) {
                alarmScheduler.schedule(saved)
            } else {
                alarmScheduler.cancel(saved)
            }
        }
    }

    /** 删除提醒并取消已注册的系统闹钟 */
    fun delete(reminder: ReminderEntity) {
        viewModelScope.launch {
            alarmScheduler.cancel(reminder)
            repository.delete(reminder)
        }
    }

    // ---- 批量选择 ----

    val isSelectionMode: Boolean get() = _uiState.value.selectedIds.isNotEmpty()

    /** 切换某个提醒的选中状态 */
    fun toggleSelection(id: Long) {
        _uiState.update {
            val newSet = it.selectedIds.toMutableSet()
            if (newSet.contains(id)) newSet.remove(id) else newSet.add(id)
            it.copy(selectedIds = newSet)
        }
    }

    /** 全选/取消全选 */
    fun selectAll() {
        _uiState.update {
            if (it.selectedIds.size == it.reminders.size) {
                it.copy(selectedIds = emptySet())
            } else {
                it.copy(selectedIds = it.reminders.map { r -> r.id }.toSet())
            }
        }
    }

    /** 退出选择模式 */
    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    /** 批量删除已选中的提醒 */
    fun deleteSelected() {
        viewModelScope.launch {
            val toDelete = _uiState.value.reminders.filter { it.id in _uiState.value.selectedIds }
            toDelete.forEach { alarmScheduler.cancel(it) }
            repository.deleteAll(toDelete)
            _uiState.update { it.copy(selectedIds = emptySet()) }
        }
    }

    /** ViewModel 工厂 */
    class Factory(
        private val repository: ReminderRepository,
        private val alarmScheduler: AlarmScheduler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReminderListViewModel(repository, alarmScheduler) as T
        }
    }
}
