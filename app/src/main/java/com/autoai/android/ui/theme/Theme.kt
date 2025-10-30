package com.autoai.android.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// 柔和配色方案 - 温暖舒适
private val SoftPink = Color(0xFFFF9AA2) // 柔和粉色 - 主色调
private val LavenderPurple = Color(0xFFB5A8D6) // 薰衣草紫 - 辅助色
private val MintGreen = Color(0xFF98D8C8) // 薄荷绿 - 成功色

private val SoftPeach = Color(0xFFFFB7B2) // 柔和蜜桃色
private val SoftYellow = Color(0xFFFFC8A2) // 柔和黄色

// 浅色模式配色 - 柔和温暖
private val LightColorScheme = lightColorScheme(
    primary = SoftPink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE5E9), // 极浅粉色
    onPrimaryContainer = Color(0xFF5C3A3E),
    
    secondary = LavenderPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0EBFF), // 极浅薰衣草色
    onSecondaryContainer = Color(0xFF3D3650),
    
    tertiary = MintGreen,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F7F1), // 极浅薄荷色
    onTertiaryContainer = Color(0xFF2D4A44),
    
    error = SoftPeach,
    onError = Color.White,
    errorContainer = Color(0xFFFFEDEB),
    onErrorContainer = Color(0xFF5C3630),
    
    background = Color(0xFFFFFBF8), // 温暖的米白色
    onBackground = Color(0xFF4A4545),
    
    surface = Color(0xFFFFFEFD),
    onSurface = Color(0xFF4A4545),
    surfaceVariant = Color(0xFFF5F0F0),
    onSurfaceVariant = Color(0xFF7A7575),
    
    outline = Color(0xFFE0D5D5),
    outlineVariant = Color(0xFFF0E8E8)
)

// 深色模式配色 - 柔和舒适
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB4BA), // 柔和亮粉
    onPrimary = Color(0xFF5C3A3E),
    primaryContainer = Color(0xFF8B5A60),
    onPrimaryContainer = Color(0xFFFFE5E9),
    
    secondary = Color(0xFFC5BAED), // 柔和亮紫
    onSecondary = Color(0xFF3D3650),
    secondaryContainer = Color(0xFF635B7A),
    onSecondaryContainer = Color(0xFFF0EBFF),
    
    tertiary = Color(0xFFAAE3D7), // 柔和亮绿
    onTertiary = Color(0xFF2D4A44),
    tertiaryContainer = Color(0xFF4A6B65),
    onTertiaryContainer = Color(0xFFE0F7F1),
    
    error = Color(0xFFFFCAC5),
    onError = Color(0xFF5C3630),
    errorContainer = Color(0xFF8B584F),
    onErrorContainer = Color(0xFFFFEDEB),
    
    background = Color(0xFF2A2525), // 温暖的深色
    onBackground = Color(0xFFF5F0F0),
    
    surface = Color(0xFF3A3535),
    onSurface = Color(0xFFF5F0F0),
    surfaceVariant = Color(0xFF4A4545),
    onSurfaceVariant = Color(0xFFD5CECE),
    
    outline = Color(0xFF6A6060),
    outlineVariant = Color(0xFF4A4545)
)

// 自定义圆角样式
private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
)

// 自定义排版
private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * AutoAI 应用主题
 * 
 * @param darkTheme 是否使用深色模式，默认跟随系统
 * @param dynamicColor 是否使用动态配色（Android 12+），默认关闭以保持一致性
 * @param content 主题内容
 */
@Composable
fun AutoAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
            val context = LocalView.current.context
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 使用透明状态栏以获得沉浸式体验
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = colorScheme.surface.toArgb()
            
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
