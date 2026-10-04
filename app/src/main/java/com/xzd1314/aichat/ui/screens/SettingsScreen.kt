package com.xzd1314.aichat.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xzd1314.aichat.AppViewModel
import com.xzd1314.aichat.data.ModelPreset
import com.xzd1314.aichat.data.ModelPresets
import com.xzd1314.aichat.net.LLMClient
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = vm.store

    var providerId by remember { mutableStateOf(store.getProviderId()) }
    var apiKey by remember { mutableStateOf(store.getApiKey()) }
    var modelName by remember { mutableStateOf(store.getModelName()) }
    var baseUrl by remember { mutableStateOf(store.getBaseUrl()) }
    var ctxCount by remember { mutableStateOf(store.getContextCount()) }
    var maxTokens by remember { mutableStateOf(store.getMaxTokens()) }
    var temperature by remember { mutableStateOf(store.getTemperature()) }
    var systemPrompt by remember { mutableStateOf(store.getSystemPrompt()) }
    var cacheOpt by remember { mutableStateOf(store.isCacheOptimized()) }
    var reasoningEffort by remember { mutableStateOf(store.getReasoningEffort()) }
    var showProviderPicker by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    // Prompt 模板
    var newTplName by remember { mutableStateOf("") }
    var newTplContent by remember { mutableStateOf("") }
    // 定时任务
    var newTaskName by remember { mutableStateOf("") }
    var newTaskCmd by remember { mutableStateOf("") }
    var newTaskInterval by remember { mutableStateOf(60) }
    // 本地模型
    var localModelUrl by remember { mutableStateOf(store.getBaseUrl()) }
    var localModelName by remember { mutableStateOf(store.getModelName()) }

    val provider = ModelPresets.get(providerId)

    Scaffold(
        modifier = modifier,
        topBar = {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text("设置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "模型接入 · 上下文 · 缓存优化",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            SectionCard(title = "模型接入", icon = Icons.Filled.CloudDone) {
                // 供应商选择
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { showProviderPicker = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("API 供应商", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            provider.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(Icons.Filled.ExpandMore, "选择", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it; store.setApiKey(it) },
                    label = { Text("API Key") },
                    placeholder = { Text("sk-…") },
                    leadingIcon = { Icon(Icons.Filled.Key, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it; store.setModelName(it) },
                    label = { Text("模型名称") },
                    placeholder = { Text(provider.defaultModel) },
                    supportingText = { Text("留空使用默认：${provider.defaultModel}") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it; store.setBaseUrl(it) },
                    label = { Text("Base URL（可选）") },
                    placeholder = { Text(provider.baseUrl) },
                    supportingText = { Text("留空使用供应商默认端点") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            testing = true
                            testResult = null
                            scope.launch {
                                val r = LLMClient.testConnection(
                                    preset = ModelPresets.get(providerId),
                                    baseUrl = baseUrl,
                                    apiKey = apiKey,
                                    model = modelName.ifBlank { ModelPresets.get(providerId).defaultModel }
                                )
                                testResult = r.fold(
                                    onSuccess = { ms -> "连接成功，延迟 ${ms}ms" },
                                    onFailure = { e -> "连接失败：${e.message}" }
                                )
                                testing = false
                            }
                        },
                        enabled = !testing && apiKey.isNotBlank()
                    ) {
                        if (testing) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                        } else {
                            Icon(Icons.Filled.RocketLaunch, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        Text("测试连接")
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        testResult ?: "填好 Key 后点击测试",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (testResult?.startsWith("连接成功") == true)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }

            SectionCard(title = "上下文与输出", icon = Icons.Filled.Memory) {
                SliderItem(
                    label = "上下文消息条数",
                    desc = "发送给模型的最近消息数（含 system）",
                    value = ctxCount.toFloat(), range = 4f..64f, step = 1f,
                    display = "$ctxCount 条",
                    onChange = { ctxCount = it.toInt(); store.setContextCount(it.toInt()) }
                )
                SliderItem(
                    label = "最大输出 Token",
                    desc = "单次回复长度上限",
                    value = maxTokens.toFloat(), range = 256f..8192f, step = 256f,
                    display = "$maxTokens",
                    onChange = { maxTokens = it.toInt(); store.setMaxTokens(it.toInt()) }
                )
                SliderItem(
                    label = "温度",
                    desc = "越低越稳定，越高越有创意",
                    value = temperature, range = 0f..2f, step = 0.1f,
                    display = "%.1f".format(temperature),
                    onChange = { temperature = it; store.setTemperature(it) }
                )
            }

            SectionCard(title = "思考模式", icon = Icons.Filled.Psychology) {
                Text(
                    "开启后模型先内部推理再输出答案，思考过程可在聊天中折叠查看。需使用支持 reasoning 的模型（如 deepseek-reasoner、glm-4.5、kimi-k2 等）。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("" to "关闭", "low" to "低", "medium" to "中", "high" to "高").forEach { (value, label) ->
                        val selected = reasoningEffort == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    reasoningEffort = value
                                    store.setReasoningEffort(value)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            SectionCard(title = "角色卡 / 人格预设", icon = Icons.Filled.Psychology) {
                Text(
                    "点击角色卡一键切换 AI 人格，系统提示词会自动替换。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                androidx.compose.foundation.layout.Box(
                    Modifier.fillMaxWidth().height(100.dp)
                ) {
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(com.xzd1314.aichat.data.CharacterPresets.ALL) { card ->
                            val isActive = systemPrompt == card.prompt
                            androidx.compose.material3.Card(
                                modifier = Modifier
                                    .width(160.dp)
                                    .fillMaxHeight()
                                    .clickable {
                                        systemPrompt = card.prompt
                                        store.setSystemPrompt(card.prompt)
                                        android.widget.Toast.makeText(context, "已切换为「${card.name}」", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(card.name, fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                    Spacer(Modifier.height(4.dp))
                                    Text(card.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3, lineHeight = 16.sp)
                                }
                            }
                        }
                    }
                }
            }

            SectionCard(title = "系统提示词", icon = Icons.Filled.Tune) {
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it; store.setSystemPrompt(it) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "提示词改动会破坏前缀缓存，修改后首次请求将重新计费缓存。",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard(title = "缓存命中优化", icon = Icons.Filled.Speed) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("前缀缓存优化", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "固定 system 前缀 + 确定性序列化 + 尾部截断，最大化 DeepSeek/GLM 等自动前缀缓存命中率，长对话省钱提速",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = cacheOpt,
                        onCheckedChange = { cacheOpt = it; store.setCacheOptimized(it) }
                    )
                }
            }

            SectionCard(title = "使用统计", icon = Icons.Filled.CheckCircle) {
                val count = store.getStatCount()
                val avg = if (count > 0) store.getStatTotal() / count else 0L
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    StatItem("请求次数", "$count")
                    StatItem("平均延迟", "${avg}ms")
                }
            }

            // ===== API 用量与费用监控 =====
            SectionCard(title = "API 用量监控", icon = Icons.Filled.Speed) {
                val usage = store.getTokenUsage()
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatItem("输入Token", "${usage.totalInputTokens / 1000}K")
                    StatItem("输出Token", "${usage.totalOutputTokens / 1000}K")
                    StatItem("估算费用", "¥${usage.totalCostCents / 100.0}")
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { store.resetTokenUsage() }) {
                    Text("重置统计", color = MaterialTheme.colorScheme.error)
                }
            }

            // ===== Prompt 模板管理器 =====
            SectionCard(title = "Prompt 模板管理器", icon = Icons.Filled.Create) {
                val templates = store.getPromptTemplates()
                Text("已保存 ${templates.size} 个模板，点击一键应用到系统提示词。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                templates.forEach { tpl ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(tpl.name, fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodyMedium)
                            Text(tpl.content.take(40) + "...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1)
                        }
                        TextButton(onClick = {
                            systemPrompt = tpl.content
                            store.setSystemPrompt(tpl.content)
                            android.widget.Toast.makeText(context, "已应用「${tpl.name}」", android.widget.Toast.LENGTH_SHORT).show()
                        }) { Text("应用") }
                        TextButton(onClick = { store.deletePromptTemplate(tpl.id) }) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = newTplName,
                    onValueChange = { newTplName = it },
                    label = { Text("新模板名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = newTplContent,
                    onValueChange = { newTplContent = it },
                    label = { Text("新模板内容（当前系统提示词可直接保存）") },
                    minLines = 2, maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = {
                    if (newTplName.isNotBlank() && newTplContent.isNotBlank()) {
                        store.addPromptTemplate(newTplName, newTplContent)
                        newTplName = ""; newTplContent = ""
                    }
                }) { Text("保存当前为模板") }
            }

            // ===== 定时任务 =====
            SectionCard(title = "Agent 定时任务", icon = Icons.Filled.DateRange) {
                val tasks = store.getScheduledTasks()
                Text("定时执行 shell 命令，最小间隔 15 分钟（系统限制）。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                tasks.forEach { task ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(task.name, fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodyMedium)
                            Text("${task.command.take(30)} · 每${task.intervalMinutes}分钟",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = task.enabled,
                            onCheckedChange = { store.toggleScheduledTask(task.id) }
                        )
                        TextButton(onClick = { store.deleteScheduledTask(task.id) }) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = newTaskName,
                    onValueChange = { newTaskName = it },
                    label = { Text("任务名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = newTaskCmd,
                    onValueChange = { newTaskCmd = it },
                    label = { Text("shell 命令") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("间隔：${newTaskInterval}分钟", Modifier.weight(1f))
                    Slider(
                        value = newTaskInterval.toFloat(),
                        onValueChange = { newTaskInterval = it.toInt() },
                        valueRange = 15f..1440f,
                        modifier = Modifier.weight(2f)
                    )
                }
                TextButton(onClick = {
                    if (newTaskName.isNotBlank() && newTaskCmd.isNotBlank()) {
                        store.addScheduledTask(newTaskName, newTaskCmd, newTaskInterval)
                        newTaskName = ""; newTaskCmd = ""
                    }
                }) { Text("添加定时任务") }
            }

            // ===== 本地模型接入 =====
            SectionCard(title = "本地模型接入", icon = Icons.Filled.Memory) {
                Text("接入局域网内的 llama.cpp / Ollama 等本地模型，断网也能用。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = localModelUrl,
                    onValueChange = { localModelUrl = it; store.setBaseUrl(it) },
                    label = { Text("本地端点 URL（如 http://192.168.1.100:11434/v1/chat/completions）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = localModelName,
                    onValueChange = { localModelName = it; store.setModelName(it) },
                    label = { Text("本地模型名（如 llama3.2、qwen2.5-7b）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = {
                    store.setProviderId("custom")
                    android.widget.Toast.makeText(context, "已切换到本地模型", android.widget.Toast.LENGTH_SHORT).show()
                }) { Text("启用本地模型") }
            }

            Spacer(Modifier.height(20.dp))
        }
    }

    if (showProviderPicker) {
        ProviderPickerDialog(
            currentId = providerId,
            onPick = {
                providerId = it
                store.setProviderId(it)
                showProviderPicker = false
                testResult = null
            },
            onDismiss = { showProviderPicker = false }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SliderItem(
    label: String,
    desc: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    display: String,
    onChange: (Float) -> Unit
) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(desc, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(display, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = ((range.endInclusive - range.start) / step).toInt() - 1
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProviderPickerDialog(
    currentId: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择模型供应商") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ModelPresets.all.forEach { p ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(p.id) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(
                                    if (p.id == currentId) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(5.dp)
                                )
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                p.doc,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
