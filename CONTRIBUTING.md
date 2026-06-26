# 贡献指南

感谢你对 Melody 项目的兴趣！欢迎贡献代码、报告问题或建议功能。

## 🚀 快速开始

### 环境要求
- JDK 17
- Android SDK（build-tools 36.1.0、platform android-34）
- Android Studio（推荐最新版）

### 构建步骤

```bash
# 1. 克隆仓库
git clone https://github.com/Tod0532/musicPlayer.git
cd musicPlayer

# 2. 构建 debug APK
./gradlew assembleDebug

# 3. 安装到真机
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> ⚠️ 中国大陆网络环境：项目已配置腾讯云 Gradle 镜像和阿里云 Maven 镜像，无需额外配置。

## 📋 贡献流程

1. **Fork** 仓库
2. 创建特性分支：`git checkout -b feat/your-feature`
3. 提交改动：`git commit -m 'feat: 添加 xxx 功能'`
4. 推送分支：`git push origin feat/your-feature`
5. 提交 **Pull Request**

## 📝 提交规范

使用 [Conventional Commits](https://www.conventionalcommits.org/) 格式：

| 前缀 | 用途 | 示例 |
|---|---|---|
| `feat` | 新功能 | `feat: 添加搜索历史` |
| `fix` | Bug 修复 | `fix: 修复播放崩溃` |
| `docs` | 文档更新 | `docs: 更新 README` |
| `style` | 代码格式 | `style: 统一缩进` |
| `refactor` | 重构 | `refactor: 抽取播放控制` |
| `test` | 测试 | `test: 添加歌词解析测试` |
| `chore` | 构建/工具 | `chore: 升级依赖版本` |

## 🏗️ 架构约定

遵循项目的 Clean Architecture 分层：

- **UI 层** (`app/ui`): Composable 屏幕 + ViewModel
- **Domain 层** (`app/domain`): 纯 Kotlin 模型，无 Android 依赖
- **Data 层** (`app/data`): Room / 在线音源 / MediaStore
- **Media 层** (`app/media`): Media3 播放服务

**原则**：
- 依赖单向向下，无循环
- Domain 层保持纯 Kotlin
- 新功能先写测试再实现（TDD）

## 🎨 UI 规范

- 深色主题为主（仿 QQ 音乐风格）
- 配色见 `app/ui/theme/Color.kt`
- 使用 Material 3 组件
- 避免硬编码颜色，用 `MaterialTheme.colorScheme`

## 🐛 报告问题

提交 Issue 时请使用模板，包含：
- 问题描述 + 复现步骤
- 手机型号 + Android 版本
- 日志输出（如有）

## 📄 许可证

贡献的代码将遵循 [MIT 许可证](LICENSE)。
