<div align="center">

# ⏰ Herlarmay

**强提醒 · 不遗漏**

一款轻量级 Android 强提醒应用，专注解决重要事项不被遗漏的问题。
支持一次性、周期性、固定间隔、不等间隔等多种提醒模式。

[![Min SDK](https://img.shields.io/badge/minSdk-24-8A2BE2)]()
[![Target SDK](https://img.shields.io/badge/targetSdk-36-8A2BE2)]()
[![Kotlin](https://img.shields.io/badge/kotlin-2.2.10-orange)]()
[![Compose BOM](https://img.shields.io/badge/Compose_BOM-2026.02.01-blue)]()
[![License](https://img.shields.io/badge/license-MIT-green)]()

</div>

---

## ✨ 功能特性

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

## 🏗️ 技术栈

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

## 📋 环境要求

- Android 7.0（API 24）及以上
- Gradle 9.4.1
- AGP 9.2.1

## 🚀 快速开始

```bash
# 克隆项目
git clone https://github.com/your-username/herlarmay.git

# 打开 Android Studio，等待 Gradle sync 完成

# 直接运行
./gradlew installDebug
```

## 📁 项目结构

```
app/
├── src/main/java/cn/zibiren/herlarmay/
│   ├── core/alarm/          # 闹钟调度、服务、广播接收
│   ├── data/
│   │   ├── db/              # Room 数据库 + DAO
│   │   ├── entity/          # 数据实体 + 重复规则
│   │   └── repository/      # 数据仓库
│   ├── navigation/          # 导航图
│   ├── ui/
│   │   ├── components/      # 公共组件（ReminderCard）
│   │   ├── screens/
│   │   │   ├── edit/        # 创建/编辑提醒
│   │   │   └── list/        # 提醒列表
│   │   └── theme/           # Material 3 主题
│   ├── MainActivity.kt
│   └── HerlarmayApplication.kt
├── src/main/AndroidManifest.xml
└── build.gradle.kts
```

## ⚙️ 提醒类型说明

### 一次性

选择星期 + 时刻，在下一个匹配的星期时刻触发一次后结束。

### 周期性

- **每天**：每隔 N 天触发一次，可用星期过滤
- **每周**：每隔 N 周触发一次，自动对齐选中星期
- **每月**：每隔 N 月触发一次（按 30 天近似计算）

### 间隔

- **固定间隔**：每隔 N 秒/分钟/小时触发一次，可用星期过滤
- **不等间隔**：自定义分钟序列（如 5、10、30），可选循环

## 🔧 MIUI / 定制 ROM 适配

部分国产 ROM（MIUI、EMUI 等）会拦截后台广播和 AlarmManager 调度，需要：

1. **安全中心 → 自启动 → 开启 Herlarmay**
2. **设置 → 省电策略 → 无限制**
3. **最近任务 → 下拉锁定应用**

否则可能导致后台闹钟不触发。

## 📄 开源协议

[MIT License](LICENSE)

---

<div align="center">
  <sub>Built with ❤️ using Jetpack Compose</sub>
</div>
