# XZD AI Chat

一个基于 Jetpack Compose 的 Android AI 聊天应用，支持多模型接入、Agent 模式（通过 Shell 控制手机）、多种聊天风格、语音输入、定时任务等功能。

## 功能特性

### 聊天模式
- 支持 OpenAI 兼容 API（DeepSeek、Moonshot、智谱、通义、Kimi 等）
- 多会话管理，每会话独立风格和上下文
- 12+ 种聊天风格预设（极简、终端、樱花、赛博朋克等）
- 思考过程展示（支持 reasoning 模型）
- Markdown 渲染、代码高亮
- 语音输入（需系统语音识别服务）
- 导出会话为 Markdown / 纯文本 / 长图
- 提示词模板管理
- 定时任务（定时发送消息）

### Agent 模式
- 通过自然语言或 Shell 命令控制 Android 设备
- 支持 Root（Magisk/KernelSU/su）和 Shizuku 两种提权方式
- 多步任务执行：AI 根据命令输出自动决定下一步
- 内置常用 Android Shell 命令知识库（am/pm/settings/input/dumpsys 等）
- 脚本模式：批量执行多条命令
- 命令历史记录

### 其他
- 云端 API / 本地部署模式切换（支持 Ollama、llama.cpp、vLLM）
- 全部数据本地存储，无云端同步
- Material You 设计，严格遵循 Material 3 规范
- 前缀缓存优化，降低 Token 消耗

## 截图

> 待补充

## 构建

### 环境要求
- JDK 17
- Android SDK (compileSdk 35, minSdk 26)
- Gradle 8.9+

### 步骤

```bash
# 克隆仓库
git clone https://github.com/xzd1314/XZD-AI-Chat.git
cd XZD-AI-Chat

# 配置 local.properties（指定 SDK 路径）
echo "sdk.dir=/path/to/android-sdk" > local.properties

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK（需要配置签名，见下文）
./gradlew assembleRelease
```

### 签名配置（可选）

在项目根目录创建 `keystore.properties`：

```properties
storeFile=your-keystore.jks
storePassword=your-store-password
keyAlias=your-key-alias
keyPassword=your-key-password
```

将 `.jks` 文件放在项目根目录，然后执行 `./gradlew assembleRelease`。

## 配置

### API 配置

首次打开应用后，进入「设置」页面：
1. 选择 API 供应商（或自定义）
2. 填入 API Key
3. 选择模型
4. （可选）自定义 Base URL

### Shizuku 配置（Agent 模式）

1. 下载安装 [Shizuku](https://github.com/RikkaApps/Shizuku)
2. 通过 ADB 或 Root 启动 Shizuku 服务
3. 在 Agent 会话中点击「授权」，允许应用使用 Shizuku
4. 授权后即可通过 Shizuku 执行 Shell 命令

### 语音输入

应用使用 Android 系统自带的语音识别服务。如果提示不支持：
1. 安装 Google 语音搜索或讯飞语音等语音引擎
2. 在系统设置中设为默认语音输入服务
3. 授予麦克风权限

## 技术栈

- **语言**：Kotlin
- **UI**：Jetpack Compose + Material 3
- **架构**：MVVM（ViewModel + StateFlow）
- **网络**：OkHttp + 自定义 LLM 客户端（OpenAI 兼容协议）
- **存储**：SharedPreferences + JSON
- **Agent**：Shizuku API + Root Shell
- **后台任务**：WorkManager
- **最低版本**：Android 8.0 (API 26)
- **目标版本**：Android 15 (API 35)

## 项目结构

```
app/src/main/java/com/xzd1314/aichat/
├── agent/              # Agent 模式：Shell 执行器
├── data/               # 数据模型、存储、预设
├── net/                # 网络请求、LLM 客户端
├── security/           # 安全相关
├── ui/
│   ├── components/     # 通用 UI 组件
│   ├── theme/          # 主题、颜色、聊天风格
│   └── screens/        # 各页面（聊天、设置、Agent、关于等）
├── util/               # 工具类
├── AppViewModel.kt     # 全局 ViewModel
└── MainActivity.kt     # 入口 Activity
```

## 贡献

欢迎提交 Issue 和 Pull Request！

## License

[MIT](LICENSE)
