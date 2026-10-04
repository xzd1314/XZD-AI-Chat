package com.xzd1314.aichat.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xzd1314.aichat.R
import com.xzd1314.aichat.util.Util

/** 圆形头像：优先加载用户图片 URI，否则显示默认 AI 角色头像 */
@Composable
fun CircularAvatar(
    uri: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    borderColor: Color = Color.Transparent,
    borderWidth: Dp = 0.dp
) {
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    LaunchedEffect(uri) {
        bitmap = if (uri.isNullOrBlank()) null else Util.loadBitmap(context, uri)
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(if (borderWidth > 0.dp) Modifier.border(borderWidth, borderColor, CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(size).clip(CircleShape)
            )
        } else {
            // 默认 AI 角色头像（抠图透明底）
            Image(
                painter = painterResource(R.drawable.avatar_ai),
                contentDescription = null,
                modifier = Modifier.size(size).clip(CircleShape)
            )
        }
    }
}
