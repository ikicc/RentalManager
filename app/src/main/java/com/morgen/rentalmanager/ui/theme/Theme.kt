package com.morgen.rentalmanager.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val HyperOSLightColorScheme = lightColorScheme(
    primary = VitalityBlue,
    onPrimary = PureWhite,
    primaryContainer = SkyBlue,
    onPrimaryContainer = PureWhite,
    
    secondary = LightGrass,
    onSecondary = PureWhite,
    secondaryContainer = Color(0xFFE8F5E8),
    onSecondaryContainer = DeepGray,
    
    tertiary = SkyBlue,
    onTertiary = PureWhite,
    tertiaryContainer = Color(0xFFE3F2FD),
    onTertiaryContainer = DeepGray,
    
    error = ErrorRed,
    onError = PureWhite,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFB71C1C),
    
    background = PureWhite, // 改用纯白色背景，去掉黄色调
    onBackground = DeepGray,
    surface = PureWhite, // 改用纯白色卡片，去掉蓝色调
    onSurface = DeepGray,
    surfaceVariant = LightGray,
    onSurfaceVariant = SoftGray,
    
    outline = BorderGray,
    outlineVariant = Color(0xFFF0F0F0),
    scrim = Color.Black.copy(alpha = 0.25f),
    
    inverseSurface = DeepGray,
    inverseOnSurface = PureWhite,
    inversePrimary = SkyBlue,
    
    surfaceDim = LightGray,
    surfaceBright = PureWhite,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = PureWhite, // 改用纯白色，去掉米色调
    surfaceContainer = LightGray,
    surfaceContainerHigh = Color(0xFFEEEEEE),
    surfaceContainerHighest = BorderGray
)

// 完整的深色主题配色方案
private val HyperOSDarkColorScheme = darkColorScheme(
    primary = VitalityBlue,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1557B0),
    onPrimaryContainer = Color.White,
    
    secondary = LightGrass,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2E7D32),
    onSecondaryContainer = Color.White,
    
    tertiary = SkyBlue,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF1976D2),
    onTertiaryContainer = Color.White,
    
    error = Color(0xFFFF6B6B),
    onError = Color.Black,
    errorContainer = Color(0xFFD32F2F),
    onErrorContainer = Color.White,
    
    background = Color(0xFF121212),        // 深色背景
    onBackground = Color(0xFFE0E0E0),      // 浅色文字
    surface = Color(0xFF1E1E1E),           // 深色表面
    onSurface = Color(0xFFE0E0E0),         // 浅色文字
    surfaceVariant = Color(0xFF2C2C2C),    // 深色变体表面
    onSurfaceVariant = Color(0xFFB0B0B0),  // 中等浅色文字
    
    outline = Color(0xFF404040),           // 深色边框
    outlineVariant = Color(0xFF303030),    // 深色边框变体
    scrim = Color.Black.copy(alpha = 0.5f),
    
    inverseSurface = Color(0xFFE0E0E0),
    inverseOnSurface = Color(0xFF121212),
    inversePrimary = Color(0xFF1A73E8),
    
    surfaceDim = Color(0xFF0F0F0F),
    surfaceBright = Color(0xFF2C2C2C),
    surfaceContainerLowest = Color(0xFF0A0A0A),
    surfaceContainerLow = Color(0xFF1A1A1A),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF252525),
    surfaceContainerHighest = Color(0xFF2C2C2C)
)

@Composable
fun MyApplicationTheme(
        darkTheme: Boolean = isSystemInDarkTheme(), // 自动跟随系统深色模式
        // 禁用动态颜色，使用我们自定义的HyperOS配色
        dynamicColor: Boolean = false,
        // 添加轻量级主题选项
        useLightOptimizedTheme: Boolean = false,
        content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> HyperOSDarkColorScheme
        else -> HyperOSLightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 使用透明状态栏，更符合现代设计
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
    )
} 