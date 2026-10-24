# XZD AI Chat - Windows (WinUI 3)

Android 原版（Kotlin + Jetpack Compose）的 Windows 移植版，使用 **WinUI 3 + .NET 8**。

## 已实现功能

- OpenAI 兼容 API（DeepSeek / 智谱 / Kimi / 通义 / 豆包 / 文心 / 星火 / 混元 / 百川 / 零一 / MiniMax / 阶跃 / Ollama / 自定义）
- **流式输出**（SSE），边生成边显示
- 思考过程（reasoning_content）折叠展示
- 多会话管理：新建 / 删除 / 置顶
- 22 套聊天风格（微信 / iMessage / Telegram / 赛博朋克 / 极简 / Matrix 终端…）一键切换
- 内置 Markdown 渲染（代码块 / 粗体 / 标题 / 列表）
- 完整设置：供应商 / API Key / 模型 / Base URL / 温度 / 上下文窗口 / 最大 Token / 系统提示词
- 测试连接按钮
- 全部数据本地存放在 `%AppData%\XzdAiChat\`（JSON 文件）
- 前缀缓存优化：system 在最前、历史按时间序、只截尾部

## 暂未移植（Android 独有，Windows 版后续再加）

- Agent 模式（Shell 控机）→ Windows 计划改为 PowerShell 执行器
- 语音输入、震动反馈、Material You 动态取色
- 定时任务、提示词模板管理、会话导出

## 环境要求

- Windows 10 1809 (build 17763) 或更高，推荐 Windows 11
- [.NET 8 SDK](https://dotnet.microsoft.com/download/dotnet/8.0)
- 或者 Visual Studio 2022 17.9+，安装「.NET 桌面开发」+「Windows 应用 SDK C# 模板」工作负载

## 一键构建（出 exe）

在项目根目录（包含 `build.ps1` 的那一层）打开 PowerShell：

```powershell
# 首次运行可能需要允许脚本
Set-ExecutionPolicy -Scope Process Bypass
./build.ps1
```

构建完成后，exe 在：

```
publish\win-x64\XzdAiChat.exe
```

整个 `publish\win-x64\` 文件夹就是绿色版，可以整体拷贝走、双击 `XzdAiChat.exe` 直接运行（自包含，不需要装 .NET 运行时）。

## 用 Visual Studio 打开

1. 双击 `XzdAiChatWinUI.sln`
2. 顶部选 `Debug | x64`
3. F5 运行

## 首次使用

1. 点右上角齿轮 ⚙ 打开设置
2. 选供应商（比如 DeepSeek）
3. 填 API Key（到对应开放平台申请）
4. 模型名留空则用默认（deepseek-chat）
5. 点「测试连接」确认通畅
6. 关闭设置，开始聊天

本地模型（Ollama）：供应商选「Ollama 本地」，Base URL 默认 `http://localhost:11434/v1/chat/completions`，不需要 API Key。

## 项目结构

```
XzdAiChatWinUI/
├── XzdAiChatWinUI.sln
├── build.ps1                    # 一键出 exe
└── XzdAiChatWinUI/
    ├── XzdAiChatWinUI.csproj
    ├── App.xaml / App.xaml.cs
    ├── MainWindow.xaml / .cs    # 主窗口（会话列表 + 聊天 + 设置抽屉）
    ├── data/
    │   ├── Models.cs            # ChatMessage / Conversation / ChatStyle / AppSettings
    │   ├── ModelPresets.cs      # 14 家供应商
    │   ├── StylePresets.cs      # 22 套气泡风格
    │   └── Store.cs             # JSON 文件存储
    ├── net/LLMClient.cs         # HttpClient + SSE 流式
    ├── viewmodels/MainViewModel.cs
    └── ui/MarkdownRender.cs     # 极简 Markdown → RichTextBlock
```

## License

GPL-3.0（与上游 Android 版一致）
