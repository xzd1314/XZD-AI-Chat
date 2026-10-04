package com.xzd1314.aichat.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xzd1314.aichat.AppViewModel
import com.xzd1314.aichat.data.Conversation
import com.xzd1314.aichat.data.StylePresets
import com.xzd1314.aichat.net.LLMClient
import com.xzd1314.aichat.ui.components.ChatBubble
import com.xzd1314.aichat.ui.components.CircularAvatar
import com.xzd1314.aichat.util.Util
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    vm: AppViewModel,
    conversation: Conversation,
    onBack: () -> Unit,
    onOpenStyle: (String) -> Unit,
    onSwitchConversation: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var input by remember(conversation.id) { mutableStateOf("") }
    var sending by remember(conversation.id) { mutableStateOf(false) }
    var errorMsg by remember(conversation.id) { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var showConvSettings by remember { mutableStateOf(false) }
    var showModelInfo by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showConvList by remember { mutableStateOf(false) }
    var selectedImageUri by remember(conversation.id) { mutableStateOf<String?>(null) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) vm.setAvatar(conversation.id, uri.toString())
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // 持久化 URI 权限
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            // 保存到预览状态，用户可输入文字后一起发送
            selectedImageUri = uri.toString()
        }
    }

    // 语音输入
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val text = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!text.isNullOrBlank()) input = text
        }
    }

    // 录音权限请求
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // 权限授予后启动语音识别
            val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "请说话…")
            }
            try { voiceLauncher.launch(intent) } catch (_: Exception) {
                android.widget.Toast.makeText(context, "当前设备不支持语音输入，请安装语音识别服务", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            android.widget.Toast.makeText(context, "需要麦克风权限才能使用语音输入", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // 新消息自动滚到底
    LaunchedEffect(conversation.messages.size, sending) {
        if (conversation.messages.isNotEmpty()) {
            listState.animateScrollToItem(conversation.messages.size - 1)
        }
    }

    val style = conversation.style

    fun send() {
        val text = input.trim()
        val hasImage = selectedImageUri != null
        if (text.isEmpty() && !hasImage) return
        if (sending) return
        input = ""
        errorMsg = null
        // 图片和文字一起发送（同一条消息）
        vm.addMessage(conversation.id, "user", text, imageUri = selectedImageUri)
        selectedImageUri = null
        sending = true
        scope.launch {
            val conv = vm.getById(conversation.id) ?: return@launch
            val (reqMessages, sentCount, sentTokens) = LLMClient.buildRequestMessages(
                vm.store.getSystemPrompt(),
                conv.messages,
                vm.store.getContextCount()
            )
            val start = System.currentTimeMillis()
            val result = LLMClient.chat(
                preset = vm.store.getResolvedProvider(),
                baseUrl = vm.store.getResolvedBaseUrl(),
                apiKey = vm.store.getApiKey(),
                model = vm.store.getResolvedModelName(),
                messages = reqMessages,
                temperature = vm.store.getTemperature(),
                maxTokens = vm.store.getMaxTokens(),
                reasoningEffort = vm.store.getReasoningEffort().ifBlank { null }
            )
            vm.store.bumpStat(System.currentTimeMillis() - start)
            result.onSuccess { (reply, reasoning) ->
                vm.addMessage(conversation.id, "assistant", reply.trim(), reasoning)
            }.onFailure { e ->
                errorMsg = e.message ?: "请求失败"
            }
            sending = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularAvatar(conversation.avatarUri, 40.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                conversation.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                StylePresets.get(conversation.stylePresetId).name.let { "风格：$it" },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    Row {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                        }
                        IconButton(onClick = { showConvList = true }) {
                            Icon(Icons.Filled.Menu, "会话列表")
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, "更多")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("会话设置（名称/头像）") },
                                onClick = { menuOpen = false; showConvSettings = true }
                            )
                            DropdownMenuItem(
                                text = { Text("聊天风格") },
                                leadingIcon = { Icon(Icons.Filled.Palette, null) },
                                onClick = { menuOpen = false; onOpenStyle(conversation.id) }
                            )
                            DropdownMenuItem(
                                text = { Text("模型与上下文信息") },
                                leadingIcon = { Icon(Icons.Filled.Info, null) },
                                onClick = { menuOpen = false; showModelInfo = true }
                            )
                            DropdownMenuItem(
                                text = { Text("导出会话（Markdown）") },
                                leadingIcon = { Icon(Icons.Filled.Create, null) },
                                onClick = {
                                    menuOpen = false
                                    val md = vm.store.exportConversationAsMarkdown(conversation)
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, conversation.title)
                                        putExtra(android.content.Intent.EXTRA_TEXT, md)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "导出会话"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("导出为纯文本") },
                                leadingIcon = { Icon(Icons.Filled.Star, null) },
                                onClick = {
                                    menuOpen = false
                                    val txt = vm.store.exportConversationAsText(conversation)
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_TEXT, txt)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "导出文本"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("痕迹分享（长图）") },
                                leadingIcon = { Icon(Icons.Filled.Refresh, null) },
                                onClick = {
                                    menuOpen = false
                                    scope.launch {
                                        try {
                                            val file = com.xzd1314.aichat.util.LongShare.renderConversation(conversation, context)
                                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                                context, "${context.packageName}.fileprovider", file
                                            )
                                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "image/png"
                                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(android.content.Intent.createChooser(shareIntent, "分享痕迹长图"))
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "生成长图失败：${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("清空消息") },
                                onClick = { menuOpen = false; showClearConfirm = true }
                            )
                            DropdownMenuItem(
                                text = { Text("删除会话", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Filled.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menuOpen = false; showDeleteConfirm = true }
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
                .fillMaxSize()
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
            ) {
                items(conversation.messages.size, key = { i -> conversation.messages[i].ts.toString() + i }) { i ->
                    val m = conversation.messages[i]
                    ChatBubble(
                        modifier = Modifier,
                        isMine = m.role == "user",
                        text = m.content,
                        time = m.ts,
                        style = style,
                        avatarUri = if (m.role == "user") null else conversation.avatarUri,
                        reasoning = m.reasoning,
                        imageUri = m.imageUri
                    )
                }
                if (sending) {
                    item { TypingIndicator(style.isDark) }
                }
                errorMsg?.let { err ->
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(
                                err,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            )
                        }
                    }
                }
            }

            // 图片预览区域（选中图片后显示，可删除，可与文字一起发送）
            if (selectedImageUri != null) {
                var previewBitmap by remember(selectedImageUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
                LaunchedEffect(selectedImageUri) {
                    previewBitmap = Util.loadBitmap(context, selectedImageUri!!)
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(Modifier.size(80.dp)) {
                        if (previewBitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = previewBitmap!!.asImageBitmap(),
                                contentDescription = "待发送图片",
                                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Box(Modifier.size(80.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)))
                        }
                        // 删除按钮
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .size(22.dp)
                                .background(MaterialTheme.colorScheme.error, CircleShape)
                                .clickableNoRipple { selectedImageUri = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Close, "移除图片", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "图片已添加，输入文字后点击发送即可一起发出",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // ➕ 按钮：发送图片
                Box(
                    Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .clickableNoRipple {
                            imagePicker.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, "发送图片", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(8.dp))
                // 🎤 语音输入按钮
                Box(
                    Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .clickableNoRipple {
                            // 先检查录音权限，没有则申请
                            if (androidx.core.content.ContextCompat.checkSelfPermission(
                                    context, android.Manifest.permission.RECORD_AUDIO
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                                    putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "请说话…")
                                }
                                try { voiceLauncher.launch(intent) } catch (_: Exception) {
                                    android.widget.Toast.makeText(context, "当前设备不支持语音输入，请安装语音识别服务", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                recordPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Phone, "语音输入", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("给小蓝发消息…") },
                    maxLines = 5,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(48.dp)
                        .background(
                            if (sending) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
                        .clickableNoRipple { send() },
                    contentAlignment = Alignment.Center
                ) {
                    if (sending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "发送",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }

    if (showConvSettings) {
        ConvSettingsDialog(
            vm = vm,
            conversation = conversation,
            onPickAvatar = {
                avatarPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDismiss = { showConvSettings = false }
        )
    }
    if (showModelInfo) {
        ModelInfoDialog(
            vm = vm,
            conversation = conversation,
            onDismiss = { showModelInfo = false }
        )
    }
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空消息") },
            text = { Text("将删除该会话的全部 ${conversation.messages.size} 条消息，此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearMessages(conversation.id)
                    showClearConfirm = false
                }) { Text("清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("取消") }
            }
        )
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除会话") },
            text = { Text("确定删除「${conversation.title}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    vm.deleteConversation(conversation.id)
                    onBack()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )

        // 多会话快捷侧边栏（BottomSheet）
        if (showConvList) {
            val allConvs = vm.convs.value
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { showConvList = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Text("全部会话", modifier = Modifier.padding(horizontal = 20.dp),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                androidx.compose.foundation.layout.Box(
                    Modifier.fillMaxWidth().height(400.dp).padding(horizontal = 12.dp)
                ) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(allConvs) { c ->
                            val isCurrent = c.id == conversation.id
                            androidx.compose.material3.Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    showConvList = false
                                    onSwitchConversation(c.id)
                                },
                                colors = androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    CircularAvatar(uri = c.avatarUri, size = 36.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(c.title, fontWeight = FontWeight.Medium, maxLines = 1)
                                        Text(c.messages.lastOrNull()?.content?.take(30) ?: "暂无消息",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                    if (isCurrent) {
                                        Icon(Icons.Filled.CheckCircle, "当前", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TypingIndicator(dark: Boolean) {
    val dotColor = if (dark) Color(0xFF9FB0CC) else Color(0xFF8A94A6)
    Row(
        Modifier.padding(start = 16.dp, top = 4.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.SmartToy, null, tint = dotColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .background(if (dark) Color(0xFF1F2A3D) else Color(0xFFE8ECF4), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            repeat(3) {
                Box(
                    Modifier
                        .size(6.dp)
                        .background(dotColor, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))

@Composable
private fun ConvSettingsDialog(
    vm: AppViewModel,
    conversation: Conversation,
    onPickAvatar: () -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember(conversation.id) { mutableStateOf(conversation.title) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("会话设置") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularAvatar(conversation.avatarUri, 56.dp)
                    Spacer(Modifier.width(14.dp))
                    TextButton(onClick = onPickAvatar) {
                        Icon(Icons.Filled.PhotoCamera, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (conversation.avatarUri == null) "选择头像" else "更换头像")
                    }
                    if (conversation.avatarUri != null) {
                        TextButton(onClick = { vm.setAvatar(conversation.id, null) }) {
                            Text("恢复默认")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("对话名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.rename(conversation.id, title)
                onDismiss()
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun ModelInfoDialog(
    vm: AppViewModel,
    conversation: Conversation,
    onDismiss: () -> Unit
) {
    val store = vm.store
    val provider = store.getResolvedProvider()
    val model = store.getResolvedModelName()
    val ctxCount = store.getContextCount()
    val (_, sentCount, sentTokens) = LLMClient.buildRequestMessages(
        store.getSystemPrompt(), conversation.messages, ctxCount
    )
    val cacheOpt = store.isCacheOptimized()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("模型与上下文") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoRow("模型供应商", provider.name)
                InfoRow("当前模型", model)
                InfoRow("上下文窗口", "最近 $ctxCount 条消息")
                InfoRow("本轮将发送", "$sentCount 条消息")
                InfoRow("估算 Token", "≈ $sentTokens tokens")
                InfoRow("缓存优化", if (cacheOpt) "已开启（前缀稳定，命中优先）" else "已关闭")
                Spacer(Modifier.height(4.dp))
                Text(
                    "提示：缓存优化模式下，系统提示词与历史消息按固定顺序发送，不注入动态字段，DeepSeek/GLM 等服务会自动命中前缀缓存，省钱又提速。",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

/** 供外部使用的取色上下文辅助 */
fun styleNameOf(conv: Conversation): String = StylePresets.get(conv.stylePresetId).name
