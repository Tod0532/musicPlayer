<div align="center">

# 🎵 Melody

### 仿 QQ 音乐 · 原生 Android 本地音乐播放器

学习现代 Android 开发架构的本地音乐播放器，UI 仿 QQ 音乐风格，支持本地音乐扫描、真实播放、歌单管理、在线搜索、歌词联动。

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.12-4285F4?logo=jetpackcompose&logoColor=white)
![Material 3](https://img.shields.io/badge/Material%203-Design-757575?logo=materialdesign&logoColor=white)
![Media3](https://img.shields.io/badge/Media3-1.4.1-34A853?logo=android&logoColor=white)
![Room](https://img.shields.io/badge/Room-2.6.1-FF6F00?logo=android&logoColor=white)
![Android](https://img.shields.io/badge/Android-API%2026+-3DDC84?logo=android&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow)

![minSdk](https://img.shields.io/badge/minSdk-26-red)
![targetSdk](https://img.shields.io/badge/targetSdk-34-green)
![APK Size](https://img.shields.io/badge/APK%20Size-23MB-blue)

**原生 Kotlin + Jetpack Compose · 非 WebView · 非跨平台**

</div>

---

> 📱 **Android 8.0+** · 🎵 **Media3 流式播放** · 📂 **Room 歌单管理** · 🎤 **LRC 歌词联动** · 🔔 **后台播放 + 通知栏**
>
> 📊 24 个 Kotlin 文件 · ~3196 行代码 · 7 个 Git 提交 · 9/9 MVP 功能完成

---

## 目录

- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [架构设计](#架构设计)
- [核心功能实现](#核心功能实现)
- [项目结构](#项目结构)
- [构建与运行](#构建与运行)
- [设计文档](#设计文档)
- [版本历史](#版本历史)

---

## 功能特性

### 9 项 MVP 功能（全部完成）

| ID | 功能 | 说明 |
|---|---|---|
| F1 | 本地音乐扫描 | MediaStore 扫描设备音乐（mp3/m4a/flac），过滤铃声片段，含扫描按钮 |
| F2 | 播放器全屏页 | 沉浸式封面背景 + 封面旋转动画 + 进度条 + 控件 + 歌词 |
| F3 | 底部迷你播放条 | 全局悬浮控制，常驻所有页面，点击展开播放页 |
| F4 | 后台播放 + 通知栏 | 切后台/锁屏继续播放，系统通知栏 + 锁屏播放控件 + 蓝牙按键 |
| F5 | 自建歌单 | Room 数据库 CRUD，歌单列表/详情双模式，加入/移除歌曲 |
| F6 | 我喜欢 | 收藏歌曲，内嵌字段高频读写 |
| F7 | 搜索（在线 + 本地） | 在线搜 CC 协议音乐；本地按歌名/歌手/专辑实时过滤 |
| F8 | 歌词联动 | LRC 解析，封面/歌词双视图，自动滚动高亮，点击跳转 |
| F9 | 播放模式 | 顺序播放 / 随机播放 / 单曲循环 |
| **F10** | **AI 资讯播报** | HackerNews+GitHub+RSSHub 聚合，ML Kit 英文→中文翻译，TTS 连续朗读 |

### 额外体验优化

- 播放控制健壮性：Service 未连接时暂存播放请求
- 进度条可拖拽：拖动时实时预览时间，松手才 seek
- 本地搜索过滤：我的音乐页实时筛选
- 歌单详情页：双模式导航，加入当前歌曲

---

## 技术栈

### 语言与构建

| 技术 | 版本 | 用途 |
|---|---|---|
| Kotlin | 2.0.21 | 主开发语言 |
| Gradle | 8.10.2 | 构建工具 |
| AGP | 8.7.3 | Android Gradle Plugin |
| KSP | 2.0.21-1.0.27 | Room 注解处理（替代 KAPT，更快） |
| JDK | 17 | 编译环境 |
| minSdk | 26（Android 8.0） | 覆盖 ~98% 设备 |
| targetSdk | 34 | Android 14 |

### UI 层（Jetpack Compose 全家桶）

| 库 | 版本 | 用途 |
|---|---|---|
| Compose BOM | 2024.12.01 | 统一 Compose 版本管理 |
| Material 3 | BOM 管理 | Material Design 3 组件系统 |
| Material Icons Extended | BOM 管理 | 图标库 |
| Activity Compose | 1.9.3 | Compose 与 Activity 集成 |
| Lifecycle ViewModel Compose | 2.8.7 | ViewModel + Compose 状态管理 |

### 播放与媒体

| 库 | 版本 | 用途 |
|---|---|---|
| Media3 ExoPlayer | 1.4.1 | 音频解码播放引擎（替代旧 ExoPlayer） |
| Media3 Session | 1.4.1 | 后台播放 + 通知栏 + 锁屏控件（MediaSessionService） |
| Media3 UI / Common | 1.4.1 | 媒体组件支持 |

### 数据层

| 库 | 版本 | 用途 |
|---|---|---|
| Room | 2.6.1 | 本地关系型数据库（歌单 CRUD） |
| Kotlinx Coroutines + Flow | 默认 | 异步任务 + 响应式数据流 |
| Navigation Compose | 2.8.5 | 页面路由（预留扩展） |

---

## 架构设计

采用 **Clean Architecture 分层 + MVVM** 模式，单向依赖，每层职责清晰。

```
┌──────────────────────────────────────────────────────┐
│  📱 UI 层 (app/ui)                                   │
│  Composable 屏幕 ← 观察 → ViewModel 的 StateFlow     │
│  ├── theme/        主题配色（仿 QQ 音乐深色风格）     │
│  ├── mymusic/      我的音乐页 + 本地搜索              │
│  ├── player/       播放器全屏页（封面/歌词双视图）    │
│  ├── search/       在线搜索页                        │
│  ├── playlist/     歌单页（列表/详情双模式）          │
│  ├── components/   复用组件（底部播放条）             │
│  └── PlayerViewModel  统一状态管理                   │
├──────────────────────────────────────────────────────┤
│  🧠 Domain 层 (app/domain)                           │
│  纯 Kotlin 模型（无 Android 依赖）                    │
│  Song / LyricLine / PlayState / PlayMode             │
├──────────────────────────────────────────────────────┤
│  💾 Data 层 (app/data)                               │
│  ├── local/     Room 实体 + DAO + Database           │
│  ├── online/    在线搜索（CC 音源 + MusicBrainz）     │
│  ├── MusicScanner  MediaStore 本地扫描               │
│  └── SampleData    内置示例数据                      │
├──────────────────────────────────────────────────────┤
│  🎵 Media 层 (app/media)                             │
│  ├── PlaybackService       后台 MediaSessionService  │
│  ├── MediaServiceConnection 连接管理 + 待播放队列     │
│  ├── MusicPlayerController  ExoPlayer 封装            │
│  └── LyricsParser          LRC 歌词解析              │
└──────────────────────────────────────────────────────┘
```

### 关键架构原则

- **单向依赖**：UI → Domain ← Data，无循环引用
- **响应式数据流**：所有状态用 `StateFlow`，Compose UI 自动重组
- **单一 ViewModel**：`PlayerViewModel` 集中管理播放/搜索/歌单状态，避免状态分散
- **Domain 层纯 Kotlin**：领域模型无 Android 框架依赖，可独立单元测试

---

## 核心功能实现

### F1. 本地音乐扫描

```
MusicScanner.scan(context)
    ↓
查询 MediaStore.Audio.Media (IS_MUSIC=1, duration > 10s)
    ↓
按标题排序，限制 200 首
    ↓
返回 List<Song>（含 content:// URI）
```

- Android 13+ 用 `READ_MEDIA_AUDIO` 权限，旧版本降级 `READ_EXTERNAL_STORAGE`
- 扫描在 `Dispatchers.IO` 协程执行，不阻塞 UI

### F4. 后台播放 + 通知栏（核心架构）

```
UI 层:  MediaController (异步连接)
          ↕
Service: PlaybackService extends MediaSessionService
          ↓
系统自动生成:
  - 媒体通知栏（播放/暂停/上下首）
  - 锁屏播放控件
  - 蓝牙/耳机按键支持
  - 前台服务保活
```

- **`PlaybackService`** 继承 `MediaSessionService`，系统自动管理通知栏
- **`MediaServiceConnection`** 封装 `MediaController` 异步连接，带**待播放队列**（Service 未连上时暂存请求，连上后自动执行）
- 配置 `FOREGROUND_SERVICE_MEDIA_PLAYBACK` + `POST_NOTIFICATIONS` 权限

### F5. 自建歌单（Room 数据库）

```
PlaylistEntity (playlists 表)
    │  id, name, createdAt
    └──< PlaylistSongEntity (playlist_song 表)
            playlistId, songId, 冗余歌曲信息
            复合主键 (playlistId, songId) ← 天然防重复
```

- **Flow 观察**：增删自动刷新列表，无需手动刷新
- 歌单详情页观察 `observeSongsInPlaylist()`
- `PlaylistSongEntity.toSong()` 转换为可播放模型

### F7. 在线搜索（合法多音源聚合）

```
输入关键词 → OnlineMusicSource.searchWithPlayUrl(query)
              ↓
返回 List<SearchResult>（每个带 playUrl）
  ├── SoundHelix（CC BY 4.0 协议音乐，8 首，可流式播放）
  └── MusicBrainz（开放元数据，歌名/歌手检索）
              ↓
点击 → playUrl 交给 Media3 流式播放（非下载）
```

- **搜索与播放解耦**：`SearchResult` 是独立模型，`toSong()` 转换为播放模型
- **本地搜索**：MyMusicScreen 内部 `remember` 过滤，实时响应
- **合法边界**：只播放 CC 协议音乐和开放元数据，不碰版权内容

### F8. 歌词联动

```
播放歌曲 → LyricsParser.parseFromAssets(lrcAsset)
            ↓
解析 [mm:ss.xx]歌词 格式（支持多时间戳行）
            ↓
List<LyricLine>（按时间排序）
            ↓
封面/歌词双视图切换：
  - 封面模式：旋转动画 + 底部三行歌词预览
  - 歌词模式：完整歌词自动滚动 + 当前行高亮放大 + 距离渐隐
            ↓
点击任意歌词行 → seekTo(该行时间)
```

---

## 项目结构

```
Melody/
├── app/
│   ├── build.gradle.kts                    # 应用模块构建配置
│   └── src/main/
│       ├── AndroidManifest.xml             # 清单（权限 + Service 注册）
│       ├── assets/                         # 内置资源
│       │   ├── song_qingtian.wav/.lrc      # 示例曲（音频 + 歌词）
│       │   ├── song_qilixiang.wav/.lrc
│       │   └── song_daoxiang.wav/.lrc
│       ├── res/                            # 资源（主题/颜色/图标）
│       └── java/com/melody/app/
│           ├── MainActivity.kt             # 入口 + 主导航
│           ├── data/
│           │   ├── MusicScanner.kt         # MediaStore 本地扫描
│           │   ├── SampleData.kt           # 内置示例数据
│           │   ├── local/                  # Room 数据库
│           │   │   ├── Entities.kt         # 歌单/关联实体
│           │   │   ├── PlaylistDao.kt      # 数据访问对象
│           │   │   └── MelodyDatabase.kt   # 数据库实例
│           │   └── online/                 # 在线音源
│           │       ├── OnlineMusicSource.kt
│           │       └── SearchResult.kt
│           ├── domain/
│           │   └── model/Song.kt           # 领域模型（纯 Kotlin）
│           ├── media/                      # 媒体播放层
│           │   ├── PlaybackService.kt      # 后台服务
│           │   ├── MediaServiceConnection.kt
│           │   ├── MusicPlayerController.kt
│           │   └── LyricsParser.kt         # LRC 解析
│           └── ui/
│               ├── theme/                  # 主题配色
│               ├── mymusic/                # 我的音乐页
│               ├── player/                 # 播放器全屏页
│               ├── search/                 # 在线搜索页
│               ├── playlist/               # 歌单页
│               ├── components/             # 复用组件
│               ├── CoverPlaceholder.kt     # 封面占位
│               ├── PlayerViewModel.kt      # 主状态管理
│               └── Util.kt                 # 工具函数
├── docs/superpowers/specs/                 # 设计文档
│   └── 2026-06-24-melody-music-player-design.md
├── gradle/
│   ├── libs.versions.toml                  # 版本目录
│   └── wrapper/                            # Gradle Wrapper
├── build.gradle.kts                        # 根构建文件
├── settings.gradle.kts                     # 项目设置
└── README.md                               # 本文档
```

---

## 构建与运行

### 环境要求

- **Android Studio**（任意近期版本，用于 IDE 打开）
- **JDK 17**
- **Android SDK**：build-tools 36.1.0、platform android-34

### 命令行构建

```bash
# 设置环境变量
export ANDROID_HOME=/path/to/Android/Sdk
export JAVA_HOME=/path/to/jdk-17

# 构建 debug APK
./gradlew assembleDebug

# APK 输出位置
# app/build/outputs/apk/debug/app-debug.apk
```

### 网络镜像（中国大陆环境）

由于默认 Gradle/Maven 源访问慢，已配置镜像加速：

- **Gradle 下载**：`gradle/wrapper/gradle-wrapper.properties`
  - 腾讯云：`https://mirrors.cloud.tencent.com/gradle/`
- **Maven 依赖**：`settings.gradle.kts`
  - 阿里云：`https://maven.aliyun.com/repository/{google,central,gradle-plugin}`

### 安装到真机

```bash
# 连接手机（开启 USB 调试）
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 启动
adb shell am start -n com.melody.app/.MainActivity
```

### 真机测试注意事项

- 首次启动需授予 `READ_MEDIA_AUDIO`（读取音乐）和 `POST_NOTIFICATIONS`（通知栏）权限
- 扫描本地音乐需手机内有音频文件；无文件时显示内置示例曲
- 后台播放测试：播放后按 Home 键，检查通知栏是否有媒体控件

---

## 设计文档

完整设计文档位于 `docs/superpowers/specs/2026-06-24-melody-music-player-design.md`，包含：

- 项目定位与功能范围
- 技术栈选型理由
- 信息架构与导航（三层结构）
- 技术架构分层（Clean Architecture + MVVM）
- 数据模型（Room Schema，5 张表设计）
- UI 设计规范（配色方案、播放页布局）
- 错误处理与边界场景
- 测试策略

---

## 版本历史

| 版本 | 内容 |
|---|---|
| v1.0.0 | UI 骨架 + 真实播放 + MediaStore 扫描 + 在线搜索 + 后台播放 + 歌单 + 歌词 + 体验优化 |

### Git 提交历史

```
6f36e3c  feat: 体验优化（播放健壮性/本地搜索/歌单详情/进度条拖拽）
c62db9f  feat: F4 后台播放 / F8 歌词联动 / F5 自建歌单
c37aea1  feat: MediaStore 扫描 + 在线搜索 + Media3 流式播放
67eb287  feat: UI 优化（状态栏/封面渐变/播放条投影）
bf68bf1  feat: Melody UI 骨架（首个可运行 APK）
2a10c52  docs: 设计文档 (spec)
```

---

## 关键技术决策

| 决策 | 选择 | 理由 |
|---|---|---|
| 播放器 | Media3（非旧 ExoPlayer） | Google 现主推，内置 MediaSession 集成 |
| 后台播放 | MediaSessionService | 系统级通知/锁屏/蓝牙一体化，无需手写通知 |
| 数据库 | Room + KSP | 编译期 SQL 校验，比 KAPT 编译快 |
| 异步 | Flow + StateFlow | 与 Compose 响应式天然契合 |
| 音源 | CC 协议（SoundHelix） | 合法合规，避免版权风险 |
| 状态管理 | 单一 AndroidViewModel | 集中管理，避免状态分散 |

---

## 后续可扩展方向

- **接入 Jamendo**：CC 协议音乐平台（几万首），需免费注册 client_id
- **试听缓存**：听过的 CC 音乐存本地，离线可听
- **专辑/歌手分类**：基于扫描结果聚合浏览
- **真实封面加载**：接入 Coil + MediaStore 封面 URI
- **主题切换**：跟随系统深浅色模式
- **应用图标**：专业设计图标

---

*Melody 项目 · 仿 QQ 音乐本地播放器 · 原生 Kotlin + Jetpack Compose*
