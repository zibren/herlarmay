package cn.zibiren.herlarmay

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import cn.zibiren.herlarmay.core.alarm.AlarmScheduler
import cn.zibiren.herlarmay.navigation.HerlarmayNavGraph
import cn.zibiren.herlarmay.ui.theme.HerlarmayTheme

/**
 * 应用主 Activity。
 *
 * 职责：
 * - 初始化导航图（NavHost）
 * - 提供 AlarmScheduler 和 Database 实例给子界面
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val alarmScheduler = AlarmScheduler(this)

        // Android 12+ 检查精确闹钟权限，未授予则引导用户开启
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmScheduler.hasExactAlarmPermission()) {
            alarmScheduler.openExactAlarmSettings()
        }

        setContent {
            HerlarmayTheme {
                val navController = rememberNavController()
                HerlarmayNavGraph(
                    navController = navController,
                    database = (application as HerlarmayApplication).database,
                    alarmScheduler = alarmScheduler
                )
            }
        }
    }
}
