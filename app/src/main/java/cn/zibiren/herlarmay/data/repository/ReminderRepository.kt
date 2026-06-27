package cn.zibiren.herlarmay.data.repository

import cn.zibiren.herlarmay.data.dao.ReminderDao
import cn.zibiren.herlarmay.data.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

/**
 * 提醒数据仓库。
 *
 * 封装 ReminderDao，提供统一的数据访问接口。
 * 上层（ViewModel）通过此类操作数据，不直接访问 DAO。
 */
class ReminderRepository(private val dao: ReminderDao) {
    /** 获取所有提醒（响应式 Flow） */
    fun getAllReminders(): Flow<List<ReminderEntity>> = dao.getAllReminders()

    /** 根据 ID 获取提醒 */
    suspend fun getReminder(id: Long): ReminderEntity? = dao.getReminder(id)

    /** 保存提醒（自动判断插入或更新），返回带真实 ID 的实体 */
    suspend fun save(reminder: ReminderEntity): ReminderEntity {
        return if (reminder.id == 0L) {
            val newId = dao.insert(reminder)
            reminder.copy(id = newId)
        } else {
            dao.update(reminder)
            reminder
        }
    }

    /** 删除单个提醒 */
    suspend fun delete(reminder: ReminderEntity) = dao.delete(reminder)

    /** 批量删除提醒 */
    suspend fun deleteAll(reminders: List<ReminderEntity>) = dao.deleteAll(reminders)

    /** 获取所有已启用提醒 */
    suspend fun getAllEnabled(): List<ReminderEntity> = dao.getAllEnabled()

    /** 获取已到期的提醒 */
    suspend fun getDueReminders(now: Long): List<ReminderEntity> = dao.getDueReminders(now)
}
