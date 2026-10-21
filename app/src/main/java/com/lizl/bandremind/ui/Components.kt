package com.lizl.bandremind.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lizl.bandremind.ui.theme.AccentPalette
import com.lizl.bandremind.ui.theme.BrandMint
import com.lizl.bandremind.ui.theme.BrandTeal
import com.lizl.bandremind.ui.theme.BrandTealDeep
import com.lizl.bandremind.ui.theme.StatusOk
import com.lizl.bandremind.ui.theme.StatusWarn
import android.app.TimePickerDialog
import android.text.format.DateFormat
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

/** 每秒更新一次的当前时间，用于倒计时展示。 */
@Composable
fun rememberNow(tickMillis: Long = 1_000): State<LocalDateTime> =
    produceState(LocalDateTime.now(), tickMillis) {
        while (true) {
            value = LocalDateTime.now()
            delay(tickMillis)
        }
    }

/** 规则 id → 稳定的点缀色。 */
fun accentColorOf(id: String): Color {
    val hash = id.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
    return AccentPalette[hash % AccentPalette.size]
}

/** 页面背景：顶部一层柔和的品牌色辉光，向下过渡到纯背景色。 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to scheme.primary.copy(alpha = if (dark) 0.14f else 0.10f),
                    0.28f to scheme.background,
                    1f to scheme.background,
                ),
            ),
    ) {
        Box(
            Modifier
                .size(340.dp)
                .offset(x = 170.dp, y = (-150).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(scheme.primary.copy(alpha = if (dark) 0.22f else 0.16f), Color.Transparent),
                    ),
                ),
        )
        Box(
            Modifier
                .size(260.dp)
                .offset(x = (-90).dp, y = (-60).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(scheme.tertiary.copy(alpha = if (dark) 0.16f else 0.10f), Color.Transparent),
                    ),
                ),
        )
        content()
    }
}

/** 品牌渐变表面：主色卡、FAB、按钮共用一套渐变。 */
@Composable
fun brandBrush(): Brush = Brush.linearGradient(
    colors = listOf(BrandMint, BrandTeal, BrandTealDeep),
    start = Offset.Zero,
    end = Offset.Infinite,
)

/** 渐变主卡。 */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(18.dp, shape, clip = false, ambientColor = BrandTealDeep, spotColor = BrandTealDeep)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF17C3AC), BrandTeal, Color(0xFF0A6E63)),
                    start = Offset(0f, 0f),
                    end = Offset(1200f, 1600f),
                ),
            ),
    ) {
        // 右上角高光
        Box(
            Modifier
                .size(220.dp)
                .offset(x = 150.dp, y = (-110).dp)
                .background(
                    Brush.radialGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Transparent)),
                ),
        )
        content()
    }
}

/**
 * 倒计时圆环。
 *
 * @param progress 0f..1f，表示从上一次触发到下一次触发已经走过的比例
 */
@Composable
fun CountdownRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 168.dp,
    strokeWidth: Dp = 12.dp,
    trackColor: Color = Color.White.copy(alpha = 0.22f),
    progressColor: Color = Color.White,
    content: @Composable BoxScope.() -> Unit,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900),
        label = "ringProgress",
    )
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        content()
    }
}

/** 状态胶囊：正常 / 待处理。 */
@Composable
fun StatusPill(
    text: String,
    ok: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (ok) StatusOk else StatusWarn
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color.copy(alpha = 0.14f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color,
            )
        }
    }
}

/** 圆形图标气泡，图标用品牌色，底色用低透明度着色。 */
@Composable
fun IconBubble(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    backgroundAlpha: Float = 0.14f,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = backgroundAlpha)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** 渐变主按钮。[compact] 用于顶栏等空间有限的位置。 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    compact: Boolean = false,
) {
    val shape = RoundedCornerShape(if (compact) 12.dp else 16.dp)
    val height = if (compact) 36.dp else 52.dp
    Row(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(
                if (enabled) {
                    Brush.horizontalGradient(listOf(BrandMint, BrandTeal, BrandTealDeep))
                } else {
                    Brush.horizontalGradient(listOf(Color.LightGray, Color.LightGray))
                },
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (compact) 14.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(if (compact) 15.dp else 18.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** 渐变悬浮按钮。 */
@Composable
fun GradientFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String? = null,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .size(58.dp)
            .shadow(16.dp, shape, clip = false, ambientColor = BrandTealDeep, spotColor = BrandTealDeep)
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(BrandMint, BrandTeal, BrandTealDeep)),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

/**
 * 时间选择字段。
 *
 * 用平台原生对话框而不是 Material3 的 TimePicker：跨版本表现一致，代码也更少。
 */
@Composable
fun TimeField(
    label: String,
    time: LocalTime,
    modifier: Modifier = Modifier,
    onTimeChange: (LocalTime) -> Unit,
) {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val pattern = if (is24Hour) "HH:mm" else "hh:mm a"
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = scheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .clickable {
                    TimePickerDialog(
                        context,
                        { _, hour, minute -> onTimeChange(LocalTime.of(hour, minute)) },
                        time.hour,
                        time.minute,
                        is24Hour,
                    ).show()
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Schedule,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    text = time.format(DateTimeFormatter.ofPattern(pattern)),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                )
            }
        }
    }
}

/** 一行标题 + 可选的操作。 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}

/** 描边小标签。 */
@Composable
fun OutlineTag(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontSize = 10.5.sp,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
        )
    }
}
