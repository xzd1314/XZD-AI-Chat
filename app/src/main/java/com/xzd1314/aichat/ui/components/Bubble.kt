package com.xzd1314.aichat.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xzd1314.aichat.data.ChatStyle
import com.xzd1314.aichat.util.Util

/** 聊天消息气泡（完全由 ChatStyle 驱动，支持思考过程可折叠 + 进入动画） */
@Composable
fun ChatBubble(
    isMine: Boolean,
    text: String,
    time: Long,
    style: ChatStyle,
    avatarUri: String?,
    reasoning: String? = null,
    imageUri: String? = null,
    modifier: Modifier = Modifier
) {
    // 进入动画：淡入 + 上移
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(350), label = "bubble_alpha"
    )
    val transY by animateFloatAsState(
        targetValue = if (visible) 0f else 18f,
        animationSpec = tween(350), label = "bubble_trans"
    )

    // 图片消息加载
    val context = LocalContext.current
    var imgBitmap by remember(imageUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(imageUri) {
        imgBitmap = if (imageUri.isNullOrBlank()) null else Util.loadBitmap(context, imageUri)
    }

    val bubbleColor = if (isMine) Color(style.myBubbleColor) else Color(style.aiBubbleColor)
    val textColor = if (isMine) Color(style.myTextColor) else Color(style.aiTextColor)
    val radius = style.bubbleRadius.dp
    val shape = if (isMine)
        RoundedCornerShape(radius, radius, 4.dp, radius)
    else
        RoundedCornerShape(radius, radius, radius, 4.dp)

    var reasoningExpanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (reasoningExpanded) 180f else 0f,
        animationSpec = tween(250), label = "arrow_rot"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .graphicsLayer(alpha = alpha, translationY = transY),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isMine && style.showAvatar) {
            CircularAvatar(avatarUri, 34.dp, Modifier.padding(end = 8.dp))
        }
        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
            if (style.showNameLabel && !isMine) {
                Text(
                    "小蓝",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }
            // 思考过程块（可折叠）
            if (!isMine && !reasoning.isNullOrBlank()) {
                Box(
                    Modifier
                        .widthIn(max = (style.bubbleMaxWidthPct * 3.6).dp)
                        .background(
                            if (style.isDark) Color(0xFF1A3326) else Color(0xFFE8E4D6),
                            RoundedCornerShape(12.dp, 12.dp, 4.dp, 12.dp)
                        )
                        .clickable { reasoningExpanded = !reasoningExpanded }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Psychology,
                                contentDescription = null,
                                tint = if (style.isDark) Color(0xFFC9A961) else Color(0xFF8B5A3C),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "思考过程",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (style.isDark) Color(0xFFC9A961) else Color(0xFF8B5A3C),
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                            Spacer(Modifier.weight(1f))
                            Icon(
                                Icons.Filled.ExpandMore,
                                contentDescription = null,
                                tint = if (style.isDark) Color(0xFFC9A961) else Color(0xFF8B5A3C),
                                modifier = Modifier.size(16.dp).rotate(arrowRotation)
                            )
                        }
                        androidx.compose.animation.AnimatedVisibility(visible = reasoningExpanded) {
                            Column(Modifier.padding(top = 6.dp)) {
                                Text(
                                    reasoning,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (style.isDark) Color(0xFF9AA898) else Color(0xFF5A6B5E),
                                    fontStyle = FontStyle.Italic,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            // 正文气泡
            Box(
                Modifier
                    .widthIn(max = (style.bubbleMaxWidthPct * 3.6).dp)
                    .background(bubbleColor, shape)
                    .padding(if (imgBitmap != null) 4.dp else 12.dp, if (imgBitmap != null) 4.dp else 9.dp)
            ) {
                Column {
                    if (imgBitmap != null) {
                        Image(
                            bitmap = imgBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        if (text.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text,
                                color = textColor,
                                fontSize = style.fontSize.sp,
                                lineHeight = (style.fontSize * 1.45).sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Text(
                            text,
                            color = textColor,
                            fontSize = style.fontSize.sp,
                            lineHeight = (style.fontSize * 1.45).sp
                        )
                    }
                }
            }
            if (style.showTime) {
                Text(
                    Util.formatClock(time),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                    textAlign = if (isMine) TextAlign.End else TextAlign.Start
                )
            }
        }
        if (isMine && style.showAvatar) {
            CircularAvatar(null, 34.dp, Modifier.padding(start = 8.dp))
        }
    }
}

/** 风格实时预览：模拟两条消息 */
@Composable
fun StylePreview(style: ChatStyle, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color(style.bgColor))
            .padding(vertical = 8.dp)
    ) {
        ChatBubble(false, "你好！我是小蓝，今天想聊点什么？", System.currentTimeMillis() - 300_000, style, null)
        ChatBubble(true, "帮我写一份周报吧～", System.currentTimeMillis() - 200_000, style, null)
        ChatBubble(false, "好的，这就来！", System.currentTimeMillis() - 100_000, style, null)
    }
}
