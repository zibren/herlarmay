package cn.zibiren.herlarmay

import android.app.Application
import cn.zibiren.herlarmay.data.db.HerlarmayDatabase

/**
 * 应用 Application 类。
 *
 * 提供数据库实例的懒加载单例，供整个应用使用。
 */
class HerlarmayApplication : Application() {
    val database: HerlarmayDatabase by lazy { HerlarmayDatabase.getInstance(this) }
}
