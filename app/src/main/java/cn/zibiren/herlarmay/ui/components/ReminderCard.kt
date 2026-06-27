package cn.zibiren.herlarmay.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.zibiren.herlarmay.data.entity.IntervalUnit
import cn.zibiren.herlarmay.data.entity.PeriodUnit
import cn.zibiren.herlarmay.data.entity.ReminderEntity
import cn.zibiren.herlarmay.data.entity.RepeatRule
import cn.zibiren.herlarmay.data.entity.RepeatType
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 提醒卡片组件。
 *
 * 每张卡片显示：
 * - 提醒标题
 * - 类型描述（一次性 / 每N天/周/月 / 每N分钟 / 不等间隔）
 * - 下次触发时间
 * - 启用/禁用开关
 *
 * 交互：
 * - 点击卡片进入编辑（普通模式）/ 切换选中（选择模式）
 * - 左滑删除（选择模式下禁用）
 * - 禁用状态时降低透明度
 *
 * @param isSelectionMode 是否处于批量选择模式
 * @param isSelected 当前卡片是否被选中（选择模式下显示 Checkbox）
 * @param onToggleSelected 切换选中状态的回调
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReminderCard(
    reminder: ReminderEntity,
    onToggleEnabled: (ReminderEntity) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelected: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    // 解析 JSON 重复规则
    val rule = remember(reminder.repeatRuleJson) {
        Gson().fromJson(reminder.repeatRuleJson, RepeatRule::class.java)
    }

    // 生成类型描述文字
    val scheduleText = remember(rule) {
        when (rule.type) {
            RepeatType.once -> "一次性"
            RepeatType.periodic -> {
                "每${rule.periodInterval ?: 1}${when (rule.periodUnit) {
                    PeriodUnit.day -> "天"
                    PeriodUnit.week -> "周"
                    PeriodUnit.month -> "月"
                    null -> ""
                }}"
            }
            RepeatType.interval -> {
                if (rule.intervalValue != null && rule.intervalUnit != null) "每${rule.intervalValue}${when (rule.intervalUnit) {
                    IntervalUnit.SECONDS -> "秒"
                    IntervalUnit.MINUTES -> "分钟"
                    IntervalUnit.HOURS -> "小时"
                }}"
                else "不等间隔"
            }
        }
    }

    // 左滑删除状态管理（选择模式下禁用左滑）
    val dismissState = rememberSwipeToDismissBoxState()
    if (!isSelectionMode && dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
        LaunchedEffect(Unit) {
            onDelete()
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !isSelectionMode,
        backgroundContent = {
            // 左滑时显示删除文字
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        },
        content = {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { if (isSelectionMode) onToggleSelected() else onClick() },
                        onLongClick = { if (!isSelectionMode) onLongClick() }
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        reminder.isEnabled -> MaterialTheme.colorScheme.surface
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 选择模式下显示 Checkbox
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelected() }
                        )
                        Spacer(Modifier.width(8.dp))
                    }

                    // 左侧文字区域
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = reminder.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = scheduleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // 显示下次触发时间
                        if (reminder.nextTriggerTimeMillis != null) {
                            Text(
                                text = "下次: ${dateFormat.format(Date(reminder.nextTriggerTimeMillis))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    // 右侧开关（选择模式下隐藏开关）
                    if (!isSelectionMode) {
                        Switch(
                            checked = reminder.isEnabled,
                            onCheckedChange = { onToggleEnabled(reminder) }
                        )
                    }
                }
            }
        }
    )
}
