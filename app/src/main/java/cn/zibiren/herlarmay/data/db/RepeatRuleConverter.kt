package cn.zibiren.herlarmay.data.db

import androidx.room.TypeConverter
import cn.zibiren.herlarmay.data.entity.RepeatRule
import com.google.gson.Gson

/**
 * Room 类型转换器。
 *
 * 将 RepeatRule 对象与 JSON 字符串相互转换，
 * 使 Room 可以存储复杂对象到单个数据库字段。
 */
class RepeatRuleConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromRepeatRule(value: RepeatRule): String = gson.toJson(value)

    @TypeConverter
    fun toRepeatRule(value: String): RepeatRule =
        gson.fromJson(value, RepeatRule::class.java)
}
