# Melody · AI 资讯播报功能 设计文档（Design Spec）

> **项目**：Melody 音乐播放器新增模块
> **功能**：AI 新闻聚合 + TTS 中文播报
> **创建日期**：2026-06-26
> **状态**：待用户审阅

---

## 1. 项目概述

### 1.1 一句话定位
在 Melody 播放器里新增"资讯"Tab，聚合最新 AI 资讯（HackerNews + GitHub + RSSHub），自动生成摘要，用 Android 内置 TTS 连续播报，像听新闻电台一样听 AI 新闻。

### 1.2 已确认的技术决策

| 决策 | 选择 | 理由 |
|---|---|---|
| 集成方式 | Melody APP 内新增"资讯"Tab | 复用现有架构，不单独建项目 |
| 数据源 | HackerNews API + GitHub Trending + RSSHub 公共节点 | 三层降级，保证可用性 |
| 语言处理 | 不翻译，直接用原文 | 抓中文源/英文源各自原文，零成本零延迟 |
| TTS 引擎 | Android 内置 TextToSpeech | 离线、免费、稳定 |
| 播报方式 | 播放列表式连续播报，读标题+摘要 | 复用音乐播放架构，体验连贯 |
| 摘要方式 | 截取式（标题+前2-3句） | 无需 LLM API，零成本 |

### 1.3 非目标（明确不做）
- ❌ X/Twitter 大V动态（API 收费 + 抓取违反 ToS）
- ❌ AI 大模型翻译（抓中文源免翻译，英文源保持原文）
- ❌ 云端 TTS（用系统内置 TTS）
- ❌ LLM 摘要（用截取式摘要，不调大模型）

---

## 2. 数据源抓取架构

### 2.1 三层数据源

```
APP 启动 / 下拉刷新
        ↓
   并发抓取三个源（Dispatchers.IO）
        ↓
┌─────────────────┬──────────────────┬───────────────────┐
│ HackerNews API  │ GitHub Trending  │ RSSHub 公共节点    │
│ (直连，最可靠)   │ (直连，很可靠)    │ (不稳定，降级)     │
└────────┬────────┴────────┬─────────┴─────────┬─────────┘
         ↓                  ↓                   ↓
         合并 → 去重 → 按时间倒序排序
                   ↓
            List<NewsItem>
                   ↓
         每条生成摘要（标题 + 前2-3句）
```

### 2.2 NewsItem 数据模型

```kotlin
data class NewsItem(
    val id: String,           // 唯一标识（用于去重）
    val title: String,        // 标题
    val summary: String,      // 摘要（标题+前几句，TTS 朗读的内容）
    val source: String,       // 来源（"HackerNews"/"GitHub"/"机器之心"）
    val url: String,          // 原文链接（点击跳转）
    val publishedAt: Long,    // 发布时间（排序用）
    val score: Int = 0        // 热度（GitHub stars / HN points，可选排序）
)
```

### 2.3 各源实现

**HackerNews**（最可靠，主力）
- 官方 API：`https://hacker-news.firebaseio.com/v0/topstories.json` 取前 100 条 ID
- 逐条取详情：`/v0/item/{id}.json`（title, url, score, time）
- 关键词过滤：标题含 AI 关键词（见 §2.5）
- 约从 100 条筛出 10-20 条 AI 相关

**GitHub Trending**（很可靠，补充）
- 抓取 `https://github.com/trending?since=daily` 页面 HTML
- 用 Jsoup 解析仓库名、描述、stars、语言
- 过滤：描述/话题含 AI 关键词，或语言为 Python
- 转为 NewsItem：标题格式 `[新项目] repo名 - 描述`

**RSSHub**（不稳定，降级源）
- 请求 `https://rsshub.app/jiqizhixin`（机器之心）、`/qbitai`（量子位）
- 用 Android 内置 XmlPullParser 解析 RSS XML
- 请求失败/超时（3秒）→ 静默跳过，不影响其他源

### 2.4 去重策略
- 按标题相似度去重（相同标题只保留热度最高的）
- 跨源去重（HackerNews 和 GitHub 可能转推同一新闻）

### 2.5 AI 关键词过滤

```kotlin
val AI_KEYWORDS = listOf(
    "ai", "ml", "gpt", "llm", "transformer", "diffusion",
    "claude", "gemini", "rag", "agent", "neural", "deep learning",
    "machine learning", "openai", "anthropic", "huggingface",
    "stable diffusion", "midjourney", "copilot", "prompt"
)
```

### 2.6 错误处理

| 场景 | 处理 |
|---|---|
| HackerNews 失败 | 显示 GitHub + RSSHub 内容 |
| 全部失败 | 显示"暂无资讯，下拉重试" |
| RSSHub 超时 | 静默跳过（3秒超时） |
| 单条解析失败 | 跳过该条，不影响其他 |

---

## 3. TTS 播报架构（核心）

### 3.1 核心问题

Melody 现有播放器是 **ExoPlayer（音频文件）**，新闻播报是 **TextToSpeech（文字→语音）**。两者 API 不同，但用户需同一套控制。

### 3.2 统一播放接口

```
              PlayController（概念接口）
              ├── play()  pause()  next()  previous()
              ├── 状态: isPlaying / currentIndex / 总数
              │
    ┌─────────┴──────────┐
    ↓                    ↓
MusicController      NewsPlayerController
(现有 MediaService     (Android TextToSpeech)
 Connection)          把 NewsItem 文字读出来
```

### 3.3 NewsPlayerController 核心

```kotlin
class NewsPlayerController(context: Context) {
    private var tts: TextToSpeech?
    private var queue: List<NewsItem> = emptyList()
    private var currentIndex = 0

    var onStateChanged: ((isPlaying: Boolean, index: Int, total: Int) -> Unit)?
    var onCompleted: (() -> Unit)?

    fun startPlayback(items: List<NewsItem>, startIndex: Int = 0)
    fun play()       // 恢复朗读
    fun pause()      // 暂停（tts.stop + 记录）
    fun next()       // 跳到下一条
    fun previous()   // 上一条
    fun stop()       // 停止并释放
}
```

### 3.4 朗读内容格式

每条新闻朗读格式：
```
第 {当前条数} 条，共 {总数} 条。来自 {来源}。
{标题}。
{摘要}。
```
例：*第 3 条，共 12 条。来自 HackerNews。OpenAI 发布 GPT-5。OpenAI 今日发布了新一代大模型 GPT-5...*

### 3.5 关键技术点

**TTS 引擎**
- `TextToSpeech(context, initListener)`，设置 `Locale.CHINESE`
- vivo 系统自带中文 TTS，无需用户配置
- 系统无中文 TTS 时提示安装（罕见）

**与 ExoPlayer 互斥**
- 新闻播报开始 → 暂停音乐播放
- 停止播报 → 切回音乐模式（不自动恢复，让用户主动点）
- 底部播放条根据模式切换显示（音乐/新闻）

**通知栏集成**
- 新闻播报用**自定义前台通知**（带暂停/下一条按钮）
- 不走 MediaSession（TTS 不经过 ExoPlayer）
- 点击通知回到 APP

**后台播报保活**
- 复用 PlaybackService 前台服务保活逻辑
- 切后台继续朗读（TextToSpeech 本身支持后台）

**朗读进度监听**
- `UtteranceProgressListener.onDone` → 自动播报下一条
- `UtteranceProgressListener.onStart/onError` → 状态更新

### 3.6 局限性

| 局限 | 说明 |
|---|---|
| 通知栏控件不统一 | 音乐走 MediaSession（系统样式），新闻走自定义通知 |
| 无锁屏大封面 | 新闻通知只有文字+按钮 |
| TTS 音色取决于系统 | 机械感由系统 TTS 引擎决定 |
| 朗读到一半切走 | 切回音乐 Tab 会中断当前新闻朗读 |

---

## 4. UI/交互设计

### 4.1 新增"资讯"Tab

底部导航加第 5 个 Tab："资讯"（📰）。

```
底部导航：[我的] [搜索] [歌单] [发现] [资讯]
```

### 4.2 资讯列表页（NewsScreen）

```
┌─────────────────────────────────┐
│  📰 AI 资讯          ⟳ 刷新      │
│  更新于 10:30 · 12 条            │
├─────────────────────────────────┤
│  ┌─────────────────────────┐    │
│  │ ▶ 开始播报全部            │    │  ← 大按钮
│  └─────────────────────────┘    │
├─────────────────────────────────┤
│  新闻卡片列表                     │
│  ┌─────────────────────────┐    │
│  │ [来源标签] 标题           ▶  │  ← 单条播放
│  │ 摘要文字...                 │
│  │ 热度 · 时间                  │
│  └─────────────────────────┘    │
└─────────────────────────────────┘
```

- 标题 + 刷新按钮 + 更新时间/条数
- "开始播报全部"大按钮
- 新闻卡片：来源标签（彩色）、标题、摘要、热度/时间、单条播放按钮
- 点击卡片 → 系统浏览器打开原文

### 4.3 播报详情页（NewsPlayerScreen，类似音乐播放页）

```
┌─────────────────────────────────┐
│  ⌄                              │
│         📰 正在播报              │
│       第 3 条/共 12 条           │
├─────────────────────────────────┤
│       OpenAI 发布 GPT-5          │  ← 当前标题（大字）
│       （朗读进度指示）            │
├─────────────────────────────────┤
│   🔁  ⏮    ⏸    ⏭    📋         │  ← 控制按钮
├─────────────────────────────────┤
│   "OpenAI 今日发布了..."         │  ← 朗读文字
└─────────────────────────────────┘
```

### 4.4 底部播放条双模式

| 模式 | 显示 | 图标 |
|---|---|---|
| 音乐模式 | 🎵 歌名/歌手 | 音乐封面 |
| 新闻模式 | 📰 新闻标题/来源 | 📰 |

### 4.5 配色

复用 Melody 深色主题。来源标签色：
- HackerNews：橙色 `#FF6600`
- GitHub：绿色 `#34D399`
- 机器之心/量子位：青色 `#31C27C`

---

## 5. 技术实现

### 5.1 新增依赖

```toml
jsoup = "1.18.1"    # GitHub Trending HTML 解析
```

### 5.2 新增包结构

```
com.melody.app/
├── data/
│   └── news/                          ← 新增
│       ├── NewsItem.kt                # 新闻数据模型
│       ├── NewsRepository.kt          # 聚合三源 + 去重排序
│       ├── HackerNewsSource.kt        # HN API 抓取
│       ├── GitHubTrendingSource.kt    # GitHub HTML 解析
│       └── RSSHubSource.kt            # RSSHub RSS 解析（降级）
├── media/
│   └── NewsPlayerController.kt        ← 新增：TTS 播报控制器
└── ui/
    └── news/                          ← 新增
        ├── NewsScreen.kt              # 资讯列表页
        └── NewsPlayerScreen.kt        # 播报详情页
```

### 5.3 PlayerViewModel 扩展

新增状态字段：
```kotlin
data class PlayerUiState(
    // ... 现有字段 ...
    val newsItems: List<NewsItem> = emptyList(),     // 新闻列表
    val isNewsMode: Boolean = false,                  // 是否新闻播报模式
    val newsIndex: Int = 0,                           // 当前播报索引
    val isFetchingNews: Boolean = false               // 是否正在抓取
)
```

新增方法：
- `fetchNews()` — 并发抓取三源
- `startNewsPlayback(index)` — 开始播报（切新闻模式，暂停音乐）
- `stopNewsPlayback()` — 停止播报（切回音乐模式）
- `newsNext()` / `newsPrevious()` / `newsPause()` / `newsPlay()` — 播报控制

### 5.4 音乐/新闻互斥

```kotlin
fun startNewsPlayback(index: Int) {
    mediaConnection.pause()           // 暂停音乐
    _uiState.value = state.copy(isNewsMode = true, newsIndex = index)
    newsPlayer.startPlayback(newsItems, index)
}
```

### 5.5 后台播报保活

新闻播报时用自定义前台通知（非 MediaSession）：
- `startForeground()` + 自定义 RemoteViews
- 通知含：标题 + 暂停/下一条按钮
- 点击通知回到 APP

---

## 6. 边界场景

| 场景 | 处理 |
|---|---|
| 无网络 | 显示"网络不可用，下拉重试" + 本地缓存的上次内容 |
| 三源全空 | 显示"暂无资讯" |
| 系统无中文 TTS | 提示"请安装中文语音引擎" + 提供跳转系统 TTS 设置 |
| 播报中切到音乐 | 停止新闻播报，切回音乐模式 |
| 来电 | TTS 自动暂停（音频焦点） |
| APP 被系统杀死 | 前台服务保活，尽量不被杀 |

---

## 7. 测试策略

### 7.1 单元测试
- AI 关键词过滤逻辑
- 去重算法
- 摘要截取（前 2-3 句）
- RSS XML 解析

### 7.2 手动测试清单
- [ ] 资讯 Tab 进入，显示新闻列表
- [ ] 下拉刷新，重新抓取
- [ ] 点"开始播报全部"，连续朗读
- [ ] 单条播放按钮，只读一条
- [ ] 上/下一条切换
- [ ] 暂停/恢复
- [ ] 切后台继续播报
- [ ] 通知栏控制（暂停/下一条）
- [ ] 播报中切到音乐 Tab，新闻停止
- [ ] 点击新闻卡片打开原文

---

**文档结束。请审阅后告知是否需要修改。**
