# Herlarmay Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Implement the full Herlarmay strong-alarm reminder app per the design spec.

**Architecture:** Multi-module (:app + :core + :data), MVVM + Repository pattern, Jetpack Compose UI, Room persistence, AlarmManager-based scheduling with boot recovery.

**Tech Stack:** Kotlin 2.2, Jetpack Compose + M3, Room 2.7, Navigation Compose, Gson, Android API 24-36.

---

### Task 1: Project Setup — Version Catalog, Plugins, Module Scaffolding

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`
- Create: `core/build.gradle.kts`
- Create: `core/src/main/AndroidManifest.xml`
- Create: `data/build.gradle.kts`
- Create: `data/src/main/AndroidManifest.xml`

- [ ] **Step 1: Update version catalog**

Replace `gradle/libs.versions.toml` with:

```toml
[versions]
agp = "9.2.1"
coreKtx = "1.10.1"
junit = "4.13.2"
junitVersion = "1.1.5"
espressoCore = "3.5.1"
lifecycleRuntimeKtx = "2.6.1"
activityCompose = "1.8.0"
kotlin = "2.2.10"
composeBom = "2026.02.01"
room = "2.7.1"
navigationCompose = "2.8.9"
gson = "2.12.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
gson = { group = "com.google.code.gson", name = "gson", version.ref = "gson" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-kapt = { id = "org.jetbrains.kotlin.kapt", version.ref = "kotlin" }
```

- [ ] **Step 2: Update root `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.kapt) apply false
}
```

- [ ] **Step 3: Update `settings.gradle.kts`**

Add `:core` and `:data` to the include list:

```kotlin
rootProject.name = "herlarmay"
include(":app")
include(":core")
include(":data")
```

- [ ] **Step 4: Create `core/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "cn.zibiren.herlarmay.core"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
}
```

- [ ] **Step 5: Create `data/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "cn.zibiren.herlarmay.data"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    implementation(libs.gson)
}
```

- [ ] **Step 6: Create `core/src/main/AndroidManifest.xml`**

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
```

- [ ] **Step 7: Create `data/src/main/AndroidManifest.xml`**

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
```

- [ ] **Step 8: Update `app/build.gradle.kts` dependencies**

Add dependencies on `:core` and `:data`:

```kotlin
dependencies {
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
```

- [ ] **Step 9: Verify project syncs**

Run: `cd F:\code\android\herlarmay && ./gradlew projects`
Expected: lists `:app`, `:core`, `:data` projects

---

### Task 2: Data Layer — Room Database

**Files:**
- Create: `data/src/main/java/cn/zibiren/herlarmay/data/entity/RepeatRule.kt`
- Create: `data/src/main/java/cn/zibiren/herlarmay/data/entity/ReminderEntity.kt`
- Create: `data/src/main/java/cn/zibiren/herlarmay/data/db/RepeatRuleConverter.kt`
- Create: `data/src/main/java/cn/zibiren/herlarmay/data/dao/ReminderDao.kt`
- Create: `data/src/main/java/cn/zibiren/herlarmay/data/db/HerlarmayDatabase.kt`
- Create: `data/src/main/java/cn/zibiren/herlarmay/data/repository/ReminderRepository.kt`

- [ ] **Step 1: Create `RepeatRule.kt`**

Package: `cn.zibiren.herlarmay.data.entity`

```kotlin
package cn.zibiren.herlarmay.data.entity

enum class RepeatType { once, periodic, interval }
enum class PeriodUnit { day, week, month }

data class RepeatRule(
    val type: RepeatType = RepeatType.once,
    val periodUnit: PeriodUnit? = null,
    val periodInterval: Int? = null,
    val fixedIntervalMinutes: Int? = null,
    val variableIntervals: List<Int>? = null,
    val variableLoop: Boolean = true,
    val endTimeMillis: Long? = null
)
```

- [ ] **Step 2: Create `ReminderEntity.kt`**

```kotlin
package cn.zibiren.herlarmay.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val startTimeMillis: Long,
    val repeatRuleJson: String,
    val ringtoneUri: String? = null,
    val vibrate: Boolean = true,
    val isEnabled: Boolean = true,
    val nextTriggerTimeMillis: Long? = null,
    val currentStepIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 3: Create `RepeatRuleConverter.kt`**

```kotlin
package cn.zibiren.herlarmay.data.db

import androidx.room.TypeConverter
import cn.zibiren.herlarmay.data.entity.RepeatRule
import com.google.gson.Gson

class RepeatRuleConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromRepeatRule(value: RepeatRule): String = gson.toJson(value)

    @TypeConverter
    fun toRepeatRule(value: String): RepeatRule =
        gson.fromJson(value, RepeatRule::class.java)
}
```

- [ ] **Step 4: Create `ReminderDao.kt`**

```kotlin
package cn.zibiren.herlarmay.data.dao

import androidx.room.*
import cn.zibiren.herlarmay.data.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

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

- [ ] **Step 5: Create `HerlarmayDatabase.kt`**

```kotlin
package cn.zibiren.herlarmay.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cn.zibiren.herlarmay.data.dao.ReminderDao
import cn.zibiren.herlarmay.data.entity.ReminderEntity

@Database(entities = [ReminderEntity::class], version = 1, exportSchema = false)
@TypeConverters(RepeatRuleConverter::class)
abstract class HerlarmayDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: HerlarmayDatabase? = null

        fun getInstance(context: Context): HerlarmayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance =                 Room.databaseBuilder(
                    context.applicationContext,
                    HerlarmayDatabase::class.java,
                    "herlarmay_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

- [ ] **Step 6: Create `ReminderRepository.kt`**

```kotlin
package cn.zibiren.herlarmay.data.repository

import cn.zibiren.herlarmay.data.dao.ReminderDao
import cn.zibiren.herlarmay.data.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

class ReminderRepository(private val dao: ReminderDao) {
    fun getAllReminders(): Flow<List<ReminderEntity>> = dao.getAllReminders()

    suspend fun getReminder(id: Long): ReminderEntity? = dao.getReminder(id)

    suspend fun save(reminder: ReminderEntity) {
        if (reminder.id == 0L) {
            dao.insert(reminder)
        } else {
            dao.update(reminder)
        }
    }

    suspend fun delete(reminder: ReminderEntity) = dao.delete(reminder)

    suspend fun getAllEnabled(): List<ReminderEntity> = dao.getAllEnabled()

    suspend fun getDueReminders(now: Long): List<ReminderEntity> = dao.getDueReminders(now)
}
```

- [ ] **Step 7: Build check**

Run: `cd F:\code\android\herlarmay && ./gradlew :data:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 3: Core Layer — Alarm Scheduling

**Files:**
- Create: `core/src/main/java/cn/zibiren/herlarmay/core/alarm/AlarmScheduler.kt`
- Create: `core/src/main/java/cn/zibiren/herlarmay/core/alarm/AlarmReceiver.kt`
- Create: `core/src/main/java/cn/zibiren/herlarmay/core/alarm/BootReceiver.kt`
- Create: `core/src/main/java/cn/zibiren/herlarmay/core/alarm/NextTriggerCalculator.kt`
- Create: `core/src/main/java/cn/zibiren/herlarmay/core/alarm/AlarmService.kt`

- [ ] **Step 1: Create `NextTriggerCalculator.kt`**

```kotlin
package cn.zibiren.herlarmay.core.alarm

import cn.zibiren.herlarmay.data.entity.PeriodUnit
import cn.zibiren.herlarmay.data.entity.RepeatRule
import cn.zibiren.herlarmay.data.entity.RepeatType

object NextTriggerCalculator {
    fun calculateNextTrigger(
        currentTriggerTimeMillis: Long,
        rule: RepeatRule,
        currentStepIndex: Int = 0
    ): Pair<Long?, Int> {
        val nextIndex: Int
        val nextTime: Long?

        when (rule.type) {
            RepeatType.once -> {
                nextTime = null
                nextIndex = 0
            }
            RepeatType.periodic -> {
                val unitMillis = when (rule.periodUnit) {
                    PeriodUnit.day -> 86400000L
                    PeriodUnit.week -> 604800000L
                    PeriodUnit.month -> 2592000000L
                    null -> 86400000L
                }
                val interval = (rule.periodInterval ?: 1).toLong()
                nextTime = currentTriggerTimeMillis + unitMillis * interval
                nextIndex = 0
            }
            RepeatType.interval -> {
                if (!rule.variableIntervals.isNullOrEmpty()) {
                    val intervals = rule.variableIntervals
                    if (currentStepIndex < intervals.size) {
                        nextTime = currentTriggerTimeMillis + intervals[currentStepIndex].toLong() * 60000
                        nextIndex = currentStepIndex + 1
                    } else if (rule.variableLoop) {
                        nextTime = currentTriggerTimeMillis + intervals[0].toLong() * 60000
                        nextIndex = 1
                    } else {
                        nextTime = null
                        nextIndex = 0
                    }
                } else if (rule.fixedIntervalMinutes != null) {
                    nextTime = currentTriggerTimeMillis + rule.fixedIntervalMinutes.toLong() * 60000
                    nextIndex = 0
                } else {
                    nextTime = null
                    nextIndex = 0
                }
            }
        }

        return Pair(nextTime, nextIndex)
    }
}
```

- [ ] **Step 2: Create `AlarmScheduler.kt`**

```kotlin
package cn.zibiren.herlarmay.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import cn.zibiren.herlarmay.data.entity.ReminderEntity

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(reminder: ReminderEntity) {
        val triggerTime = reminder.nextTriggerTimeMillis ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("reminder_id", reminder.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun cancel(reminder: ReminderEntity) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun rescheduleAll(reminders: List<ReminderEntity>) {
        reminders.forEach { schedule(it) }
    }
}
```

- [ ] **Step 3: Create `AlarmReceiver.kt`**

```kotlin
package cn.zibiren.herlarmay.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cn.zibiren.herlarmay.data.db.HerlarmayDatabase
import cn.zibiren.herlarmay.data.entity.RepeatRule
import cn.zibiren.herlarmay.data.entity.ReminderEntity
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminder_id", -1)
        if (reminderId == -1L) return

        val db = HerlarmayDatabase.getInstance(context)
        val reminder = runBlocking { db.reminderDao().getReminder(reminderId) } ?: return
        if (!reminder.isEnabled) return

        val rule = Gson().fromJson(reminder.repeatRuleJson, RepeatRule::class.java)

        val currentTime = reminder.nextTriggerTimeMillis ?: reminder.startTimeMillis
        val (nextTrigger, nextIndex) = NextTriggerCalculator.calculateNextTrigger(
            currentTime,
            rule,
            reminder.currentStepIndex
        )

        runBlocking {
            db.reminderDao().update(
                reminder.copy(
                    nextTriggerTimeMillis = nextTrigger,
                    currentStepIndex = nextIndex
                )
            )
        }

        if (nextTrigger != null) {
            val updatedReminder = runBlocking { db.reminderDao().getReminder(reminderId) }
            if (updatedReminder != null) {
                AlarmScheduler(context).schedule(updatedReminder)
            }
        }

        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("reminder_id", reminder.id)
        }
        context.startActivity(alarmIntent)
    }
}
```

- [ ] **Step 4: Create `BootReceiver.kt`**

```kotlin
package cn.zibiren.herlarmay.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cn.zibiren.herlarmay.data.db.HerlarmayDatabase
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val db = HerlarmayDatabase.getInstance(context)
        val enabledReminders = runBlocking { db.reminderDao().getAllEnabled() }
        val scheduler = AlarmScheduler(context)
        enabledReminders.forEach { reminder ->
            if (reminder.nextTriggerTimeMillis != null) {
                scheduler.schedule(reminder)
            }
        }
    }
}
```

- [ ] **Step 5: Create `AlarmService.kt`**

```kotlin
package cn.zibiren.herlarmay.core.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import cn.zibiren.herlarmay.data.db.HerlarmayDatabase
import cn.zibiren.herlarmay.data.entity.ReminderEntity
import kotlinx.coroutines.runBlocking

class AlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val reminderId = intent?.getLongExtra("reminder_id", -1) ?: -1
        if (reminderId == -1L) return START_NOT_STICKY

        val db = HerlarmayDatabase.getInstance(this)
        val reminder = runBlocking { db.reminderDao().getReminder(reminderId) }
        if (reminder == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification(reminder)
        startForeground(NOTIFICATION_ID, notification)

        playRingtone(reminder.ringtoneUri)
        if (reminder.vibrate) {
            startVibrating()
        }

        return START_STICKY
    }

    private fun playRingtone(uri: String?) {
        try {
            val ringtoneUri = if (uri != null) Uri.parse(uri) else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmService, ringtoneUri)
                setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibrating() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = VibrationEffect.createWaveform(
                longArrayOf(0, 500, 500, 500),
                intArrayOf(0, 255, 0, 255),
                -1
            )
            vibrator?.vibrate(pattern)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 500, 500), 0)
        }
    }

    fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(reminder: ReminderEntity): Notification {
        val dismissIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = "DISMISS_ALARM"
            putExtra("reminder_id", reminder.id)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this, reminder.id.toInt(), dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(reminder.title)
            .setContentText(reminder.description)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(dismissPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Alarm", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Herlarmay alarm notifications"
            setSound(null, null)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "herlarmay_alarm"
        const val NOTIFICATION_ID = 1001
        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmService::class.java))
        }
    }
}
```

- [ ] **Step 6: Create `core/src/main/java/cn/zibiren/herlarmay/core/alarm/AlarmActivity.kt`**

(This is actually in the :app module conceptually since it's an activity, but to avoid a circular dependency where :core depends on :app, this needs to be in :app. However AlarmReceiver currently references AlarmActivity by class. Let me move AlarmReceiver's startActivity to use a class name string or put AlarmActivity in :core.)

Actually, the cleanest approach: AlarmsActivity stays in `:app`, and AlarmReceiver uses an intent with the full class name. The receiver can reference it without compile-time dependency. Let me adjust.

Actually wait. AlarmReceiver and BootReceiver currently use `HerlarmayDatabase.getInstance()` which is from `:data`. But `:core` doesn't depend on `:data`. This is a problem.

Let me rethink the dependency structure. Options:
1. `:core` depends on `:data` — breaks the clean separation
2. Have the receiver be a thin delegator that calls a callback registered by `:app`
3. Move the receivers to `:app` module, keep `:core` as pure utility

Actually option 3 is cleanest. Let me move `AlarmReceiver` and `BootReceiver` to `:app` module (since they need to coordinate between data and scheduling). `:core` can contain `AlarmScheduler`, `NextTriggerCalculator`, `AlarmService`.

Hmm, but the design says :core contains these. Let me reconsider.

Actually, the simplest approach: have :core depend on :data. In many Android projects, the core/infrastructure layer depends on data. Let me adjust:

- `:core` depends on `:data` (for ReminderEntity, database access)
- `:app` depends on `:core` (transitively gets :data)

This is pragmatic for the scope. Or alternatively, define interfaces in :core that :data implements. But that's overengineering for this app.

Let me go with: 
- `:core` depends on `:data`
- All alarm components live in `:core`
- `:app` depends on `:core`
- `:core/build.gradle.kts` adds `implementation(project(":data"))`

Let me update the plan accordingly.

- [ ] **Step 7: Update core/build.gradle.kts** (add data dependency)

Replace the dependencies block with:

```kotlin
dependencies {
    implementation(project(":data"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.gson)
}
```

- [ ] **Step 8: Build check**

Run: `cd F:\code\android\herlarmay && ./gradlew :core:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 4: Manifest and Application Setup

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/cn/zibiren/herlarmay/HerlarmayApplication.kt`

- [ ] **Step 1: Update `AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.USE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />

    <application
        android:name=".HerlarmayApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Herlarmay">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Herlarmay">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name="cn.zibiren.herlarmay.core.alarm.AlarmActivity"
            android:exported="false"
            android:launchMode="singleTask"
            android:showWhenLocked="true"
            android:turnScreenOn="true"
            android:theme="@style/Theme.Herlarmay" />

        <receiver
            android:name="cn.zibiren.herlarmay.core.alarm.AlarmReceiver"
            android:exported="false" />

        <receiver
            android:name="cn.zibiren.herlarmay.core.alarm.BootReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>

        <service
            android:name="cn.zibiren.herlarmay.core.alarm.AlarmService"
            android:exported="false"
            android:foregroundServiceType="specialUse" />

    </application>
</manifest>
```

- [ ] **Step 2: Create `HerlarmayApplication.kt`**

```kotlin
package cn.zibiren.herlarmay

import android.app.Application
import cn.zibiren.herlarmay.data.db.HerlarmayDatabase

class HerlarmayApplication : Application() {
    val database: HerlarmayDatabase by lazy { HerlarmayDatabase.getInstance(this) }
}
```

---

### Task 5: UI Screens — Reminder List

**Files:**
- Create: `app/src/main/java/cn/zibiren/herlarmay/ui/screens/list/ReminderListViewModel.kt`
- Create: `app/src/main/java/cn/zibiren/herlarmay/ui/screens/list/ReminderListScreen.kt`
- Create: `app/src/main/java/cn/zibiren/herlarmay/ui/components/ReminderCard.kt`

- [ ] **Step 1: Create `ReminderListViewModel.kt`**

```kotlin
package cn.zibiren.herlarmay.ui.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.zibiren.herlarmay.core.alarm.AlarmScheduler
import cn.zibiren.herlarmay.data.entity.ReminderEntity
import cn.zibiren.herlarmay.data.repository.ReminderRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ReminderListUiState(
    val reminders: List<ReminderEntity> = emptyList(),
    val isLoading: Boolean = true
)

class ReminderListViewModel(
    private val repository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReminderListUiState())
    val uiState: StateFlow<ReminderListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllReminders().collect { reminders ->
                _uiState.update { it.copy(reminders = reminders, isLoading = false) }
            }
        }
    }

    fun toggleEnabled(reminder: ReminderEntity) {
        viewModelScope.launch {
            val updated = reminder.copy(isEnabled = !reminder.isEnabled)
            repository.save(updated)
            if (updated.isEnabled && updated.nextTriggerTimeMillis != null) {
                alarmScheduler.schedule(updated)
            } else {
                alarmScheduler.cancel(reminder)
            }
        }
    }

    fun delete(reminder: ReminderEntity) {
        viewModelScope.launch {
            alarmScheduler.cancel(reminder)
            repository.delete(reminder)
        }
    }

    class Factory(
        private val repository: ReminderRepository,
        private val alarmScheduler: AlarmScheduler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReminderListViewModel(repository, alarmScheduler) as T
        }
    }
}
```

- [ ] **Step 2: Create `ReminderCard.kt`**

```kotlin
package cn.zibiren.herlarmay.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.zibiren.herlarmay.data.entity.ReminderEntity
import com.google.gson.Gson
import cn.zibiren.herlarmay.data.entity.RepeatRule
import cn.zibiren.herlarmay.data.entity.RepeatType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderCard(
    reminder: ReminderEntity,
    onToggleEnabled: (ReminderEntity) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val rule = remember(reminder.repeatRuleJson) {
        Gson().fromJson(reminder.repeatRuleJson, RepeatRule::class.java)
    }
    val scheduleText = remember(rule) {
        when (rule.type) {
            RepeatType.once -> "One-time"
            RepeatType.periodic -> {
                val unit = rule.periodUnit?.name ?: ""
                "Every ${rule.periodInterval ?: 1} $unit"
            }
            RepeatType.interval -> {
                if (rule.fixedIntervalMinutes != null) "Every ${rule.fixedIntervalMinutes} min"
                else "Variable interval"
            }
        }
    }

    SwipeToDismiss(
        state = rememberSwipeToDismissBoxState(
            confirmValueChange = {
                if (it == SwipeToDismissBoxValue.EndToStart) {
                    onDelete()
                    true
                } else false
            }
        ),
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        content = {
            Card(
                onClick = onClick,
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (reminder.isEnabled)
                        MaterialTheme.colorScheme.surface
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        if (reminder.nextTriggerTimeMillis != null) {
                            Text(
                                text = "Next: ${dateFormat.format(Date(reminder.nextTriggerTimeMillis))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Switch(
                        checked = reminder.isEnabled,
                        onCheckedChange = { onToggleEnabled(reminder) }
                    )
                }
            }
        }
    )
}
```

- [ ] **Step 3: Create `ReminderListScreen.kt`**

```kotlin
package cn.zibiren.herlarmay.ui.screens.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.zibiren.herlarmay.ui.components.ReminderCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    viewModel: ReminderListViewModel = viewModel(),
    onNavigateToCreate: () -> Unit = {},
    onNavigateToEdit: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Herlarmay") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreate) {
                Icon(Icons.Default.Add, contentDescription = "Create reminder")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.reminders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No reminders yet.\nTap + to create one.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
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
                        onDelete = { viewModel.delete(reminder) }
                    )
                }
            }
        }
    }
}
```

---

### Task 6: UI Screens — Edit Reminder

**Files:**
- Create: `app/src/main/java/cn/zibiren/herlarmay/ui/screens/edit/EditReminderViewModel.kt`
- Create: `app/src/main/java/cn/zibiren/herlarmay/ui/screens/edit/EditReminderScreen.kt`

- [ ] **Step 1: Create `EditReminderViewModel.kt`**

```kotlin
package cn.zibiren.herlarmay.ui.screens.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.zibiren.herlarmay.core.alarm.AlarmScheduler
import cn.zibiren.herlarmay.core.alarm.NextTriggerCalculator
import cn.zibiren.herlarmay.data.entity.*
import cn.zibiren.herlarmay.data.repository.ReminderRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class EditReminderUiState(
    val title: String = "",
    val description: String = "",
    val startTimeMillis: Long = System.currentTimeMillis() + 3600000,
    val repeatType: RepeatType = RepeatType.once,
    val periodUnit: PeriodUnit = PeriodUnit.day,
    val periodInterval: Int = 1,
    val fixedIntervalMinutes: Int = 60,
    val variableIntervals: List<Int> = emptyList(),
    val variableLoop: Boolean = true,
    val ringtoneUri: String? = null,
    val vibrate: Boolean = true,
    val isEditing: Boolean = false,
    val isLoading: Boolean = false
)

class EditReminderViewModel(
    private val reminderId: Long,
    private val repository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditReminderUiState())
    val uiState: StateFlow<EditReminderUiState> = _uiState.asStateFlow()

    init {
        if (reminderId != -1L) {
            loadReminder(reminderId)
        }
    }

    private fun loadReminder(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val reminder = repository.getReminder(id) ?: return@launch
            val rule = Gson().fromJson(reminder.repeatRuleJson, RepeatRule::class.java)
            _uiState.update {
                it.copy(
                    title = reminder.title,
                    description = reminder.description,
                    startTimeMillis = reminder.startTimeMillis,
                    repeatType = rule.type,
                    periodUnit = rule.periodUnit ?: PeriodUnit.day,
                    periodInterval = rule.periodInterval ?: 1,
                    fixedIntervalMinutes = rule.fixedIntervalMinutes ?: 60,
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

    fun updateTitle(title: String) { _uiState.update { it.copy(title = title) } }
    fun updateDescription(desc: String) { _uiState.update { it.copy(description = desc) } }
    fun updateStartTime(time: Long) { _uiState.update { it.copy(startTimeMillis = time) } }
    fun updateRepeatType(type: RepeatType) { _uiState.update { it.copy(repeatType = type) } }
    fun updatePeriodUnit(unit: PeriodUnit) { _uiState.update { it.copy(periodUnit = unit) } }
    fun updatePeriodInterval(interval: Int) { _uiState.update { it.copy(periodInterval = interval) } }
    fun updateFixedInterval(minutes: Int) { _uiState.update { it.copy(fixedIntervalMinutes = minutes) } }
    fun updateVariableLoop(loop: Boolean) { _uiState.update { it.copy(variableLoop = loop) } }
    fun updateVibrate(v: Boolean) { _uiState.update { it.copy(vibrate = v) } }

    fun addVariableInterval(minutes: Int) {
        _uiState.update { it.copy(variableIntervals = it.variableIntervals + minutes) }
    }

    fun removeVariableInterval(index: Int) {
        _uiState.update {
            it.copy(variableIntervals = it.variableIntervals.toMutableList().also { list ->
                if (index in list.indices) list.removeAt(index)
            })
        }
    }

    private fun buildReminder(): ReminderEntity {
        val state = _uiState.value
        val rule = RepeatRule(
            type = state.repeatType,
            periodUnit = if (state.repeatType == RepeatType.periodic) state.periodUnit else null,
            periodInterval = if (state.repeatType == RepeatType.periodic) state.periodInterval else null,
            fixedIntervalMinutes = if (state.repeatType == RepeatType.interval) state.fixedIntervalMinutes else null,
            variableIntervals = if (state.repeatType == RepeatType.interval && state.variableIntervals.isNotEmpty()) state.variableIntervals else null,
            variableLoop = if (state.repeatType == RepeatType.interval) state.variableLoop else true
        )
        val ruleJson = Gson().toJson(rule)
        val nextTrigger = NextTriggerCalculator.calculateNextTrigger(
            state.startTimeMillis, rule
        ).first

        return ReminderEntity(
            id = if (state.isEditing) reminderId else 0,
            title = state.title,
            description = state.description,
            startTimeMillis = state.startTimeMillis,
            repeatRuleJson = ruleJson,
            ringtoneUri = state.ringtoneUri,
            vibrate = state.vibrate,
            isEnabled = true,
            nextTriggerTimeMillis = nextTrigger
        )
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            val reminder = buildReminder()
            repository.save(reminder)
            if (reminder.isEnabled && reminder.nextTriggerTimeMillis != null) {
                alarmScheduler.schedule(reminder)
            }
            onSaved()
        }
    }

    class Factory(
        private val reminderId: Long,
        private val repository: ReminderRepository,
        private val alarmScheduler: AlarmScheduler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditReminderViewModel(reminderId, repository, alarmScheduler) as T
        }
    }
}
```

- [ ] **Step 2: Create `EditReminderScreen.kt`**

```kotlin
package cn.zibiren.herlarmay.ui.screens.edit

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cn.zibiren.herlarmay.data.entity.PeriodUnit
import cn.zibiren.herlarmay.data.entity.RepeatType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReminderScreen(
    viewModel: EditReminderViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = uiState.startTimeMillis }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, day)
                viewModel.updateStartTime(cal.timeInMillis)
                showDatePicker = false
                showTimePicker = true
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
            show()
        }
        showDatePicker = false
    }

    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = uiState.startTimeMillis }
        TimePickerDialog(
            context,
            { _, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                viewModel.updateStartTime(cal.timeInMillis)
                showTimePicker = false
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
        showTimePicker = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Edit Reminder" else "New Reminder") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save(onNavigateBack) },
                        enabled = uiState.title.isNotBlank()
                    ) {
                        Text("Save")
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
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // Date/Time picker
            Text("Start Time", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(dateFormat.format(Date(uiState.startTimeMillis)))
            }

            // Repeat type
            Text("Type", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RepeatType.entries.forEach { type ->
                    FilterChip(
                        selected = uiState.repeatType == type,
                        onClick = { viewModel.updateRepeatType(type) },
                        label = { Text(type.name.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // Periodic options
            if (uiState.repeatType == RepeatType.periodic) {
                Text("Repeat", style = MaterialTheme.typography.labelLarge)
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
                            label = { Text("${unit.name}s") }
                        )
                    }
                }
            }

            // Interval options
            if (uiState.repeatType == RepeatType.interval) {
                Text("Interval", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = uiState.fixedIntervalMinutes.toString(),
                    onValueChange = { viewModel.updateFixedInterval(it.toIntOrNull() ?: 60) },
                    label = { Text("Fixed interval (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Variable intervals", style = MaterialTheme.typography.labelLarge)
                var newInterval by remember { mutableStateOf("") }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newInterval,
                        onValueChange = { newInterval = it },
                        label = { Text("Minutes") },
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
                        Icon(Icons.Default.Add, contentDescription = "Add interval")
                    }
                }
                uiState.variableIntervals.forEachIndexed { index, minutes ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("${index + 1}. $minutes min")
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { viewModel.removeVariableInterval(index) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove")
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Loop sequence")
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = uiState.variableLoop,
                        onCheckedChange = { viewModel.updateVariableLoop(it) }
                    )
                }
            }

            // Vibrate toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Vibrate", style = MaterialTheme.typography.labelLarge)
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

---

### Task 7: UI — AlarmActivity (Full-screen Alarm)

**Files:**
- Create: `core/src/main/java/cn/zibiren/herlarmay/core/alarm/AlarmActivity.kt`

- [ ] **Step 1: Update core/build.gradle.kts** (add Compose to :core for AlarmActivity)

Replace the plugins block and add compose:

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "cn.zibiren.herlarmay.core"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":data"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.gson)
}
```

- [ ] **Step 2: Create `AlarmActivity.kt`**

```kotlin
package cn.zibiren.herlarmay.core.alarm

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.zibiren.herlarmay.data.db.HerlarmayDatabase
import cn.zibiren.herlarmay.data.entity.ReminderEntity
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class AlarmActivity : ComponentActivity() {
    private var reminder: ReminderEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        val reminderId = intent.getLongExtra("reminder_id", -1)
        if (reminderId != -1L) {
            val db = HerlarmayDatabase.getInstance(this)
            reminder = runBlocking { db.reminderDao().getReminder(reminderId) }
        }

        setContent {
            MaterialTheme {
                AlarmScreen(
                    reminder = reminder,
                    onDismiss = { finishAlarm() },
                    onSnooze = { minutes -> snooze(minutes) }
                )
            }
        }
    }

    private fun finishAlarm() {
        AlarmService.stop(this)
        finish()
    }

    private fun snooze(minutes: Int) {
        val r = reminder ?: return
        val newTrigger = System.currentTimeMillis() + minutes * 60000L
        val db = HerlarmayDatabase.getInstance(this)
        val resetReminder = r.copy(
            nextTriggerTimeMillis = newTrigger,
            currentStepIndex = 0
        )
        runBlocking {
            db.reminderDao().update(resetReminder)
        }
        AlarmScheduler(this).schedule(resetReminder)
        AlarmService.stop(this)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        AlarmService.stop(this)
    }
}

@Composable
private fun AlarmScreen(
    reminder: ReminderEntity?,
    onDismiss: () -> Unit,
    onSnooze: (Int) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = reminder?.title ?: "Alarm",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        if (!reminder?.description.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = reminder?.description ?: "",
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = dateFormat.format(Date()),
            fontSize = 48.sp,
            fontWeight = FontWeight.Light
        )

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Confirm", fontSize = 18.sp)
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(5, 10, 30).forEach { minutes ->
                OutlinedButton(onClick = { onSnooze(minutes) }) {
                    Text("$minutes min")
                }
            }
        }
    }
}
```

---

### Task 8: Navigation and MainActivity

**Files:**
- Create: `app/src/main/java/cn/zibiren/herlarmay/navigation/NavGraph.kt`
- Modify: `app/src/main/java/cn/zibiren/herlarmay/MainActivity.kt`

- [ ] **Step 1: Create `NavGraph.kt`**

```kotlin
package cn.zibiren.herlarmay.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import cn.zibiren.herlarmay.core.alarm.AlarmScheduler
import cn.zibiren.herlarmay.data.db.HerlarmayDatabase
import cn.zibiren.herlarmay.data.repository.ReminderRepository
import cn.zibiren.herlarmay.ui.screens.edit.EditReminderScreen
import cn.zibiren.herlarmay.ui.screens.edit.EditReminderViewModel
import cn.zibiren.herlarmay.ui.screens.list.ReminderListScreen
import cn.zibiren.herlarmay.ui.screens.list.ReminderListViewModel

object Routes {
    const val LIST = "list"
    const val EDIT = "edit/{id}"
    fun edit(id: Long) = "edit/$id"
}

@Composable
fun HerlarmayNavGraph(
    navController: NavHostController,
    database: HerlarmayDatabase,
    alarmScheduler: AlarmScheduler
) {
    val repository = ReminderRepository(database.reminderDao())

    NavHost(navController = navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            val viewModel: ReminderListViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = ReminderListViewModel.Factory(repository, alarmScheduler)
            )
            ReminderListScreen(
                viewModel = viewModel,
                onNavigateToCreate = { navController.navigate(Routes.edit(-1L)) },
                onNavigateToEdit = { id -> navController.navigate(Routes.edit(id)) }
            )
        }

        composable(
            route = Routes.EDIT,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: -1L
            val viewModel: EditReminderViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = EditReminderViewModel.Factory(id, repository, alarmScheduler)
            )
            EditReminderScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
```

- [ ] **Step 2: Update `MainActivity.kt`**

```kotlin
package cn.zibiren.herlarmay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import cn.zibiren.herlarmay.core.alarm.AlarmScheduler
import cn.zibiren.herlarmay.navigation.HerlarmayNavGraph
import cn.zibiren.herlarmay.ui.theme.HerlarmayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as HerlarmayApplication
        val alarmScheduler = AlarmScheduler(this)

        setContent {
            HerlarmayTheme {
                val navController = rememberNavController()
                HerlarmayNavGraph(
                    navController = navController,
                    database = app.database,
                    alarmScheduler = alarmScheduler
                )
            }
        }
    }
}
```

- [ ] **Step 3: Verify full build**

Run: `cd F:\code\android\herlarmay && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL
