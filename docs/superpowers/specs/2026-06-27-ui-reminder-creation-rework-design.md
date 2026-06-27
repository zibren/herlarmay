# Herlarmay 提醒创建界面改造设计

## 概述

将提醒创建界面从"选择日期+时间"改为"选择星期+选择时刻"，星期过滤适用于所有提醒类型（一次性/重复/间隔）。

## 数据模型变更

### RepeatRule 字段变更（JSON 持久化）

| 字段 | 变更 | 说明 |
|---|---|---|
| `daysOfWeek` | 新增 | `List<Int>?`，选中星期，1=周一...7=周日（ISO 8601），null=每天 |
| `timeOfDaySeconds` | 新增 | `Int?`，提醒时刻（秒数，如 14:30=52200） |
| `fixedIntervalMinutes` | 替换为 `intervalValue` + `intervalUnit` | 旧字段不再使用 |
| `intervalValue` | 新增 | `Int?`，间隔数值 |
| `intervalUnit` | 新增 | `IntervalUnit?`，SECONDS/MINUTES/HOURS，null=向后兼容 |

注：ISO 8601 星期与 java.util.Calendar 的 DAY_OF_WEEK 映射关系：
- ISO 1=Mon → Calendar.MONDAY=2
- ISO 2=Tue → Calendar.TUESDAY=3
- ...
- ISO 7=Sun → Calendar.SUNDAY=1

### EditReminderUiState 变更

| 变更 | 说明 |
|---|---|
| 删除 `startTimeMillis: Long` | 不再用户选择，保存时间自动为生效时间 |
| 新增 `selectedDays: Set<Int>` | 选中星期集合 |
| 新增 `triggerHour: Int` | 提醒时（0-23） |
| 新增 `triggerMinute: Int` | 提醒分（0-59） |
| 新增 `intervalValue: String` | 间隔数值（字符串，允许空） |
| 新增 `intervalUnit: IntervalUnit` | 间隔单位（SECONDS/MINUTES/HOURS） |
| 新增 `showVariableInterval: Boolean` | 不等间隔面板是否展开，默认 false |
| 保留 `variableIntervals: List<Int>` | 不变，但默认隐藏 |
| 保留 `variableLoop: Boolean` | 不变 |

### 新枚举

```kotlin
enum class IntervalUnit { SECONDS, MINUTES, HOURS }
```

## UI 布局（从上到下）

```
[标题输入]
[描述输入]
==== 生效设置 ====
[星期一] [二] [三] [四] [五] [六] [日]    ← 星期多选 chips，至少选一个
[08:30]                                     ← TimePicker，24 小时制，仅选时分
==== 类型 ====
[一次性] [重复] [间隔]
==== 重复参数（类型=重复时显示） ====
[3]  [天/周/月]
==== 间隔参数（类型=间隔时显示） ====
[30] [秒 ▼]                                  ← 输入框 + 单位下拉
[▼ 不等间隔]                                  ← 折叠，默认收起
====
[震动] 开关
```

### 关键交互规则

- **星期 chips**：至少选一个才可保存；点击切换选中/未选中
- **TimePicker**：使用 Android `TimePickerDialog`（24 小时制），`DisposableEffect` 管理生命周期
- **间隔单位下拉**：用 `DropdownMenu` 或 `ExposedDropdownMenuBox` 三选一
- **不等间隔**：默认折叠，点击展开后显示序列输入（同现有逻辑 + 循环开关）
- **输入框删除干净**：`intervalValue` 为 String 类型，保留空字符串；保存时 `toIntOrNull()` 失败则使用默认值（30 秒）

## NextTriggerCalculator 逻辑变更

### 首次触发计算

新增 `calculateFirstTrigger(saveTimeMillis: Long, rule: RepeatRule): Long`：

```
1. 如果类型 == interval：
   - 返回 saveTimeMillis（AlarmScheduler 内部有 +1s 安全守卫）

2. 否则（once / periodic）：
   a. 从 saveTimeMillis 构建 Calendar，设置到 timeOfDaySeconds 对应的时分秒
   b. 如果今天的目标时刻 > saveTimeMillis 且今天是选中星期之一 → 返回目标时刻
   c. 否则，逐日推进到下一个选中星期的目标时刻
   d. 如果 daysOfWeek == null → 使用今天（即无星期限制）
```

### 后续触发计算（现有 calculateNextTrigger 扩展）

| 类型 | 逻辑 |
|---|---|
| **once** | 返回 null（同现有） |
| **periodic** | 按照 periodUnit + periodInterval 累加，然后跳转到下一个选中星期 |
| **interval (fixed)** | 当前时间 + interval（秒），检查是否在选中星期内；否→跳到该选中星期的同一 wall-clock 时刻（hour:minute:second） |
| **interval (variable)** | 当前时间 + 步进间隔（秒），同上检查 |

### 星期跳转算法

```
fun advanceToNextSelectedDay(currentTimeMillis, daysOfWeek, timeOfDaySeconds):
  1. 从 currentTimeMillis 构建 Calendar
  2. 最多搜索 7 天
  3. 每天检查 Calendar.get(Calendar.DAY_OF_WEEK) 是否在 daysOfWeek 映射中
  4. 找到匹配后，设置时分秒为 timeOfDaySeconds
  5. 如果找不到（所有天都未选中），回退到 +7 天后的同一时刻
```

## IntervalUnit 到毫秒的转换

```kotlin
fun IntervalUnit.toMillis(value: Int): Long = when (this) {
    IntervalUnit.SECONDS -> value * 1000L
    IntervalUnit.MINUTES -> value * 60_000L
    IntervalUnit.HOURS   -> value * 3_600_000L
}
```

## 涉及文件清单

| 文件 | 变更类型 |
|---|---|
| `data/entity/RepeatRule.kt` | 新增字段 `daysOfWeek`, `timeOfDaySeconds` |
| `data/entity/ReminderEntity.kt` | 无变化（`startTimeMillis` 字段保留，仅不再 UI 设置） |
| `core/alarm/NextTriggerCalculator.kt` | 重写：新增星期过滤逻辑 |
| `ui/screens/edit/EditReminderUiState.kt` | 重构：删除 startTimeMillis，新增星期/时刻/间隔单位/折叠状态 |
| `ui/screens/edit/EditReminderScreen.kt` | 重构：删除 DatePicker，新增星期 chips + 时刻选择 + 单位下拉 + 不等间隔折叠 |
| `ui/screens/edit/EditReminderViewModel.kt` | 重构：buildReminder 使用 calculateFirstTrigger，新增星期/时刻/单位更新方法 |

## 验证要点

1. 创建一次性提醒选周二+周四 08:30→应在下一个周二/周四 08:30 触发
2. 创建间隔提醒选工作日+30 分钟→周五 17:00 触发后，周六 00:00 应跳到周一
3. 间隔输入框删除所有数字后→显示为空，保存时使用默认 30 秒
4. 不等间隔面板默认收起→点击展开后正常添加/删除
5. 编辑已有提醒能正确加载星期和时间
6. 不选任何星期→保存按钮灰色不可点击
