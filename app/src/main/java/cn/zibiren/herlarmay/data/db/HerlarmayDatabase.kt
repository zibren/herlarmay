package cn.zibiren.herlarmay.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cn.zibiren.herlarmay.data.dao.ReminderDao
import cn.zibiren.herlarmay.data.entity.ReminderEntity

/**
 * Room 数据库。
 *
 * 单例模式，通过 HerlarmayDatabase.getInstance() 获取实例。
 * 使用 fallbackToDestructiveMigration 简化开发阶段的数据库迁移。
 */
@Database(entities = [ReminderEntity::class], version = 1, exportSchema = false)
@TypeConverters(RepeatRuleConverter::class)
abstract class HerlarmayDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: HerlarmayDatabase? = null

        /** 获取数据库单例 */
        fun getInstance(context: Context): HerlarmayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HerlarmayDatabase::class.java,
                    "herlarmay_database"
                ).fallbackToDestructiveMigration(false).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
