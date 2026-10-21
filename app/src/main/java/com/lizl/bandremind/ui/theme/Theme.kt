package com.lizl.bandremind.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 品牌主色阶：薄荷 → 湖绿 → 深松。 */
val BrandMint = Color(0xFF5FE3CB)
val BrandTeal = Color(0xFF12B5A0)
val BrandTealDeep = Color(0xFF0A7A6C)
val BrandInk = Color(0xFF06302B)

/** 规则卡片用的点缀色，按规则 id 稳定分配。 */
val AccentPalette = listOf(
    Color(0xFF12B5A0),
    Color(0xFF5B8DEF),
    Color(0xFFFF8A65),
    Color(0xFFA78BFA),
    Color(0xFFFFB74D),
    Color(0xFF3FC9A6),
    Color(0xFFF06292),
)

/** 状态色：正常 / 待处理。 */
val StatusOk = Color(0xFF16A34A)
val StatusWarn = Color(0xFFE8912A)

private val LightColors = lightColorScheme(
    primary = BrandTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB6F2E7),
    onPrimaryContainer = BrandInk,
    inversePrimary = BrandMint,

    secondary = Color(0xFF3D6B64),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC5EDE4),
    onSecondaryContainer = Color(0xFF04201C),

    tertiary = Color(0xFF4A5FA8),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDDE1FF),
    onTertiaryContainer = Color(0xFF00105C),

    background = Color(0xFFF4F9F7),
    onBackground = Color(0xFF101917),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF101917),
    surfaceVariant = Color(0xFFE8F1EE),
    onSurfaceVariant = Color(0xFF4A5B57),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFDFC),
    surfaceContainer = Color(0xFFF2F7F5),
    surfaceContainerHigh = Color(0xFFECF3F0),
    surfaceContainerHighest = Color(0xFFE6EEEB),

    outline = Color(0xFFB9C8C4),
    outlineVariant = Color(0xFFDCE6E3),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    scrim = Color(0xFF000000),
)

private val DarkColors = darkColorScheme(
    primary = BrandMint,
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005044),
    onPrimaryContainer = Color(0xFF8FF7E3),
    inversePrimary = BrandTealDeep,

    secondary = Color(0xFFA9D2C8),
    onSecondary = Color(0xFF11352F),
    secondaryContainer = Color(0xFF244C46),
    onSecondaryContainer = Color(0xFFC5EDE4),

    tertiary = Color(0xFFB9C4FF),
    onTertiary = Color(0xFF1B2C78),
    tertiaryContainer = Color(0xFF324390),
    onTertiaryContainer = Color(0xFFDDE1FF),

    background = Color(0xFF080D0C),
    onBackground = Color(0xFFDEE4E1),
    surface = Color(0xFF0C1311),
    onSurface = Color(0xFFDEE4E1),
    surfaceVariant = Color(0xFF3F4946),
    onSurfaceVariant = Color(0xFFBFC9C5),
    surfaceContainerLowest = Color(0xFF050A09),
    surfaceContainerLow = Color(0xFF101816),
    surfaceContainer = Color(0xFF141C1A),
    surfaceContainerHigh = Color(0xFF1E2624),
    surfaceContainerHighest = Color(0xFF293130),

    outline = Color(0xFF89938F),
    outlineVariant = Color(0xFF3F4946),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color(0xFF000000),
)

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.6).sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 27.sp,
        lineHeight = 33.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp,
    ),
    headlineSmall = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontSize = 19.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.1).sp,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = TextStyle(
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelLarge = TextStyle(
        fontSize = 13.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp,
    ),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun BandRemindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
