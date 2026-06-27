# Herlarmay App Design

## Overview

Herlarmay is an Android strong-alarm reminder app. Even if the app process is killed by the system, reminders will still trigger on time (like the system alarm clock). All reminder data is stored locally on the device.

- **Package**: `cn.zibiren.herlarmay`
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36 (Android 15)
- **UI**: Jetpack Compose + Material 3

## Module Structure

```
herlarmay/
├── :app          # Application entry, DI, Navigation, UI screens
├── :core         # AlarmScheduler, AlarmService, BootReceiver, extensions, theme
└── :data         # Room database, DAO, Repository, entities, TypeConverters
```

Dependency: `:app` -> `:core`, `:data`

## Architecture: MVVM + Repository

```
UI (Compose) -> ViewModel -> Repository -> Room DB
                                  \-> AlarmScheduler -> AlarmManager
```

## Data Layer

### Entity: ReminderEntity

```kotlin
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val startTimeMillis: Long,
    val repeatRuleJson: String,
    val ringtoneUri: String?,
    val vibrate: Boolean = true,
    val isEnabled: Boolean = true,
    val nextTriggerTimeMillis: Long?,
    val createdAt: Long = System.currentTimeMillis()
)
```

### RepeatRule (JSON model)

```kotlin
data class RepeatRule(
    val type: RepeatType,                       // once, periodic, interval
    val periodUnit: PeriodUnit? = null,          // day, week, month
    val periodInterval: Int? = null,             // every N days/weeks/months
    val fixedIntervalMinutes: Int? = null,        // fixed interval in minutes
    val variableIntervals: List<Int>? = null,     // variable interval sequence (minutes)
    val variableLoop: Boolean = true,             // loop variable sequence
    val endTimeMillis: Long? = null               // end time for daily window
)

enum class RepeatType { once, periodic, interval }
enum class PeriodUnit { day, week, month }
```

### ReminderDao

```kotlin
@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY nextTriggerTimeMillis ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>
    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminder(id: Long): ReminderEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long
    @Update
    suspend fun update(reminder: ReminderEntity)
    @Delete
    suspend fun delete(reminder: ReminderEntity)
    @Query("SELECT * FROM reminders WHERE isEnabled = 1")
    suspend fun getAllEnabled(): List<ReminderEntity>
    @Query("SELECT * FROM reminders WHERE isEnabled = 1 AND nextTriggerTimeMillis <= :now")
    suspend fun getDueReminders(now: Long): List<ReminderEntity>
}
```

### ReminderRepository

```kotlin
class ReminderRepository(private val dao: ReminderDao) {
    fun getAllReminders(): Flow<List<ReminderEntity>> = dao.getAllReminders()
    suspend fun getReminder(id: Long): ReminderEntity? = dao.getReminder(id)
    suspend fun save(reminder: ReminderEntity) { if (id==0) dao.insert() else dao.update() }
    suspend fun delete(reminder: ReminderEntity) = dao.delete(reminder)
    suspend fun getAllEnabled(): List<ReminderEntity> = dao.getAllEnabled()
    suspend fun getDueReminders(now: Long) = dao.getDueReminders(now)
}
```

### Database

```kotlin
@Database(entities = [ReminderEntity::class], version = 1)
abstract class HerlarmayDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
}
```

Use Gson for RepeatRule JSON TypeConverter.

## Scheduling Layer (core module)

### AlarmScheduler

Wraps `AlarmManager` for setting/canceling exact alarms.

```kotlin
class AlarmScheduler(private val context: Context) {
    fun schedule(reminder: ReminderEntity)
    fun cancel(reminder: ReminderEntity)
    fun rescheduleAll(enabledReminders: List<ReminderEntity>)
}
```

- Uses `AlarmManager.setExact()` with `SCHEDULE_EXACT_ALARM`
- PendingIntent targets `AlarmReceiver` (BroadcastReceiver)
- On Android 12+, request `SCHEDULE_EXACT_ALARM` permission if needed

### AlarmReceiver (BroadcastReceiver)

Receives the AlarmManager broadcast and:
1. Launches `AlarmActivity` (full screen)
2. Starts `AlarmService` (foreground service) for continuous ringing
3. Calculates next trigger time based on RepeatRule and reschedules

### BootReceiver (BroadcastReceiver)

On `BOOT_COMPLETED`:
1. Loads all enabled reminders from Room
2. Calculates next trigger time for each
3. Calls `AlarmScheduler.rescheduleAll()`

### AlarmService (Foreground Service)

- Runs as foreground service with high-priority notification
- Plays ringtone in loop (MediaPlayer with Alarm audio stream)
- Vibrates in pattern
- Stops when user dismisses from `AlarmActivity`

## UI Layer (app module)

### Navigation Routes

```
composable("list")                    -> ReminderListScreen
composable("edit/{id}")               -> EditReminderScreen (id=-1 for create)
```

AlarmActivity is a separate activity (not in NavHost) launched by AlarmReceiver intent.

### ReminderListScreen

- TopAppBar with title "Herlarmay"
- LazyColumn of reminder cards
- Each card: title, schedule description, next trigger time, enable Switch
- SwipeToDismiss for delete
- Click card -> navigate to edit
- FAB -> navigate to create
- Empty state when no reminders

### EditReminderScreen

- DatePicker + TimePicker for start time
- Radio buttons: one-time / repeat / interval
- Repeat options: period unit (day/week/month), interval count
- Interval options: fixed (number + unit) / variable (add/remove/reorder list items)
- Title and description text fields
- Ringtone picker (system default + custom)
- Vibrate toggle
- Save button

### AlarmActivity

- Activity theme: `@android:style/Theme.Material.NoActionBar`
- Window flags: `SHOW_WHEN_LOCKED`, `TURN_SCREEN_ON`, `KEEP_SCREEN_ON`
- Displays: title, description, trigger time
- Buttons: Confirm, Snooze (5/10/30 min)
- Incoming call handling: dismiss alarm on call

## RepeatRule Calculation Logic

### Next trigger calculation

1. **once**: no next trigger
2. **periodic**: `lastTrigger + periodInterval * unit`
3. **fixed interval**: `lastTrigger + fixedIntervalMinutes * 60000`
4. **variable interval**: track current step index in RepeatRule, advance on each trigger

### Smart interval / Random interval (future)

- Smart: time-of-day based interval selection at trigger time
- Random: random value between min/max at trigger time
- These can be added later by extending RepeatRule JSON fields

## Permissions

```xml
POST_NOTIFICATIONS (Android 13+)
SCHEDULE_EXACT_ALARM (Android 12+)
USE_EXACT_ALARM
FOREGROUND_SERVICE
FOREGROUND_SERVICE_SPECIAL_USE
RECEIVE_BOOT_COMPLETED
VIBRATE
WAKE_LOCK
```

## Dependencies to Add

| Library | Version | Purpose |
|---------|---------|---------|
| androidx.room:room-runtime | 2.7.1 | Room database |
| androidx.room:room-ktx | 2.7.1 | Room coroutines |
| androidx.room:room-compiler | 2.7.1 | Room annotation processor |
| androidx.navigation:navigation-compose | 2.8.9 | Compose navigation |
| com.google.code.gson | 2.12.1 | JSON serialization for RepeatRule |

## MVP Feature Set

1. One-time reminders
2. Periodic reminders (daily/weekly/monthly)
3. Fixed interval reminders (every N minutes/hours)
4. Variable interval reminders (sequence of intervals, loop optional)
5. Strong alarm: full-screen + ring + vibrate until dismissed
6. Snooze (5/10/30 min)
7. Boot recovery
8. Enable/disable toggle
9. Swipe-to-delete
10. Local persistence (Room)

## Future Features (post-MVP)

- Smart interval (time-of-day based)
- Random interval (min/max range)
- Reminder history log
- Batch dismiss
- Dark theme enhancements
