package com.xzd1314.aichat.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xzd1314.aichat.AppViewModel
import com.xzd1314.aichat.data.ChatStyle
import com.xzd1314.aichat.data.Conversation
import com.xzd1314.aichat.data.StylePresets
import com.xzd1314.aichat.ui.components.ColorPickerDialog
import com.xzd1314.aichat.ui.components.StylePreview
import com.xzd1314.aichat.util.Util

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleEditorScreen(
    vm: AppViewModel,
    conversation: Conversation,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var current by remember(conversation.id) { mutableStateOf(conversation.style) }
    var currentPresetId by remember(conversation.id) { mutableStateOf(conversation.stylePresetId) }
    var customMode by remember(conversation.id) { mutableStateOf(conversation.stylePresetId == "custom") }
    var editingField by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }

    fun apply(presetId: String, style: ChatStyle) {
        currentPresetId = presetId
        current = style
        customMode = presetId == "custom"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("聊天风格") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                actions = {
                    TextButton(onClick = { customMode = true; currentPresetId = "custom" }) {
                        Icon(Icons.Filled.Edit, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("自定义")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionButton(
                    icon = { Icon(Icons.Filled.Save, null, Modifier.size(18.dp)) },
                    label = "保存",
                    onClick = {
                        vm.applyStyle(conversation.id, current, currentPresetId)
                        Toast.makeText(context, "风格已保存", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = { Icon(Icons.Filled.ContentCopy, null, Modifier.size(18.dp)) },
                    label = "分享",
                    onClick = {
                        val encoded = Util.encodeStyle(current.toJson())
                        Util.copyToClipboard(context, "风格分享", encoded)
                        Toast.makeText(context, "分享文本已复制到剪贴板", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = { Icon(Icons.Filled.Download, null, Modifier.size(18.dp)) },
                    label = "导入",
                    onClick = { showImportDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 实时预览
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(Modifier.padding(8.dp)) {
                    Text(
                        "实时预览 · ${current.name}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                    StylePreview(
                        current,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    )
                }
            }

            // 预设网格
            Text(
                "预设风格（${StylePresets.all.size} 套）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(340.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(StylePresets.all, key = { it.id }) { preset ->
                    PresetCard(
                        style = preset,
                        selected = !customMode && currentPresetId == preset.id,
                        onClick = { apply(preset.id, preset) }
                    )
                }
            }

            // 自定义编辑区
            AnimatedVisibility(visible = customMode) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Text(
                        "自定义编辑",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    ColorField("我方气泡", current.myBubbleColor) { editingField = "myBubble" }
                    ColorField("对方气泡", current.aiBubbleColor) { editingField = "aiBubble" }
                    ColorField("我方文字", current.myTextColor) { editingField = "myText" }
                    ColorField("对方文字", current.aiTextColor) { editingField = "aiText" }
                    ColorField("聊天背景", current.bgColor) { editingField = "bg" }
                    ColorField("顶部栏", current.topBarColor) { editingField = "topBar" }

                    SliderField("气泡圆角", current.bubbleRadius.toFloat(), 0f..32f) {
                        current = current.copy(bubbleRadius = it.toInt())
                    }
                    SliderField("字号", current.fontSize.toFloat(), 12f..22f) {
                        current = current.copy(fontSize = it.toInt())
                    }
                    SliderField("气泡最大宽度", current.bubbleMaxWidthPct.toFloat(), 40f..95f) {
                        current = current.copy(bubbleMaxWidthPct = it.toInt())
                    }
                    SwitchField("显示头像", current.showAvatar) { current = current.copy(showAvatar = it) }
                    SwitchField("显示时间", current.showTime) { current = current.copy(showTime = it) }
                    SwitchField("显示对方昵称", current.showNameLabel) { current = current.copy(showNameLabel = it) }
                    SwitchField("深色文字模式", current.isDark) { current = current.copy(isDark = it) }
                }
            }
            Spacer(Modifier.height(90.dp))
        }
    }

    // 颜色编辑对话框
    editingField?.let { field ->
        val initial = when (field) {
            "myBubble" -> current.myBubbleColor
            "aiBubble" -> current.aiBubbleColor
            "myText" -> current.myTextColor
            "aiText" -> current.aiTextColor
            "bg" -> current.bgColor
            else -> current.topBarColor
        }
        val label = when (field) {
            "myBubble" -> "我方气泡"
            "aiBubble" -> "对方气泡"
            "myText" -> "我方文字"
            "aiText" -> "对方文字"
            "bg" -> "聊天背景"
            else -> "顶部栏"
        }
        ColorPickerDialog(
            title = label,
            initial = initial,
            onConfirm = { c ->
                current = when (field) {
                    "myBubble" -> current.copy(myBubbleColor = c)
                    "aiBubble" -> current.copy(aiBubbleColor = c)
                    "myText" -> current.copy(myTextColor = c)
                    "aiText" -> current.copy(aiTextColor = c)
                    "bg" -> current.copy(bgColor = c)
                    else -> current.copy(topBarColor = c)
                }
                editingField = null
            },
            onDismiss = { editingField = null }
        )
    }

    if (showImportDialog) {
        val clip = Util.readClipboard(context)
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("导入风格") },
            text = {
                Column {
                    Text(
                        "从剪贴板读取分享文本（XZD-STYLE:v1.…）并应用到当前会话。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        clip?.take(120) ?: "剪贴板为空",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val json = clip?.let { Util.decodeStyle(it) }
                    if (json != null) {
                        val imported = ChatStyle.fromJson(json)
                            .with("custom", "导入·${json.optString("name", "自定义")}")
                        apply("custom", imported)
                        Toast.makeText(context, "导入成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "剪贴板中没有有效的风格分享文本", Toast.LENGTH_SHORT).show()
                    }
                    showImportDialog = false
                }) { Text("导入") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun PresetCard(style: ChatStyle, selected: Boolean, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(8.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(style.bgColor), RoundedCornerShape(10.dp))
                    .padding(4.dp)
            ) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .width(30.dp)
                            .height(20.dp)
                            .background(Color(style.aiBubbleColor), RoundedCornerShape(style.bubbleRadius.coerceAtMost(10).dp))
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .width(30.dp)
                            .height(20.dp)
                            .background(Color(style.myBubbleColor), RoundedCornerShape(style.bubbleRadius.coerceAtMost(10).dp))
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    style.name,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (selected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon()
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ColorField(label: String, value: Long, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            Util.argbColor(value),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(32.dp)
                .background(Color(value), RoundedCornerShape(10.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
        )
    }
}

@Composable
private fun SliderField(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(110.dp))
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f))
        Text(value.toInt().toString(), Modifier.width(30.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun SwitchField(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}
