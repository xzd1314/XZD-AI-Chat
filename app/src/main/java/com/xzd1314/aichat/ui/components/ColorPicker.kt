package com.xzd1314.aichat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xzd1314.aichat.util.Util

/** 预设色板 + RGB 微调的颜色选择器对话框 */
@Composable
fun ColorPickerDialog(
    title: String,
    initial: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var color by remember { mutableStateOf(initial) }
    val palette = listOf(
        0xFF000000, 0xFF444444, 0xFF888888, 0xFFBBBBBB, 0xFFFFFFFF,
        0xFFF44336, 0xFFFF9800, 0xFFFFEB3B, 0xFF4CAF50, 0xFF00BCD4,
        0xFF2196F3, 0xFF3F51B5, 0xFF9C27B0, 0xFFE91E63, 0xFF795548, 0xFF607D8B,
        0xFF1E6FE8, 0xFFA78BFA, 0xFFFFD1DC, 0xFFD4AF37, 0xFF00FF41, 0xFFFF00E5
    )
    var r by remember { mutableFloatStateOf(((color shr 16) and 0xFF).toFloat()) }
    var g by remember { mutableFloatStateOf(((color shr 8) and 0xFF).toFloat()) }
    var b by remember { mutableFloatStateOf((color and 0xFF).toFloat()) }

    fun update() {
        color = 0xFF000000 or
            (r.toInt().coerceIn(0, 255).toLong() shl 16) or
            (g.toInt().coerceIn(0, 255).toLong() shl 8) or
            (b.toInt().coerceIn(0, 255).toLong())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .background(Color(color), RoundedCornerShape(12.dp))
                    )
                    Text(
                        Util.argbColor(color),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(palette.size) { i ->
                        val c = palette[i]
                        Box(
                            Modifier
                                .size(36.dp)
                                .background(Color(c), CircleShape)
                                .clickable {
                                    color = c
                                    r = ((c shr 16) and 0xFF).toFloat()
                                    g = ((c shr 8) and 0xFF).toFloat()
                                    b = (c and 0xFF).toFloat()
                                }
                        )
                    }
                }
                ChannelSlider("R", r) { r = it; update() }
                ChannelSlider("G", g) { g = it; update() }
                ChannelSlider("B", b) { b = it; update() }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(color) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun ChannelSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(20.dp), textAlign = TextAlign.Center)
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f)
        )
        Text(value.toInt().toString(), Modifier.width(28.dp), textAlign = TextAlign.End)
    }
}
