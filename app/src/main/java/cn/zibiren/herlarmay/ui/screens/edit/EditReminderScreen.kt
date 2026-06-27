package cn.zibiren.herlarmay.ui.screens.edit

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cn.zibiren.herlarmay.data.entity.IntervalUnit
import cn.zibiren.herlarmay.data.entity.PeriodUnit
import cn.zibiren.herlarmay.data.entity.RepeatType
import java.util.Calendar

/**
 * 创建/编辑提醒界面。
 *
 * 用户可设置：
 * - 提醒标题、描述
 * - 星期（一~日，多选）
 * - 提醒时刻（TimePicker）
 * - 提醒类型：一次性 / 按周期重复 / 按间隔重复
 * - 重复参数：周期单位（天/周/月）和间隔数
 * - 间隔参数：数值 + 单位（秒/分钟/小时）+ 不等间隔（默认折叠）
 * - 是否震动
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReminderScreen(
    viewModel: EditReminderViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showTimePicker by remember { mutableStateOf(false) }

    // TimePickerDialog 生命周期管理
    DisposableEffect(showTimePicker) {
        if (!showTimePicker) return@DisposableEffect onDispose { }
        val dialog = TimePickerDialog(
            context,
            { _, hour, minute ->
                viewModel.updateTriggerHour(hour)
                viewModel.updateTriggerMinute(minute)
                showTimePicker = false
            },
            uiState.triggerHour,
            uiState.triggerMinute,
            true
        ).apply {
            setOnDismissListener { showTimePicker = false }
            show()
        }
        onDispose { dialog.dismiss() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "编辑提醒" else "新建提醒") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save(onNavigateBack) },
                        enabled = uiState.title.isNotBlank()
                    ) {
                        Text("保存")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 提醒标题
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 提醒描述
            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("描述") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // ==== 生效设置 ====
            Text("提醒时间", style = MaterialTheme.typography.labelLarge)

            // 星期选择 chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val dayNames = listOf("一", "二", "三", "四", "五", "六", "日")
                dayNames.forEachIndexed { index, name ->
                    val dayNum = index + 1
                    FilterChip(
                        selected = dayNum in uiState.selectedDays,
                        onClick = { viewModel.toggleDay(dayNum) },
                        label = { Text(name) }
                    )
                }
            }

            // 时刻选择
            OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(String.format("%02d:%02d", uiState.triggerHour, uiState.triggerMinute))
            }

            // ==== 类型 ====
            Text("类型", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RepeatType.entries.forEach { type ->
                    FilterChip(
                        selected = uiState.repeatType == type,
                        onClick = { viewModel.updateRepeatType(type) },
                        label = { Text(
                            when (type) {
                                RepeatType.once -> "一次性"
                                RepeatType.periodic -> "重复"
                                RepeatType.interval -> "间隔"
                            }
                        )}
                    )
                }
            }

            // ==== 周期性重复设置 ====
            if (uiState.repeatType == RepeatType.periodic) {
                Text("重复", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.periodInterval.toString(),
                        onValueChange = { viewModel.updatePeriodInterval(it.toIntOrNull() ?: 1) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )
                    PeriodUnit.entries.forEach { unit ->
                        FilterChip(
                            selected = uiState.periodUnit == unit,
                            onClick = { viewModel.updatePeriodUnit(unit) },
                            label = { Text(
                                when (unit) {
                                    PeriodUnit.day -> "天"
                                    PeriodUnit.week -> "周"
                                    PeriodUnit.month -> "月"
                                }
                            )}
                        )
                    }
                }
            }

            // ==== 间隔提醒设置 ====
            if (uiState.repeatType == RepeatType.interval) {
                Text("间隔", style = MaterialTheme.typography.labelLarge)

                // 固定间隔输入 + 单位下拉
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.intervalValue,
                        onValueChange = { viewModel.updateIntervalValue(it) },
                        label = { Text("间隔") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expanded = true }) {
                            Text(when (uiState.intervalUnit) {
                                IntervalUnit.SECONDS -> "秒"
                                IntervalUnit.MINUTES -> "分钟"
                                IntervalUnit.HOURS -> "小时"
                            })
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            IntervalUnit.entries.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(when (unit) {
                                        IntervalUnit.SECONDS -> "秒"
                                        IntervalUnit.MINUTES -> "分钟"
                                        IntervalUnit.HOURS -> "小时"
                                    })},
                                    onClick = { viewModel.updateIntervalUnit(unit); expanded = false }
                                )
                            }
                        }
                    }
                }

                // 不等间隔（默认折叠）
                TextButton(onClick = { viewModel.toggleVariableInterval() }) {
                    Icon(
                        if (uiState.showVariableInterval) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("不等间隔")
                }

                if (uiState.showVariableInterval) {
                    var newInterval by remember { mutableStateOf("") }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newInterval,
                            onValueChange = { newInterval = it },
                            label = { Text("分钟") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(onClick = {
                            val minutes = newInterval.toIntOrNull()
                            if (minutes != null && minutes > 0) {
                                viewModel.addVariableInterval(minutes)
                                newInterval = ""
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "添加间隔")
                        }
                    }
                    uiState.variableIntervals.forEachIndexed { index, minutes ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("${index + 1}. $minutes 分钟")
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { viewModel.removeVariableInterval(index) }) {
                                Icon(Icons.Default.Close, contentDescription = "删除")
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("循环序列")
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = uiState.variableLoop,
                            onCheckedChange = { viewModel.updateVariableLoop(it) }
                        )
                    }
                }
            }

            // ==== 震动 ====
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("震动", style = MaterialTheme.typography.labelLarge)
                Switch(
                    checked = uiState.vibrate,
                    onCheckedChange = { viewModel.updateVibrate(it) }
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
