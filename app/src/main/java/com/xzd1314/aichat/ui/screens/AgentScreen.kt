package com.xzd1314.aichat.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xzd1314.aichat.AppViewModel
import com.xzd1314.aichat.agent.ShellExecutor
import com.xzd1314.aichat.agent.ShellResult
import com.xzd1314.aichat.data.AgentMessage
import com.xzd1314.aichat.data.StylePresets
import com.xzd1314.aichat.net.LLMClient
import com.xzd1314.aichat.ui.components.ChatBubble
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val AGENT_SYSTEM_PROMPT = """你是一个 Android 手机控制 Agent，可以通过 shell 命令操控手机。

## 工作方式
1. 用户描述需求 → 你分析需要执行的命令 → 用 ```sh ... ``` 输出命令
2. 命令执行结果会返回给你，你可以根据结果继续执行下一步（多步任务）
3. 如果用户直接输入命令，直接执行即可

## 输出格式
- 先用一句话说明要做什么
- 然后用 ```sh ... ``` 包裹命令（每次只输出一条命令，多条用 && 或 ; 连接）
- 命令执行后，根据输出决定是否需要继续

## 常用命令速查
**应用管理**：pm list packages、pm dump <pkg>、am start -n <pkg>/<activity>、am force-stop <pkg>、pm uninstall -k --user 0 <pkg>、cmd package install-existing <pkg>
**系统设置**：settings put system <key> <value>、settings put global <key> <value>、settings put secure <key> <value>、svc wifi enable/disable、svc bluetooth enable/disable、svc data enable/disable
**设备控制**：input tap x y、input swipe x1 y1 x2 y2、input keyevent <code>、input text "xxx"、dumpsys display、dumpsys battery、dumpsys window | grep mCurrentFocus
**信息查询**：getprop、getprop ro.product.model、getprop ro.build.version.release、dumpsys meminfo、top -n 1、df -h、free
**截图录屏**：screencap -p /sdcard/screen.png、screenrecord /sdcard/video.mp4
**文件操作**：ls、cat、cp、mv、rm、find、tar、zip
**网络**：ip addr、netstat -tlnp、ping、curl、dumpsys connectivity
**媒体**：am start -a android.intent.action.VIEW -d "file:///sdcard/x.mp4" -t "video/*"、media dispatch

## KeyEvent 常用值
HOME=3、BACK=4、POWER=26、CAMERA=27、VOLUME_UP=24、VOLUME_DOWN=25、MENU=82、NOTIFICATION=83、SETTINGS=165、APP_SWITCH=187、LOCK=26、BRIGHTNESS_UP=221、BRIGHTNESS_DOWN=220

## 任务示例
- 打开飞行模式：settings put global airplane_mode_on 1 && am broadcast -a android.intent.action.AIRPLANE_MODE
- 截图：screencap -p /sdcard/screenshot_$(date +%s).png
- 查看当前前台应用：dumpsys window | grep -E 'mCurrentFocus|mFocusedApp'
- 清理后台：am kill-all
- 查看已安装用户应用：pm list packages -3
- 设置亮度：settings put system screen_brightness 128
- 安装 APK：pm install -r /sdcard/app.apk
- 查看电池：dumpsys battery

## 安全规则
- 危险操作（rm -rf /、格式化分区、恢复出厂、删除系统文件）必须先警告用户并确认
- 不确定命令效果时，先查信息再操作
- 优先使用非破坏性方式完成任务
- 命令中涉及用户数据时要谨慎"""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentChatScreen(
    vm: AppViewModel,
    conversationId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val agentConvs by vm.agentConvs.collectAsState()
    val conv = agentConvs.firstOrNull { it.id == conversationId }

    var input by remember { mutableStateOf("") }
    var scriptMode by remember { mutableStateOf(false) }
    var showCmdHistory by remember { mutableStateOf(false) }
    var showStyleMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showModelInfo by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var permInfo by remember { mutableStateOf<ShellExecutor.PermissionInfo?>(null) }

    val style = StylePresets.get(conv?.stylePresetId ?: "minimal")
    val agentName = conv?.title ?: "Agent"

    // 权限检测放后台线程，避免阻塞 UI
    LaunchedEffect(Unit) {
        permInfo = withContext(Dispatchers.IO) { ShellExecutor.getPermissionInfo(context) }
    }

    fun refreshPerm() {
        scope.launch {
            permInfo = withContext(Dispatchers.IO) { ShellExecutor.getPermissionInfo(context) }
        }
    }

    // Binder 就绪后自动刷新权限（MainActivity 已提前注册监听器）
    LaunchedEffect(Unit) {
        ShellExecutor.onBinderReady {
            refreshPerm()
        }
    }

    // 首次进入且无消息时，添加欢迎消息（等权限信息加载完）
    LaunchedEffect(conv, permInfo) {
        if (conv != null && conv.messages.isEmpty() && permInfo != null) {
            val info = permInfo!!
            val rootStr = when (info.rootManager) {
                "magisk" -> "Magisk"
                "kernelsu" -> "KernelSU"
                "su" -> "su"
                else -> null
            }
            val modeDesc = when (info.mode) {
                "root" -> "Root 权限${rootStr?.let { "（$it）" } ?: ""}"
                "shizuku" -> "Shizuku 权限"
                else -> "普通权限（功能受限，建议 Root 或 Shizuku）"
            }
            vm.addAgentMessage(conversationId, AgentMessage(
                role = "assistant",
                content = "你好！我是 $agentName，可以帮你通过 shell 命令控制手机。\n\n" +
                    "当前权限：$modeDesc\n" +
                    when {
                        info.shizukuConnecting -> "\n⏳ 正在连接 Shizuku 服务…"
                        info.shizukuInstalled && !info.shizukuAuthorized ->
                            "\n⚠️ 检测到 Shizuku 已安装但未授权，点击右上角「授权」按钮授权。"
                        else -> ""
                    } +
                    "\n\n你可以说：\"打开飞行模式\"、\"查看已安装应用\"、\"截图\"、\"设置屏幕亮度为50%\"，或者直接输入命令。"
            ))
        }
    }

    LaunchedEffect(conv?.messages?.size, sending) {
        if (!conv?.messages.isNullOrEmpty()) {
            listState.animateScrollToItem(conv!!.messages.size - 1)
        }
    }

    fun send() {
        val text = input.trim()
        if (text.isEmpty() || sending || conv == null) return
        input = ""

        // 脚本模式：直接执行多条命令，不经过 LLM
        if (scriptMode) {
            vm.addAgentMessage(conversationId, AgentMessage(role = "user", content = "【脚本模式】\n$text"))
            sending = true
            scope.launch {
                val results = ShellExecutor.executeScript(text, context)
                val sb = StringBuilder()
                results.forEachIndexed { i, r ->
                    val cmd = text.lines().getOrNull(i) ?: "命令$i"
                    sb.appendLine("▶ $cmd")
                    sb.appendLine(r.stdout.take(500))
                    if (r.stderr.isNotBlank()) sb.appendLine("stderr: ${r.stderr.take(200)}")
                    sb.appendLine("[exit=${r.exitCode}]")
                    sb.appendLine()
                    vm.store.addCommandHistory(
                        com.xzd1314.aichat.data.CommandHistoryItem(
                            command = cmd, success = r.exitCode == 0,
                            exitCode = r.exitCode, outputPreview = r.stdout.take(100)
                        )
                    )
                }
                vm.addAgentMessage(conversationId, AgentMessage(role = "assistant", content = sb.toString()))
                sending = false
            }
            return
        }

        vm.addAgentMessage(conversationId, AgentMessage(role = "user", content = text))
        sending = true
        scope.launch {
            val userSystemPrompt = vm.store.getSystemPrompt()
            val fullSystemPrompt = "$userSystemPrompt\n\n===== Agent 模式说明 =====\n$AGENT_SYSTEM_PROMPT"
            // 构建完整对话历史（含命令执行结果），支持多步任务
            val reqMessages = mutableListOf<com.xzd1314.aichat.data.ChatMessage>()
            reqMessages.add(com.xzd1314.aichat.data.ChatMessage("system", fullSystemPrompt, 0L))
            // 找到第一条 user 消息，跳过之前的欢迎消息等 assistant 内容
            // GLM 等 API 要求 assistant 必须跟在 user 后面，不能以 assistant 开头
            val allMsgs = conv?.messages ?: emptyList()
            val firstUserIdx = allMsgs.indexOfFirst { it.role == "user" }
            val validMsgs = if (firstUserIdx >= 0) allMsgs.drop(firstUserIdx) else allMsgs
            validMsgs.forEach { msg ->
                when (msg.role) {
                    "user" -> reqMessages.add(com.xzd1314.aichat.data.ChatMessage("user", msg.content, 0L))
                    "assistant" -> {
                        val c = if (msg.command != null) "${msg.content}\n[命令: ${msg.command}]" else msg.content
                        reqMessages.add(com.xzd1314.aichat.data.ChatMessage("assistant", c, 0L))
                    }
                    "result" -> {
                        val output = (msg.resultStdout?.takeIf { it.isNotBlank() } ?: msg.resultStderr ?: "").take(2000)
                        reqMessages.add(com.xzd1314.aichat.data.ChatMessage("user",
                            "执行结果（exit=${msg.resultExitCode}，模式=${msg.resultMode ?: "normal"}）：\n$output", 0L))
                    }
                }
            }
            // 合并连续的 user 消息（result 后紧跟新 user 提问），避免 API 报错
            val merged = mutableListOf<com.xzd1314.aichat.data.ChatMessage>()
            reqMessages.forEach { m ->
                if (m.role == "user" && merged.lastOrNull()?.role == "user") {
                    merged[merged.lastIndex] = com.xzd1314.aichat.data.ChatMessage("user",
                        merged.last().content + "\n\n" + m.content, 0L)
                } else {
                    merged.add(m)
                }
            }
            val result = LLMClient.chat(
                preset = vm.store.getResolvedProvider(),
                baseUrl = vm.store.getResolvedBaseUrl(),
                apiKey = vm.store.getApiKey(),
                model = vm.store.getResolvedModelName(),
                messages = merged,
                temperature = 0.3f,
                maxTokens = 1024,
                reasoningEffort = vm.store.getReasoningEffort().ifBlank { null }
            )
            result.onSuccess { (content, reasoning) ->
                val cmd = extractCommand(content)
                vm.addAgentMessage(conversationId, AgentMessage(
                    role = "assistant", content = content, command = cmd,
                    reasoning = reasoning?.takeIf { it.isNotBlank() }
                ))
            }.onFailure { e ->
                vm.addAgentMessage(conversationId, AgentMessage(role = "assistant", content = "请求失败：${e.message}"))
            }
            sending = false
        }
    }

    fun executeCommand(index: Int) {
        if (conv == null) return
        val msg = conv.messages.getOrNull(index) ?: return
        val cmd = msg.command ?: return
        scope.launch {
            val r = ShellExecutor.execute(cmd, context)
            vm.updateAgentMessage(conversationId, index, msg.copy(
                executed = true,
                resultStdout = r.stdout,
                resultStderr = r.stderr,
                resultExitCode = r.exitCode,
                resultMode = r.mode
            ))
            vm.addAgentMessage(conversationId, AgentMessage(
                role = "result",
                content = r.stdout.ifBlank { r.stderr },
                command = cmd,
                resultStdout = r.stdout,
                resultStderr = r.stderr,
                resultExitCode = r.exitCode,
                resultMode = r.mode
            ))
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color(style.bgColor),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Terminal, null, tint = if (style.isDark) Color.White else MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(conv?.title ?: "Agent", fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = if (style.isDark) Color.White else Color.Unspecified)
                            Text(
                                permInfo?.let { permissionDetail(it) } ?: "检测权限中…",
                                style = MaterialTheme.typography.labelMedium,
                                color = when (permInfo?.mode) {
                                    "root" -> Color(0xFFB8975A)
                                    "shizuku" -> Color(0xFF1B4332)
                                    else -> if (style.isDark) Color(0xFF9FB0CC) else MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回",
                            tint = if (style.isDark) Color.White else Color.Unspecified)
                    }
                },
                actions = {
                    // 风格切换
                    Box {
                        IconButton(onClick = { showStyleMenu = true }) {
                            Icon(Icons.Filled.Palette, "聊天风格",
                                tint = if (style.isDark) Color.White else Color.Unspecified)
                        }
                        DropdownMenu(expanded = showStyleMenu, onDismissRequest = { showStyleMenu = false }) {
                            StylePresets.all.take(12).forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.name) },
                                    onClick = {
                                        showStyleMenu = false
                                        vm.setAgentStylePreset(conversationId, s.id)
                                    }
                                )
                            }
                        }
                    }
                    if (permInfo?.shizukuInstalled == true && permInfo?.shizukuAuthorized == false && permInfo?.shizukuConnecting == false) {
                        TextButton(onClick = {
                            try {
                                val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                                if (intent != null) context.startActivity(intent)
                            } catch (_: Exception) {}
                            ShellExecutor.requestShizukuPermission { granted ->
                                if (granted) {
                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                        refreshPerm()
                                    }, 500)
                                } else {
                                    android.widget.Toast.makeText(context, "授权失败，请在 Shizuku 应用中手动授权", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        }) {
                            Text("授权", color = if (style.isDark) Color(0xFF4FC3F7) else MaterialTheme.colorScheme.primary)
                        }
                    }
                    // 三点菜单
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Filled.MoreVert, "更多",
                                tint = if (style.isDark) Color.White else Color.Unspecified)
                        }
                        DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("会话设置（改名）") },
                                onClick = { showMoreMenu = false; showRename = true }
                            )
                            DropdownMenuItem(
                                text = { Text("模型与上下文信息") },
                                leadingIcon = { Icon(Icons.Filled.Info, null) },
                                onClick = { showMoreMenu = false; showModelInfo = true }
                            )
                            DropdownMenuItem(
                                text = { Text("清空消息") },
                                onClick = { showMoreMenu = false; showClearConfirm = true }
                            )
                            DropdownMenuItem(
                                text = { Text("删除会话", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Filled.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { showMoreMenu = false; showDeleteConfirm = true }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(style.topBarColor),
                    titleContentColor = if (style.isDark) Color.White else Color(0xFF141824),
                    navigationIconContentColor = if (style.isDark) Color.White else Color(0xFF141824),
                    actionIconContentColor = if (style.isDark) Color.White else Color(0xFF141824)
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp, horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(conv?.messages?.size ?: 0, key = { i -> conv?.messages?.getOrNull(i)?.content?.hashCode().toString() + i + (conv?.messages?.getOrNull(i)?.role ?: "") }) { i ->
                    val msg = conv?.messages?.getOrNull(i) ?: return@items
                    when (msg.role) {
                        "user" -> ChatBubble(
                            isMine = true,
                            text = msg.content,
                            time = System.currentTimeMillis(),
                            style = style,
                            avatarUri = null,
                            name = agentName
                        )
                        "assistant" -> Column {
                            ChatBubble(
                                isMine = false,
                                text = msg.content,
                                time = System.currentTimeMillis(),
                                style = style,
                                avatarUri = null,
                                reasoning = msg.reasoning,
                                name = agentName
                            )
                            msg.command?.let { cmd ->
                                Spacer(Modifier.height(6.dp))
                                CommandBlock(
                                    cmd = cmd,
                                    executed = msg.executed,
                                    onExecute = { executeCommand(i) }
                                )
                            }
                        }
                        "result" -> ResultBubble(msg)
                    }
                }
                if (sending) {
                    item {
                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                                color = if (style.isDark) Color(0xFF9FB0CC) else MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("思考中…", style = MaterialTheme.typography.bodyMedium,
                                color = if (style.isDark) Color(0xFF9FB0CC) else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // 命令历史按钮
                Box(
                    Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceVariant, androidx.compose.foundation.shape.CircleShape)
                        .clickable { showCmdHistory = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Menu, "命令历史", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(6.dp))
                // 脚本模式按钮
                Box(
                    Modifier.size(44.dp).background(
                        if (scriptMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        androidx.compose.foundation.shape.CircleShape
                    ).clickable { scriptMode = !scriptMode },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Terminal, "脚本模式",
                        tint = if (scriptMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(6.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text(if (scriptMode) "脚本模式：每行一条命令，# 注释…" else "输入指令或自然语言…") },
                    maxLines = if (scriptMode) 8 else 4,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(48.dp)
                        .background(
                            if (sending) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.primary,
                            androidx.compose.foundation.shape.CircleShape
                        )
                        .clickable { send() },
                    contentAlignment = Alignment.Center
                ) {
                    if (sending) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, "发送", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }

        // 命令历史对话框
        if (showCmdHistory) {
            val history = vm.store.getCommandHistory()
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showCmdHistory = false },
                title = { Text("命令历史（${history.size}条）") },
                text = {
                    if (history.isEmpty()) {
                        Text("暂无命令历史", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(300.dp)) {
                            androidx.compose.foundation.lazy.LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(history) { item ->
                                    androidx.compose.material3.Card(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            input = item.command
                                            showCmdHistory = false
                                        },
                                        colors = androidx.compose.material3.CardDefaults.cardColors(
                                            containerColor = if (item.success) MaterialTheme.colorScheme.surfaceVariant
                                            else MaterialTheme.colorScheme.errorContainer
                                        )
                                    ) {
                                        Column(Modifier.padding(10.dp)) {
                                            Text(item.command, style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace, maxLines = 2)
                                            Text(
                                                "exit=${item.exitCode} · ${java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA).format(java.util.Date(item.timestamp))}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { vm.store.clearCommandHistory() }) {
                        Text("清空", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { showCmdHistory = false }) { Text("关闭") }
                }
            )
        }

        // 改名对话框
        if (showRename) {
            var newTitle by remember { mutableStateOf(conv?.title ?: "") }
            AlertDialog(
                onDismissRequest = { showRename = false },
                title = { Text("会话设置") },
                text = {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("对话名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.renameAgentConversation(conversationId, newTitle.ifBlank { "新 Agent 会话" })
                        showRename = false
                    }) { Text("保存") }
                },
                dismissButton = { TextButton(onClick = { showRename = false }) { Text("取消") } }
            )
        }

        // 模型与上下文信息
        if (showModelInfo) {
            val store = vm.store
            AlertDialog(
                onDismissRequest = { showModelInfo = false },
                title = { Text("模型与上下文") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("供应商：${store.getResolvedProvider().name}", style = MaterialTheme.typography.bodyMedium)
                        Text("模型：${store.getResolvedModelName()}", style = MaterialTheme.typography.bodyMedium)
                        Text("上下文：最近 ${store.getContextCount()} 条消息", style = MaterialTheme.typography.bodyMedium)
                        Text("消息数：${conv?.messages?.size ?: 0} 条", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        Text("Agent 模式使用 temperature=0.3，maxTokens=1024，确保命令输出准确。",
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = { TextButton(onClick = { showModelInfo = false }) { Text("知道了") } }
            )
        }

        // 清空确认
        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text("清空消息") },
                text = { Text("将删除该会话的全部 ${conv?.messages?.size ?: 0} 条消息，此操作不可恢复。") },
                confirmButton = {
                    TextButton(onClick = {
                        vm.clearAgentMessages(conversationId)
                        showClearConfirm = false
                    }) { Text("清空", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("取消") } }
            )
        }

        // 删除确认
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("删除会话") },
                text = { Text("确定删除「${conv?.title ?: ""}」吗？") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        vm.deleteAgentConversation(conversationId)
                        onBack()
                    }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") } }
            )
        }
    }
}

private fun permissionDetail(info: ShellExecutor.PermissionInfo): String {
    val rootStr = when (info.rootManager) {
        "magisk" -> "·Magisk"
        "kernelsu" -> "·KernelSU"
        "su" -> "·su"
        else -> ""
    }
    return when (info.mode) {
        "root" -> "Root$rootStr"
        "shizuku" -> "Shizuku"
        else -> {
            if (info.shizukuConnecting) return "Shizuku连接中…"
            val hints = buildList {
                if (info.rootManager != null) add("Root未授权")
                if (info.shizukuInstalled) add("Shizuku未授权")
            }
            "普通权限${if (hints.isNotEmpty()) "（${hints.joinToString("·")}）" else ""}"
        }
    }
}

/** 从 AI 回复中提取 ```sh ... ``` 命令 */
private fun extractCommand(text: String): String? {
    val regex = Regex("```sh\\s*\\n?(.*?)\\n?```", RegexOption.DOT_MATCHES_ALL)
    val match = regex.find(text)
    return match?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
}

@Composable
private fun CommandBlock(cmd: String, executed: Boolean, onExecute: () -> Unit) {
    Box(
        Modifier
            .widthIn(max = 300.dp)
            .background(Color(0xFF1B4332), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Terminal, null, tint = Color(0xFFC9A961), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("命令", style = MaterialTheme.typography.labelMedium, color = Color(0xFFC9A961),
                    fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                if (executed) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFFB7D8C4), modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(cmd, color = Color(0xFFE8E0CC), fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(8.dp))
            if (!executed) {
                TextButton(
                    onClick = onExecute,
                    modifier = Modifier.background(Color(0xFFC9A961), RoundedCornerShape(8.dp))
                ) {
                    Text("执行", color = Color(0xFF1B4332), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ResultBubble(msg: AgentMessage) {
    val success = msg.resultExitCode == 0
    Column(Modifier.fillMaxWidth().padding(start = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (success) Icons.Filled.CheckCircle else Icons.Filled.Error,
                null,
                tint = if (success) Color(0xFF1B4332) else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "执行结果 · ${msg.resultMode ?: "?"} · exit=${msg.resultExitCode ?: "?"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    if (success) Color(0xFFE8E4D6) else Color(0xFFF5D5D8),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                msg.content.ifBlank { "(无输出)" },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 20
            )
        }
    }
}
