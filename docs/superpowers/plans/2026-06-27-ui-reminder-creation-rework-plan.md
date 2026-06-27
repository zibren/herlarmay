# UI Reminder Creation Rework Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace start date/time selection with day-of-week + time-of-day selection; add interval unit (seconds/minutes/hours); collapse variable interval by default; fix empty input field behavior.

**Architecture:** MVVM + Room + Compose. RepeatRule JSON gains `daysOfWeek`, `timeOfDaySeconds`, `intervalValue`, `intervalUnit`. NextTriggerCalculator gets `calculateFirstTrigger()` and day-of-week filtering in `calculateNextTrigger()`.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Room, Gson

---

### Task 1: Update RepeatRule data model + create IntervalUnit enum

**Files:**
- Modify: `app/src/main/java/cn/zibiren/herlarmay/data/entity/RepeatRule.kt`
- No test file (pure data class)

- [ ] **Step 1: Replace `fixedIntervalMinutes` with `intervalValue` + `intervalUnit` in RepeatRule, add `daysOfWeek` and `timeOfDaySeconds`**

```kotlin
// RepeatRule.kt
package cn.zibiren.herlarmay.data.entity

enum class RepeatType { once, periodic, interval }
enum class PeriodUnit { day, week, month }
enum class IntervalUnit { SECONDS, MINUTES, HOURS }

data class RepeatRule(
    val type: RepeatType = RepeatType.once,
    val periodUnit: PeriodUnit? = null,
    val periodInterval: Int? = null,
    val intervalValue: Int? = null,
    val intervalUnit: IntervalUnit? = null,
    val variableIntervals: List<Int>? = null,
    val variableLoop: Boolean = true,
    val endTimeMillis: Long? = null,
    val daysOfWeek: List<Int>? = null,
    val timeOfDaySeconds: Int? = null
)
```

- [ ] **Step 2: Verify compilation of RepeatRule.kt**

Run: `.\gradlew :app:compileDebugKotlin 2>&1 | Select-String "error"`

---

### Task 2: Rewrite NextTriggerCalculator

**Files:**
- Modify: `app/src/main/java/cn/zibiren/herlarmay/core/alarm/NextTriggerCalculator.kt`

- [ ] **Step 1: Replace entire file with new implementation**

```kotlin
package cn.zibiren.herlarmay.core.alarm

import cn.zibiren.herlarmay.data.entity.IntervalUnit
import cn.zibiren.herlarmay.data.entity.PeriodUnit
import cn.zibiren.herlarmay.data.entity.RepeatRule
import cn.zibiren.herlarmay.data.entity.RepeatType
import java.util.Calendar

object NextTriggerCalculator {

    fun calculateFirstTrigger(saveTimeMillis: Long, rule: RepeatRule): Long {
        if (rule.type == RepeatType.interval) {
            return saveTimeMillis
        }
        val timeOfDaySeconds = rule.timeOfDaySeconds ?: 0
        val daysOfWeek = rule.daysOfWeek

        val cal = Calendar.getInstance().apply {
            timeInMillis = saveTimeMillis
            set(Calendar.HOUR_OF_DAY, timeOfDaySeconds / 3600)
            set(Calendar.MINUTE, (timeOfDaySeconds % 3600) / 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (daysOfWeek.isNullOrEmpty()) {
            return if (cal.timeInMillis > saveTimeMillis) cal.timeInMillis
            else cal.apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
        }

        repeat(8) { offset ->
            val testCal = Calendar.getInstance().apply {
                timeInMillis = saveTimeMillis
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, timeOfDaySeconds / 3600)
                set(Calendar.MINUTE, (timeOfDaySeconds % 3600) / 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (offset > 0 || testCal.timeInMillis > saveTimeMillis) {
                if (isoDayOfWeek(testCal) in daysOfWeek) {
                    return testCal.timeInMillis
                }
            }
        }
        return saveTimeMillis + 7 * 24 * 60 * 60 * 1000L
    }

    fun calculateNextTrigger(
        currentTriggerTimeMillis: Long,
        rule: RepeatRule,
        currentStepIndex: Int = 0
    ): Pair<Long?, Int> {
        val (rawTime, nextIndex) = calculateRaw(currentTriggerTimeMillis, rule, currentStepIndex)
        val adjustedTime = rawTime?.let { advanceToSelectedDay(it, rule.daysOfWeek) }
        return Pair(adjustedTime, nextIndex)
    }

    private fun calculateRaw(
        time: Long, rule: RepeatRule, stepIndex: Int
    ): Pair<Long?, Int> = when (rule.type) {
        RepeatType.once -> Pair(null, 0)

        RepeatType.periodic -> {
            val unitMillis = when (rule.periodUnit) {
                PeriodUnit.day -> 86_400_000L
                PeriodUnit.week -> 604_800_000L
                PeriodUnit.month -> 2_592_000_000L
                null -> 86_400_000L
            }
            val interval = (rule.periodInterval ?: 1).toLong()
            Pair(time + unitMillis * interval, 0)
        }

        RepeatType.interval -> {
            if (!rule.variableIntervals.isNullOrEmpty()) {
                val intervals = rule.variableIntervals
                if (stepIndex < intervals.size) {
                    Pair(time + intervals[stepIndex].toLong() * 60_000, stepIndex + 1)
                } else if (rule.variableLoop) {
                    Pair(time + intervals[0].toLong() * 60_000, 1)
                } else {
                    Pair(null, 0)
                }
            } else if (rule.intervalValue != null) {
                val millis = rule.intervalValue.toLong() * rule.intervalUnit?.toMillis().toLong()
                Pair(time + millis, 0)
            } else {
                Pair(null, 0)
            }
        }
    }

    private fun advanceToSelectedDay(time: Long, daysOfWeek: List<Int>?): Long {
        if (daysOfWeek.isNullOrEmpty()) return time
        repeat(8) { offset ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = time
                add(Calendar.DAY_OF_YEAR, offset)
            }
            if (isoDayOfWeek(cal) in daysOfWeek) {
                return cal.timeInMillis
            }
        }
        return time
    }

    private fun isoDayOfWeek(cal: Calendar): Int = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> 7
        else -> cal.get(Calendar.DAY_OF_WEEK) - 1
    }
}

private fun IntervalUnit?.toMillis(): Long = when (this) {
    IntervalUnit.SECONDS -> 1_000L
    IntervalUnit.MINUTES -> 60_000L
    IntervalUnit.HOURS -> 3_600_000L
    null -> 60_000L
}
```

- [ ] **Step 2: Verify compilation**

Run: `.\gradlew :app:compileDebugKotlin 2>&1 | Select-String "error"`

---

### Task 3: Update EditReminderUiState and ViewModel

**Files:**
- Modify: `app/src/main/java/cn/zibiren/herlarmay/ui/screens/edit/EditReminderViewModel.kt`

- [ ] **Step 1: Rewrite EditReminderUiState**

```kotlin
data class EditReminderUiState(
    val title: String = "",
    val description: String = "",
    val selectedDays: Set<Int> = emptySet(),
    val triggerHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    val triggerMinute: Int = (Calendar.getInstance().get(Calendar.MINUTE) + 1) % 60,
    val repeatType: RepeatType = RepeatType.once,
    val periodUnit: PeriodUnit = PeriodUnit.day,
    val periodInterval: Int = 1,
    val intervalValue: String = "30",
    val intervalUnit: IntervalUnit = IntervalUnit.SECONDS,
    val variableIntervals: List<Int> = emptyList(),
    val variableLoop: Boolean = true,
    val showVariableInterval: Boolean = false,
    val ringtoneUri: String? = null,
    val vibrate: Boolean = true,
    val isEditing: Boolean = false,
    val isLoading: Boolean = false
)
```

- [ ] **Step 2: Add new update methods to ViewModel**

Add inside `EditReminderViewModel`:

```kotlin
fun toggleDay(day: Int) {
    _uiState.update {
        val newSet = it.selectedDays.toMutableSet()
        if (day in newSet) newSet.remove(day) else newSet.add(day)
        it.copy(selectedDays = newSet)
    }
}

fun updateTriggerHour(hour: Int) { _uiState.update { it.copy(triggerHour = hour) } }
fun updateTriggerMinute(minute: Int) { _uiState.update { it.copy(triggerMinute = minute) } }
fun updateIntervalValue(value: String) { _uiState.update { it.copy(intervalValue = value) } }
fun updateIntervalUnit(unit: IntervalUnit) { _uiState.update { it.copy(intervalUnit = unit) } }
fun toggleVariableInterval() { _uiState.update { it.copy(showVariableInterval = !it.showVariableInterval) } }
```

Remove `updateStartTime()` and `updateFixedInterval()` (replaced).

- [ ] **Step 3: Rewrite `buildReminder()`**

```kotlin
private fun buildReminder(): ReminderEntity {
    val state = _uiState.value
    val now = System.currentTimeMillis()
    val rule = RepeatRule(
        type = state.repeatType,
        daysOfWeek = state.selectedDays.toList().ifEmpty { null },
        timeOfDaySeconds = state.triggerHour * 3600 + state.triggerMinute * 60,
        periodUnit = if (state.repeatType == RepeatType.periodic) state.periodUnit else null,
        periodInterval = if (state.repeatType == RepeatType.periodic) state.periodInterval else null,
        intervalValue = if (state.repeatType == RepeatType.interval) state.intervalValue.toIntOrNull() else null,
        intervalUnit = if (state.repeatType == RepeatType.interval) state.intervalUnit else null,
        variableIntervals = if (state.repeatType == RepeatType.interval && state.variableIntervals.isNotEmpty()) state.variableIntervals else null,
        variableLoop = if (state.repeatType == RepeatType.interval) state.variableLoop else true
    )
    return ReminderEntity(
        id = if (state.isEditing) reminderId else 0,
        title = state.title,
        description = state.description,
        startTimeMillis = now,
        repeatRuleJson = Gson().toJson(rule),
        ringtoneUri = state.ringtoneUri,
        vibrate = state.vibrate,
        isEnabled = true,
        nextTriggerTimeMillis = NextTriggerCalculator.calculateFirstTrigger(now, rule),
        currentStepIndex = 0
    )
}
```

- [ ] **Step 4: Rewrite `loadReminder()`**

```kotlin
private fun loadReminder(id: Long) {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        val reminder = repository.getReminder(id) ?: return@launch
        val rule = Gson().fromJson(reminder.repeatRuleJson, RepeatRule::class.java)
        val tod = rule.timeOfDaySeconds ?: 0
        _uiState.update {
            it.copy(
                title = reminder.title,
                description = reminder.description,
                selectedDays = rule.daysOfWeek?.toSet() ?: emptySet(),
                triggerHour = tod / 3600,
                triggerMinute = (tod % 3600) / 60,
                repeatType = rule.type,
                periodUnit = rule.periodUnit ?: PeriodUnit.day,
                periodInterval = rule.periodInterval ?: 1,
                intervalValue = rule.intervalValue?.toString() ?: "30",
                intervalUnit = rule.intervalUnit ?: IntervalUnit.SECONDS,
                variableIntervals = rule.variableIntervals ?: emptyList(),
                variableLoop = rule.variableLoop,
                ringtoneUri = reminder.ringtoneUri,
                vibrate = reminder.vibrate,
                isEditing = true,
                isLoading = false
            )
        }
    }
}
```

- [ ] **Step 5: Update imports**

Add:
```kotlin
import cn.zibiren.herlarmay.core.alarm.NextTriggerCalculator
import cn.zibiren.herlarmay.data.entity.IntervalUnit
```

Remove:
```kotlin
// no imports to remove - startTimeMillis was in UiState, not an import
```

- [ ] **Step 6: Verify compilation**

Run: `.\gradlew :app:compileDebugKotlin 2>&1 | Select-String "error"`

---

### Task 4: Rewrite EditReminderScreen

**Files:**
- Modify: `app/src/main/java/cn/zibiren/herlarmay/ui/screens/edit/EditReminderScreen.kt`

This is the biggest change. Replace the DatePicker+TimePicker section with day-of-week chips + time picker. Add interval unit dropdown. Add variable interval collapse.

- [ ] **Step 1: Replace the imports section**

New imports:
```kotlin
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
```

- [ ] **Step 2: Replace the entire body of `EditReminderScreen`**

Full file content (replacing everything inside the @Composable function - keep the function signature and Scaffold):

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReminderScreen(
    viewModel: EditReminderViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showTimePicker by remember { mutableStateOf(false) }

    // TimePickerDialog lifecycle
    DisposableEffect(showTimePicker) {
        if (!showTimePicker) return@DisposableEffect onDispose { }
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, uiState.triggerHour)
            set(Calendar.MINUTE, uiState.triggerMinute)
        }
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
            // 标题
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 描述
            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("描述") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // ==== 生效设置 ====
            Text("提醒时间", style = MaterialTheme.typography.labelLarge)

            // 星期选择
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

            // ==== 重复参数 ====
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

            // ==== 间隔参数 ====
            if (uiState.repeatType == RepeatType.interval) {
                Text("间隔", style = MaterialTheme.typography.labelLarge)

                // 固定间隔 + 单位下拉
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
                            val m = newInterval.toIntOrNull()
                            if (m != null && m > 0) {
                                viewModel.addVariableInterval(m)
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
```

- [ ] **Step 2: Verify compilation**

Run: `.\gradlew :app:compileDebugKotlin 2>&1 | Select-String "error"`

---

### Task 5: Verify full build and fix any errors

**Files:**
- All modified files

- [ ] **Step 1: Full build**

Run: `.\gradlew assembleDebug 2>&1 | Select-String "error|FAILED"`

- [ ] **Step 2: Install and run**

Run: `adb uninstall cn.zibiren.herlarmay; .\gradlew installDebug`

- [ ] **Step 3: Smoke test**
- Create a one-time reminder: select Mon+Wed, time 08:30 → verify save succeeds
- Create an interval reminder: select weekdays, 30 seconds → verify
- Edit an existing reminder → verify fields load correctly
- Delete all digits from interval input → verify field shows empty
- Toggle variable interval collapse → verify panel shows/hides
