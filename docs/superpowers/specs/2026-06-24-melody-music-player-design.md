# Melody · 设计文档（Design Spec）

> **项目代号**：Melody
> **类型**：仿 QQ 音乐 · 本地音乐播放器（学习/练手项目）
> **创建日期**：2026-06-24
> **状态**：待用户审阅

---

## 1. 项目概述

### 1.1 一句话定位
一款学习现代 Android 开发架构的本地音乐播放器，UI 仿 QQ 音乐风格，扫描设备本地音乐并提供完整的播放、歌单、搜索、歌词体验。

### 1.2 目标
- 学习当前 Android 业界主流架构（Clean Architecture + MVVM + Hilt）
- 掌握音乐类 APP 的核心技术栈（Media3、Room、Compose）
- 产出一个 UI 美观、可真实使用的本地播放器

### 1.3 非目标（明确不做）
- ❌ 在线搜索 / 流媒体播放 / 账号系统（版权 + 复杂度）
- ❌ 专辑/歌手分类浏览、最近播放独立页、动态主题切换（后续迭代）
- ❌ 卡拉OK 逐字高亮歌词（依赖逐字时间戳数据，本地音乐几乎无此数据）
- ❌ 社交功能（评论/好友/动态）

---

## 2. 技术栈

| 层 | 选型 | 用途 |
|---|---|---|
| 语言 | Kotlin | 主开发语言 |
| UI | Jetpack Compose + Material 3 | 声明式 UI |
| 架构 | MVVM + Clean Architecture | 分层架构 |
| DI | Hilt | 依赖注入 |
| 本地存储 | Room | 关系型数据库 |
| 媒体播放 | Media3 / ExoPlayer + MediaSessionService | 后台播放 + 通知栏 |
| 图片加载 | Coil | 封面加载 + 高斯模糊 |
| 导航 | Navigation Compose | 页面路由 |
| 异步 | Coroutines + Flow | 响应式数据流 |
| 调色板 | Palette | 从封面提取主色（动态背景） |

**minSdk**：26（Android 8.0，覆盖 ~98% 设备）
**targetSdk**：最新稳定版（撰写时为 35）
**测试设备**：真机（USB/WiFi 调试）

---

## 3. 功能范围

### 3.1 MVP 必做功能

| ID | 功能 | 描述 | 核心技术点 |
|---|---|---|---|
| F1 | 本地音乐扫描与列表 | 扫描设备音乐，列表展示 | MediaStore + Room 缓存 + 增量刷新 |
| F2 | 播放器全屏页 | 封面 + 进度 + 控件 + 歌词 | Compose 动画 + Media3 |
| F3 | 底部迷你播放条 | 全局悬浮控制 | Scaffold + 共享元素过渡 |
| F4 | 后台播放 + 通知栏 | 切后台/锁屏继续放 | MediaSessionService |
| F5 | 自建歌单 | 创建/编辑/删除歌单 | Room CRUD + 关联表 |
| F6 | 我喜欢 | 收藏歌曲 | songs.isFavorite 字段 |
| F7 | 本地搜索 | 按歌名/歌手/专辑过滤 | Flow + 实时过滤 |
| F8 | 逐行联动歌词 | LRC 时间戳 + 高亮滚动 | LRC 解析 + Compose 滚动 |
| F9 | 播放队列管理 | 顺序/随机/单曲循环 | Media3 队列 API |

### 3.2 数据来源策略
- **主**：MediaStore 扫描设备本地音乐（mp3/m4a/flac/ogg/wav）
- **兜底**：首次启动无音乐时，显示 assets 内置的 2-3 首免版权示例曲，避免空白页
- **歌词**：读取与音乐文件**同名**的 `.lrc` 文件（如 `晴天.mp3` + `晴天.lrc`）；无则显示"暂无歌词"

---

## 4. 信息架构与导航

### 4.1 三层结构（仿 QQ 音乐）

```
L3 浮层     播放器全屏页（点击底部播放条展开）
L2 全局层   底部播放条（所有 Tab 页共享）
L1 主框架   底部导航 + 4 个 Tab
```

### 4.2 Tab 页

| Tab | 名称 | 职责 |
|---|---|---|
| 1 | 我的音乐 | 全部歌曲列表 + 我喜欢快捷入口 + 最近播放 |
| 2 | 搜索 | 关键词搜索 + 搜索历史 |
| 3 | 歌单 | 歌单网格（封面拼图）+ 新建 |
| 4 | 发现 | 本地推荐位 + 播放次数排行榜（本地数据驱动，非在线） |

### 4.3 二级页面
- **歌单详情页**：封面 + 标题 + 歌曲列表，可播放整个歌单
- **播放队列页**：当前播放队列，拖拽排序，移除歌曲

### 4.4 关键交互
- 点击底部播放条 → 展开播放页（共享元素动画）
- 播放页**下滑手势**关闭
- 切 Tab 时底部播放条**常驻**
- 播放页与底部条**共享封面**（过渡连贯）

### 4.5 导航实现
- **Navigation Compose** 管理路由
- **Scaffold** 承载 bottomBar（Tab）+ 嵌套 NavHost
- 底部播放条用 Column 叠加在 Scaffold 之上
- 播放器全屏页用全屏 Dialog（带下滑关闭手势）

---

## 5. 技术架构

### 5.1 分层架构（Clean Architecture + MVVM）

依赖单向向下：

```
📱 UI 层 (app/ui)
   Composable 屏幕 ← 观察 → ViewModel 的 StateFlow
   - 各页面 (MyMusicScreen, PlayerScreen, ...)
   - 主题/组件 (theme/, components/)

🧠 Domain 层 (app/domain)  [纯 Kotlin，无 Android 依赖]
   - UseCase（业务逻辑封装）
   - Repository 接口（契约）
   - 领域模型 (Song, Playlist, PlaybackState)

💾 Data 层 (app/data)
   - RepositoryImpl（实现 Domain 定义的接口）
   - Room DAO + Entity（本地数据库）
   - MediaStoreSource（设备音乐扫描）
   - AssetsSource（内置示例音乐）

🎵 Media 层 (app/media)
   - PlaybackService (MediaSessionService)
   - MediaController 连接管理
   - 播放状态 → StateFlow

   ▲
   Hilt 在所有层之间做依赖注入
```

### 5.2 核心数据流（以「点击歌曲播放」为例）

```
SongItem Composable
   │ 1. onClick(song)
   ▼
MyMusicViewModel
   │ 2. playSongUseCase(song)
   ▼
PlaySongUseCase
   │ 3. 调用 mediaRepository.play(song)
   ▼
MediaRepositoryImpl
   │ 4. mediaController.setMediaItem + prepare + play
   ▼
PlaybackService (后台 Media3)
   │ 5. ExoPlayer 真实播放
   │ 6. 状态变化 → StateFlow<PlaybackUiState>
   ▼
所有 Composable 自动重组（播放条/播放页同步）
```

### 5.3 包结构

```
com.melody.app/
├── ui/                        # Compose 屏幕与组件
│   ├── theme/                 # Color.kt, Type.kt, Theme.kt
│   ├── components/            # 复用组件 (MiniPlayer, SongRow...)
│   ├── mymusic/               # 我的音乐页 + ViewModel
│   ├── player/                # 播放器全屏页 + ViewModel
│   ├── search/                # 搜索页 + ViewModel
│   ├── playlist/              # 歌单页 + ViewModel
│   └── discover/              # 发现页 + ViewModel
├── domain/                    # 纯 Kotlin，无 Android 依赖
│   ├── model/                 # Song, Playlist, PlaybackState...
│   ├── repository/            # 接口契约
│   └── usecase/               # PlaySongUseCase, ScanMusicUseCase...
├── data/                      # 数据访问实现
│   ├── local/                 # Room: Entity, DAO, Database
│   ├── source/                # MediaStoreSource, AssetsSource
│   └── repository/            # RepositoryImpl
├── media/                     # Media3 播放服务
│   ├── PlaybackService.kt
│   ├── MediaServiceConnection.kt
│   └── LyricsParser.kt        # LRC 解析器
├── di/                        # Hilt 模块
│   ├── DatabaseModule.kt
│   ├── MediaModule.kt
│   └── RepositoryModule.kt
└── MelodyApp.kt               # Application 入口
```

### 5.4 关键架构决策

1. **后台播放**：Media3 `MediaSessionService`，系统自动生成媒体通知 + 锁屏控件 + 蓝牙/耳机键支持
2. **音乐数据缓存**：Room 缓存扫描结果 + 增量刷新（首次扫描存库，后续启动先显示缓存，后台增量扫描新增/删除）
3. **UI 与服务通信**：`MediaController` + `StateFlow<PlaybackUiState>`，与 Compose 天然契合
4. **UseCase 策略**：业务逻辑复杂的功能（扫描、播放、歌词同步）用 UseCase 封装；简单 CRUD 可让 ViewModel 直接调 Repository

---

## 6. 数据模型（Room Schema）

### 6.1 实体关系

```
songs ──< playlist_song >── playlists
  │
  └──< play_history

search_history (独立)
```

### 6.2 表结构

#### `songs`（歌曲主表）
| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long PK | 自增主键 |
| `mediaUri` | String | MediaStore 的 content:// URI（播放用） |
| `title` | String | 歌曲名 |
| `artist` | String | 歌手 |
| `album` | String | 专辑 |
| `duration` | Long | 时长（毫秒） |
| `coverUri` | String? | 封面 URI（可能为空） |
| `filePath` | String | 文件绝对路径（找同名 .lrc 用） |
| `lrcUri` | String? | 对应歌词文件路径 |
| `size` | Long | 文件大小（去重用） |
| `isFavorite` | Boolean | 是否收藏 |
| `playCount` | Int | 播放次数（驱动"排行榜"） |
| `addedAt` | Long | 扫描入库时间（排序） |

#### `playlists`（歌单表）
| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long PK | 自增主键 |
| `name` | String | 歌单名 |
| `type` | Int | 0=普通歌单，1=固定"我喜欢"（系统创建，不可删） |
| `createdAt` | Long | 创建时间 |

#### `playlist_song`（歌单-歌曲 关联表）
| 字段 | 类型 | 说明 |
|---|---|---|
| `playlist_id` | Long FK | 关联 playlists.id |
| `song_id` | Long FK | 关联 songs.id |
| `addedAt` | Long | 加入时间（用于歌单内排序） |
| **复合主键** | (playlist_id, song_id) | 防止同一首歌重复加入 |

#### `play_history`（播放历史）
| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long PK | 自增主键 |
| `song_id` | Long FK | 关联 songs.id |
| `playedAt` | Long | 播放时间戳 |

> 只保留最近 100 条，超出自动清理。

#### `search_history`（搜索历史）
| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long PK | 自增主键 |
| `query` | String | 搜索关键词 |
| `searchedAt` | Long | 搜索时间戳 |

> 只保留最近 20 条，去重（同关键词更新时间）。

### 6.3 设计决策
1. **`isFavorite` 内嵌在 songs 表**（而非用 playlist type=1 关联）——收藏是高频读写，内嵌避免 JOIN，"我喜欢"页通过 `WHERE isFavorite = 1` 查询。
2. **`playlist_song` 用复合主键**——天然防止重复加入，符合关联表范式。
3. **MediaStore 同步策略**：按 `mediaUri` 做差异对比（新增/删除/更新元数据），但**保留用户数据**（isFavorite、playCount 不被覆盖）。
4. **lrcUri 扫描时一并解析**：检查同目录下是否存在同名 `.lrc`。

### 6.4 领域模型 vs Room Entity
- Domain 层 `Song` 是纯 Kotlin data class（可纯 JVM 单元测试）
- Data 层 `SongEntity` 是带 `@Entity` 注解的 Room 表
- 通过 mapper 函数 `SongEntity.toDomain()` / `Song.toEntity()` 转换

---

## 7. UI 设计规范

### 7.1 播放器全屏页（核心视觉）

**布局**（从上到下）：
1. 顶部栏：⌄（下拉关闭）· 歌曲名 · ⋯（更多菜单）
2. 封面区：200x200 圆角卡片（20px 圆角 + 柔和投影），播放时缓慢旋转
3. 歌曲信息：标题 + 歌手/专辑 + ❤️（收藏）
4. 进度条：可拖拽，带时间显示
5. 控制按钮：🔀（模式）· ⏮ · ▶/⏸（大圆按钮）· ⏭ · 📜（队列）
6. 底部歌词预览：三行（上/当前/下），当前行高亮

**沉浸式背景**：整页背景 = 当前歌曲封面**高斯模糊 + 暗化**。配合 Palette API 提取封面主色调，每首歌视觉基调不同。

**交互**：
- 点击封面/歌词区 → 切换"封面视图/歌词视图"
- 歌词视图：完整歌词自动滚动，当前行高亮+放大，点击任意行跳转播放
- 顶部下滑 → 缩放回退到底部播放条（共享元素过渡）

### 7.2 配色方案（深色为主）

| 用途 | 颜色 | Hex |
|---|---|---|
| 背景最深 | 深紫黑 | `#0F0F1E` |
| 背景 | 深紫蓝 | `#1A1A2E` |
| 卡片背景 | 深蓝 | `#16213E` |
| 强调色（品牌） | 青绿 | `#31C27C` |
| 主文字 | 白 | `#FFFFFF` |
| 次要文字 | 灰蓝 | `#94A3B8` |

> 播放页背景色**动态**：Palette API 从当前封面提取主色，每首歌不同。

### 7.3 其他页面风格
- **歌曲列表**：圆角封面缩略图 + 标题/歌手 + 右侧 ⋯ 菜单，长按多选
- **底部播放条**：毛玻璃质感（blur），左侧封面+信息，右侧控件，点击展开
- **歌单网格**：2 列卡片，封面 = 前 3 首歌封面九宫格拼图

---

## 8. 错误处理与边界场景

### 8.1 权限相关
| 场景 | 处理 |
|---|---|
| 用户拒绝读取权限 | 显示引导页 + "去设置开启权限"按钮，不强制 |
| Android 13+ 细粒度音频权限 | 申请 `READ_MEDIA_AUDIO`，旧版本降级到 `READ_EXTERNAL_STORAGE` |
| 后台播放需通知权限（Android 13+） | 申请 `POST_NOTIFICATIONS`，被拒则后台播放受限但仍可在前台播放 |

### 8.2 数据相关
| 场景 | 处理 |
|---|---|
| 扫描到 0 首歌 | 显示内置 assets 示例音乐 + 提示"未找到本地音乐，点击重新扫描" |
| 音乐文件被删除（URI 失效） | 播放时捕获异常 → 从 Room 删除该记录 + Toast 提示 |
| 同名 .lrc 不存在 | 播放页歌词区显示"暂无歌词"占位 |
| LRC 格式损坏 | 解析失败时降级为纯文本显示（忽略时间戳） |
| 封面加载失败 | 显示默认占位封面（音符图标 + 渐变背景） |

### 8.3 播放相关
| 场景 | 处理 |
|---|---|
| 音频焦点丢失（来电/其他 APP 播放） | 自动暂停，焦点恢复后可选择继续 |
| 耳机断开 | 自动暂停（AudioManager.ACTION_AUDIO_BECOMING_NOISY） |
| 播放到最后一首 | 根据播放模式：顺序→停止/循环→回到第一首/随机→随机下一首 |
| 队列为空时点击播放 | 无操作或提示"请先添加歌曲到队列" |

### 8.4 性能
| 场景 | 处理 |
|---|---|
| 歌曲数量大（1000+） | 列表用 LazyColumn + key 复用；扫描在 IO 调度器；Room 分页查询 |
| 封面加载占内存 | Coil 自动缓存 + 降采样；列表用小尺寸，播放页用大尺寸 |
| 高斯模糊耗时 | 用 Coil 的 `blur()` 扩展，在 IO 线程处理并缓存 |

---

## 9. 测试策略

### 9.1 单元测试（Domain 层，纯 JVM）
- **LyricsParser**：各种 LRC 格式（标准/多时间戳/空文件/损坏）
- **UseCase**：PlaySongUseCase、ScanMusicUseCase 等业务逻辑（mock Repository）
- **Mapper**：SongEntity ↔ Song 转换
- **工具类**：时间格式化、文件路径处理

### 9.2 集成测试（Data 层）
- **Room DAO**：用 in-memory database 测试 CRUD、关联表查询、增量刷新逻辑
- **MediaStoreSource**：mock ContentResolver 测试扫描逻辑

### 9.3 UI 测试（instrumented）
- 关键交互：点击歌曲→播放、底部播放条展开/关闭、歌词点击跳转
- 用 Compose UI Test 框架

### 9.4 手动测试清单（真机）
- [ ] 首次启动权限申请流程
- [ ] 扫描音乐→列表显示
- [ ] 播放/暂停/上一首/下一首
- [ ] 后台播放 + 通知栏控制
- [ ] 锁屏控制
- [ ] 耳机插拔自动暂停
- [ ] 歌单创建/添加/删除歌曲
- [ ] 收藏/取消收藏
- [ ] 搜索功能
- [ ] 歌词联动滚动
- [ ] 播放模式切换（顺序/随机/单曲）

---

## 10. 开发环境

| 组件 | 状态 |
|---|---|
| Android Studio | ✅ 已装（`C:\Program Files\Android\Android Studio`） |
| Android SDK | ✅ 已装（`C:\Users\m\AppData\Local\Android\Sdk`） |
| JDK 17 (Temurin) | ✅ 已装，`JAVA_HOME` 已配 |
| adb / sdkmanager | ⚠️ 不在 PATH（Android Studio 内部可用，需命令行时再配） |

**待办**：首次需要命令行操作时，将 `platform-tools` 和 `cmdline-tools` 加入 PATH。

---

## 11. 待定事项（Implementation 阶段决定）

以下细节在实施阶段决定，不影响当前设计：
- 具体依赖版本号（用最新稳定版）
- 示例音乐/歌词的免版权素材来源（实施时从 freesound.org / Pixabay 获取）
- 应用图标设计
- ProGuard 规则（发布时再配）

---

**文档结束。请审阅后告知是否需要修改。**
