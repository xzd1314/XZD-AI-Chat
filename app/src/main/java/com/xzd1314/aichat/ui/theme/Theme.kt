package com.xzd1314.aichat.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ===== 高级配色：深墨绿 + 古铜金 + 暖米白（奢华复古风）=====

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B4332),
    onPrimary = Color(0xFFF5F0E8),
    primaryContainer = Color(0xFFB7D8C4),
    onPrimaryContainer = Color(0xFF0D2818),
    secondary = Color(0xFFB8975A),
    onSecondary = Color(0xFF2D1F0E),
    secondaryContainer = Color(0xFFE8D9B8),
    onSecondaryContainer = Color(0xFF3D2E10),
    tertiary = Color(0xFF8B5A3C),
    onTertiary = Color(0xFFF5F0E8),
    background = Color(0xFFFAF6EE),
    onBackground = Color(0xFF1A2E22),
    surface = Color(0xFFFFFBF2),
    onSurface = Color(0xFF1A2E22),
    surfaceVariant = Color(0xFFE8E4D6),
    onSurfaceVariant = Color(0xFF5A6B5E),
    outline = Color(0xFFC9BFA8),
    outlineVariant = Color(0xFFD9D2BE),
    surfaceContainer = Color(0xFFF0EBDD),
    surfaceContainerLow = Color(0xFFF5F0E4),
    surfaceContainerHigh = Color(0xFFE8E2D0),
    surfaceContainerHighest = Color(0xFFDFD8C4),
    error = Color(0xFF8B2635),
    onError = Color(0xFFF5F0E8),
    errorContainer = Color(0xFFF5D5D8),
    onErrorContainer = Color(0xFF3D0A12)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC9A961),
    onPrimary = Color(0xFF0D2818),
    primaryContainer = Color(0xFF1B4332),
    onPrimaryContainer = Color(0xFFE8D9B8),
    secondary = Color(0xFFD4B87A),
    onSecondary = Color(0xFF2D1F0E),
    secondaryContainer = Color(0xFF3D2E10),
    onSecondaryContainer = Color(0xFFE8D9B8),
    tertiary = Color(0xFFA86B4A),
    onTertiary = Color(0xFF2D1F0E),
    background = Color(0xFF0A1A12),
    onBackground = Color(0xFFE8E0CC),
    surface = Color(0xFF0F2419),
    onSurface = Color(0xFFE8E0CC),
    surfaceVariant = Color(0xFF1A3326),
    onSurfaceVariant = Color(0xFF9AA898),
    outline = Color(0xFF4A4030),
    outlineVariant = Color(0xFF3A3528),
    surfaceContainer = Color(0xFF132B1F),
    surfaceContainerLow = Color(0xFF10261B),
    surfaceContainerHigh = Color(0xFF183326),
    surfaceContainerHighest = Color(0xFF1D3B2C),
    error = Color(0xFFE57373),
    onError = Color(0xFF2D0A12),
    errorContainer = Color(0xFF5C1A22),
    onErrorContainer = Color(0xFFF5D5D8)
)

@Composable
fun XZDChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
