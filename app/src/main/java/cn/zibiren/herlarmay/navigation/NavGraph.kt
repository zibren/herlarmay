package cn.zibiren.herlarmay.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.zibiren.herlarmay.core.alarm.AlarmScheduler
import cn.zibiren.herlarmay.data.db.HerlarmayDatabase
import cn.zibiren.herlarmay.data.repository.ReminderRepository
import cn.zibiren.herlarmay.ui.screens.edit.EditReminderScreen
import cn.zibiren.herlarmay.ui.screens.edit.EditReminderViewModel
import cn.zibiren.herlarmay.ui.screens.list.ReminderListScreen
import cn.zibiren.herlarmay.ui.screens.list.ReminderListViewModel

/**
 * 导航路由定义。
 */
object Routes {
    const val LIST = "list"           // 提醒列表主页
    const val EDIT = "edit/{id}"       // 创建/编辑提醒（id=-1 表示新建）
    fun edit(id: Long) = "edit/$id"
}

/**
 * 应用导航图。
 *
 * 使用 Compose Navigation 管理页面切换。
 * 包含两个路由：
 * - list：提醒列表主页
 * - edit/{id}：创建（id=-1）或编辑提醒页面
 *
 * 注意：全屏闹钟提醒（AlarmActivity）是独立 Activity，不在此导航图中。
 */
@Composable
fun HerlarmayNavGraph(
    navController: NavHostController,
    database: HerlarmayDatabase,
    alarmScheduler: AlarmScheduler
) {
    val repository = ReminderRepository(database.reminderDao())

    NavHost(navController = navController, startDestination = Routes.LIST) {

        // 提醒列表主页
        composable(Routes.LIST) {
            val vm: ReminderListViewModel = viewModel(
                factory = ReminderListViewModel.Factory(repository, alarmScheduler)
            )
            ReminderListScreen(
                viewModel = vm,
                onNavigateToCreate = { navController.navigate(Routes.edit(-1L)) },
                onNavigateToEdit = { id -> navController.navigate(Routes.edit(id)) }
            )
        }

        // 创建/编辑提醒页面
        composable(
            route = Routes.EDIT,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: -1L
            val vm: EditReminderViewModel = viewModel(
                factory = EditReminderViewModel.Factory(id, repository, alarmScheduler)
            )
            EditReminderScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
