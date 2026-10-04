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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.xzd1314.aichat.net.LLMClient
import kotlinx.coroutines.launch

private const val AGENT_SYSTEM_PROMPT = """你是一个 Android 手机控制 Agent（名字叫小蓝）。用户告诉你要做什么，你输出对应的 shell 命令。
规则：
1. 输出简短说明，然后用 ```sh ... ``` 包裹一条 shell 命令
2. 多条命令用 && 或 ; 连接成一条
3. 优先用 Android 通用命令：am, pm, settings, dumpsys, input, ls, cat, getprop, setprop, svc, cmd, content 等
4. 不要输出多余解释，命令要可直接执行
5. 如果用户直接输入命令，原样输出
6. 危险操作（rm -rf /、格式化、恢复出厂）必须先警告用户"""

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
    var sending by remember { mutableStateOf(false) }
    var permInfo by remember { mutableStateOf(ShellExecutor.getPermissionInfo(context)) }

    fun refreshPerm() { permInfo = ShellExecutor.getPermissionInfo(context) }

    // 注册 Shizuku 连接监听器（用户在 Shizuku 应用授权后自动刷新）
    LaunchedEffect(Unit) {
        ShellExecutor.registerShizukuListener {
            refreshPerm()
        }
    }

    // 首次进入且无消息时，添加欢迎消息
    LaunchedEffect(conv) {
        if (conv != null && conv.messages.isEmpty()) {
            val rootStr = when (permInfo.rootManager) {
                "magisk" -> "Magisk"
                "kernelsu" -> "KernelSU"
                "su" -> "su"
                else -> null
            }
            val modeDesc = when (permInfo.mode) {
                "root" -> "Root 权限${rootStr?.let { "（$it）" } ?: ""}"
                "shizuku" -> "Shizuku 权限"
                else -> "普通权限（功能受限，建议 Root 或 Shizuku）"
            }
            vm.addAgentMessage(conversationId, AgentMessage(
                role = "assistant",
                content = "你好！我是 Agent 小蓝，可以帮你通过 shell 命令控制手机。\n\n" +
                    "当前权限：$modeDesc\n" +
                    if (permInfo.shizukuInstalled && !permInfo.shizukuAuthorized)
                        "\n⚠️ 检测到 Shizuku 已安装但未授权，点击右上角「授权」按钮授权。"
                    else "" +
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
                    // 记录命令历史
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
            val reqMessages = listOf(
                com.xzd1314.aichat.data.ChatMessage("system", fullSystemPrompt, 0L),
                com.xzd1314.aichat.data.ChatMessage("user", text, 0L)
            )
            val result = LLMClient.chat(
                preset = vm.store.getResolvedProvider(),
                baseUrl = vm.store.getResolvedBaseUrl(),
                apiKey = vm.store.getApiKey(),
                model = vm.store.getResolvedModelName(),
                messages = reqMessages,
                temperature = 0.3f,
                maxTokens = 1024
            )
            result.onSuccess { (content, _) ->
                val cmd = extractCommand(content)
                vm.addAgentMessage(conversationId, AgentMessage(role = "assistant", content = content, command = cmd))
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
        // 标记执行中（临时更新本地状态，不持久化 executing）
        scope.launch {
            val r = ShellExecutor.execute(cmd, context)
            // 更新原消息为已执行
            vm.updateAgentMessage(conversationId, index, msg.copy(
                executed = true,
                resultStdout = r.stdout,
                resultStderr = r.stderr,
                resultExitCode = r.exitCode,
                resultMode = r.mode
            ))
            // 添加结果消息
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Terminal, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(conv?.title ?: "Agent", fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                permissionDetail(permInfo),
                                style = MaterialTheme.typography.labelMedium,
                                color = when (permInfo.mode) {
                                    "root" -> Color(0xFFB8975A)
                                    "shizuku" -> Color(0xFF1B4332)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (permInfo.shizukuInstalled && !permInfo.shizukuAuthorized) {
                        TextButton(onClick = {
                            ShellExecutor.requestShizukuPermission { granted ->
                                if (granted) {
                                    // 授权成功后延迟刷新，等 Shizuku 服务连接稳定
                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                        refreshPerm()
                                    }, 500)
                                }
                            }
                        }) {
                            Text("授权", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    TextButton(onClick = { refreshPerm() }) { Text("刷新") }
                }
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
                items(conv?.messages ?: emptyList(), key = { it.content.hashCode() + it.role.hashCode() }) { msg ->
                    val index = conv?.messages?.indexOf(msg) ?: 0
                    when (msg.role) {
                        "user" -> UserBubble(msg.content)
                        "assistant" -> AssistantBubble(
                            msg = msg,
                            onExecute = { executeCommand(index) }
                        )
                        "result" -> ResultBubble(msg)
                    }
                }
                if (sending) {
                    item {
                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("思考中…", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, maxLines = 2)
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
private fun UserBubble(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            Modifier
                .widthIn(max = 280.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(text, color = MaterialTheme.colorScheme.onPrimary, fontSize = 15.sp)
        }
    }
}

@Composable
private fun AssistantBubble(msg: AgentMessage, onExecute: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .widthIn(max = 300.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(msg.content, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp,
                maxLines = 8, overflow = TextOverflow.Ellipsis)
        }
        msg.command?.let { cmd ->
            Spacer(Modifier.height(6.dp))
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
                        if (msg.executed) {
                            Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFFB7D8C4), modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(cmd, color = Color(0xFFE8E0CC), fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(8.dp))
                    if (!msg.executed) {
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
