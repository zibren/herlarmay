package cn.zibiren.herlarmay.ui.screens.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.zibiren.herlarmay.ui.components.ReminderCard

/**
 * 提醒列表主界面。
 *
 * 功能：
 * - 显示所有提醒的 LazyColumn 列表
 * - 每张卡片显示标题、类型、下次触发时间、启用开关
 * - 左滑删除
 * - FAB 按钮创建新提醒
 * - 点击卡片编辑提醒
 * - 空状态提示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    viewModel: ReminderListViewModel = viewModel(),
    onNavigateToCreate: () -> Unit = {},
    onNavigateToEdit: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    val isSelectionMode = uiState.selectedIds.isNotEmpty()

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                // 选择模式：显示选中数量 + 全选 + 取消
                TopAppBar(
                    title = { Text("已选 ${uiState.selectedIds.size} 项") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "取消选择")
                        }
                    },
                    actions = {
                        TextButton(onClick = { viewModel.selectAll() }) {
                            Text(if (uiState.selectedIds.size == uiState.reminders.size) "取消全选" else "全选")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Herlarmay") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(onClick = onNavigateToCreate) {
                    Icon(Icons.Default.Add, contentDescription = "新建提醒")
                }
            }
        },
        bottomBar = {
            if (isSelectionMode) {
                BottomAppBar {
                    Button(
                        onClick = { viewModel.deleteSelected() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        enabled = uiState.selectedIds.isNotEmpty()
                    ) {
                        Text("删除选中（${uiState.selectedIds.size}）")
                    }
                }
            }
        }
    ) { padding ->
        when {
            // 加载中：显示进度圈
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            // 空列表：显示提示文字
            uiState.reminders.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无提醒。\n点击 + 创建第一个提醒。",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // 有数据：显示提醒列表
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.reminders, key = { it.id }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onToggleEnabled = { viewModel.toggleEnabled(it) },
                            onClick = { onNavigateToEdit(reminder.id) },
                            onDelete = { viewModel.delete(reminder) },
                            isSelectionMode = isSelectionMode,
                            isSelected = reminder.id in uiState.selectedIds,
                            onToggleSelected = { viewModel.toggleSelection(reminder.id) },
                            onLongClick = { viewModel.toggleSelection(reminder.id) }
                        )
                    }
                }
            }
        }
    }
}
