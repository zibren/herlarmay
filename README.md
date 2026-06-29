<div align="center">

# ⏰ Herlarmay（喝了没）

🍵 **一款轻量级 Android 提醒应用** · A lightweight reminder app for Android

[![Min SDK](https://img.shields.io/badge/minSdk-24-8A2BE2)]()
[![Target SDK](https://img.shields.io/badge/targetSdk-36-8A2BE2)]()
[![Kotlin](https://img.shields.io/badge/kotlin-2.2.10-orange)]()
[![Compose BOM](https://img.shields.io/badge/Compose_BOM-2026.02.01-blue)]()

</div>

---

## 🇨🇳 中文

### 简介

Herlarmay（喝了没）是一款轻量级 Android 提醒应用。支持一次性、周期性、固定间隔和不等间隔等多种提醒模式，通过前台服务 + AlarmManager 确保消息可靠送达。

### 功能特性

| 功能 | 说明 |
|------|------|
| **一次性提醒** | 指定星期 + 时刻，触发后自动结束 |
| **周期性重复** | 按天 / 周 / 月循环，可限定星期 |
| **固定间隔** | 按秒 / 分钟 / 小时循环触发 |
| **不等间隔** | 自定义间隔序列，支持循环播放 |
| **星期过滤** | 多选星期，仅选中日触发 |
| **前台服务** | 保活播放铃声 + 震动，进程被杀也能恢复 |
| **全屏提醒** | 锁屏点亮屏幕，确认 / 稍后 / 清除 |
| **本地持久化** | Room 数据库，关闭应用不丢数据 |

### 技术栈

| 层级 | 技术 |
|------|------|
| 语言 | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Repository |
| 数据库 | Room 2.7.1（KSP） |
| 导航 | Navigation Compose 2.8.9 |
| 序列化 | Gson 2.12.1 |
| 调度 | AlarmManager（setAlarmClock / setExactAndAllowWhileIdle） |
| 后台 | Foreground Service + BroadcastReceiver |

### 环境要求

- Android 7.0（API 24）及以上
- Gradle 9.4.1 / AGP 9.2.1

### 快速开始

```bash
git clone https://github.com/your-username/herlarmay.git
# Android Studio 打开 → Gradle Sync → Run
```

### 项目结构

```
app/
├── src/main/java/cn/zibiren/herlarmay/
│   ├── core/alarm/          # 闹钟调度、前台服务、广播
│   ├── data/
│   │   ├── db/              # Room 数据库 + DAO
│   │   ├── entity/          # 数据实体 + 重复规则
│   │   └── repository/      # 数据仓库
│   ├── navigation/          # Compose 导航图
│   ├── ui/
│   │   ├── components/      # ReminderCard 等公共组件
│   │   ├── screens/edit/    # 创建 / 编辑提醒
│   │   ├── screens/list/    # 提醒列表
│   │   └── theme/           # Material 3 主题
│   ├── MainActivity.kt
│   └── HerlarmayApplication.kt
└── build.gradle.kts
```

### 提醒类型

| 类型 | 说明 |
|------|------|
| **一次性** | 选择星期 + 时刻，下次匹配时触发一次 |
| **周期性** | 每 N 天/周/月，可用星期过滤 |
| **固定间隔** | 每 N 秒/分钟/小时 |
| **不等间隔** | 自定义分钟序列，可选循环 |

### MIUI 适配

国产 ROM（MIUI 等）默认拦截后台闹钟，需手动设置：

1. 安全中心 → 自启动 → 开启 Herlarmay
2. 设置 → 省电策略 → 无限制
3. 最近任务 → 下拉锁定

---

## 🇬🇧 English

### Introduction

**Herlarmay（喝了没）** is a lightweight Android reminder app. It supports one-time, periodic (daily/weekly/monthly), fixed-interval, and variable-interval reminders. A foreground service combined with AlarmManager ensures reliable delivery even when the app is backgrounded.

### Features

| Feature | Description |
|---------|-------------|
| **One-time** | Fire once at the next matching weekday + time |
| **Periodic** | Repeat every N days/weeks/months, with weekday filtering |
| **Fixed interval** | Repeat every N seconds/minutes/hours |
| **Variable interval** | Custom minute sequence, optional loop |
| **Weekday filter** | Only fire on selected days of the week |
| **Foreground service** | Persistent playback with ringtone + vibration |
| **Full-screen alert** | Wake & unlock, with dismiss / snooze / clear actions |
| **Local persistence** | Room database, survives app restart |

### Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Repository |
| Database | Room 2.7.1（KSP） |
| Navigation | Navigation Compose 2.8.9 |
| Serialization | Gson 2.12.1 |
| Scheduling | AlarmManager（setAlarmClock / setExactAndAllowWhileIdle） |
| Background | Foreground Service + BroadcastReceiver |

### Requirements

- Android 7.0（API 24）+ 
- Gradle 9.4.1 / AGP 9.2.1

### Quick Start

```bash
git clone https://github.com/your-username/herlarmay.git
# Open in Android Studio → Gradle Sync → Run
```

### Reminder Types

| Type | Description |
|------|-------------|
| **Once** | Pick time + weekdays, fires on next match |
| **Periodic** | Every N days/weeks/months with weekday filter |
| **Fixed interval** | Every N seconds/minutes/hours |
| **Variable interval** | Custom sequence of minutes, loops optionally |

### MIUI Note

On MIUI and other Chinese ROMs, background alarms are blocked by default. You must:

1. Security Center → Autostart → Enable Herlarmay
2. Settings → Battery → No restrictions
3. Recent tasks → Pull down to lock the app

---

<div align="center">
  <sub>Built with ❤️ using Jetpack Compose & Kotlin</sub>
</div>
