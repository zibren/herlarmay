package cn.zibiren.herlarmay.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cn.zibiren.herlarmay.data.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

/**
 * 提醒数据访问对象（Room DAO）。
 *
 * 所有数据库操作均为 suspend 函数（Room 自动使用后台线程），
 * getAllReminders() 返回 Flow 以支持响应式 UI 更新。
 */
@Dao
interface ReminderDao {
    /** 获取所有提醒，按下次触发时间升序排列 */
    @Query("SELECT * FROM reminders ORDER BY nextTriggerTimeMillis ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    /** 根据 ID 获取单个提醒 */
    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminder(id: Long): ReminderEntity?

    /** 插入新提醒，返回自动生成的 ID */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    /** 更新已有提醒 */
    @Update
    suspend fun update(reminder: ReminderEntity)

    /** 删除单个提醒 */
    @Delete
    suspend fun delete(reminder: ReminderEntity)

    /** 批量删除提醒 */
    @Delete
    suspend fun deleteAll(reminders: List<ReminderEntity>)

    /** 获取所有已启用的提醒（用于重启恢复） */
    @Query("SELECT * FROM reminders WHERE isEnabled = 1")
    suspend fun getAllEnabled(): List<ReminderEntity>

    /** 获取所有已到期的提醒 */
    @Query("SELECT * FROM reminders WHERE isEnabled = 1 AND nextTriggerTimeMillis <= :now")
    suspend fun getDueReminders(now: Long): List<ReminderEntity>
}
