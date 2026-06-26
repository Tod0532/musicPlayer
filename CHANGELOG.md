# 更新日志

本项目所有重要变更记录于此。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/)。

---

## [1.0.0] - 2026-06

### ✨ 新增

#### 核心播放
- **F1** MediaStore 本地音乐扫描（Android 13+ 细粒度音频权限）
- **F2** 播放器全屏页（沉浸式封面背景 + 旋转动画 + 进度条 + 控件）
- **F3** 底部迷你播放条（全局悬浮控制，点击展开播放页）
- **F4** 后台播放 + 通知栏控制（Media3 MediaSessionService）
- **F9** 播放模式（顺序 / 随机 / 单曲循环）

#### 数据管理
- **F5** 自建歌单（Room CRUD，列表/详情双模式）
- **F6** 我喜欢（收藏 toggle）

#### 搜索
- **F7** 在线搜索（SoundHelix CC 协议音乐 + MusicBrainz 元数据）
- **F7** 本地搜索过滤（我的音乐页实时筛选）

#### 歌词
- **F8** 歌词联动（LRC 解析 + 封面/歌词双视图 + 自动滚动 + 点击跳转）

### 🎨 优化
- 播放控制健壮性（Service 未连接时待播放队列）
- 进度条可拖拽（实时预览时间，松手 seek）
- UI 细节（状态栏 inset / 封面多层渐变 / 播放条投影 / 播放页光环）

### 🏗️ 架构
- Clean Architecture 分层（UI / Domain / Data / Media）
- MVVM + 单一 AndroidViewModel 状态管理
- 响应式数据流（Flow + StateFlow）

---

## 技术栈版本

| 组件 | 版本 |
|---|---|
| Kotlin | 2.0.21 |
| Compose BOM | 2024.12.01 |
| Media3 | 1.4.1 |
| Room | 2.6.1 |
| Gradle | 8.10.2 |
| AGP | 8.7.3 |

---

## Git 提交历史

```
4d8c16a docs: 添加项目 README
6f36e3c feat: 体验优化（播放健壮性/本地搜索/歌单详情/进度条拖拽）
c62db9f feat: F4 后台播放 / F8 歌词联动 / F5 自建歌单
c37aea1 feat: MediaStore 扫描 + 在线搜索 + Media3 流式播放
67eb287 feat: UI 优化（状态栏/封面渐变/播放条投影）
bf68bf1 feat: Melody UI 骨架（首个可运行 APK）
2a10c52 docs: 设计文档 (spec)
```
