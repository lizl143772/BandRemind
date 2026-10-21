package com.lizl.bandremind.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.lizl.bandremind.notify.ReminderNotifier
import com.lizl.bandremind.schedule.ReminderScheduler
import com.lizl.bandremind.ui.theme.BrandTeal
import com.lizl.bandremind.ui.theme.StatusOk
import com.lizl.bandremind.ui.theme.StatusWarn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scheduler = remember { ReminderScheduler(context) }

    var notificationGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var exactAlarmGranted by remember { mutableStateOf(scheduler.canScheduleExact()) }
    var batteryOptimized by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    var message by remember { mutableStateOf<String?>(null) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationGranted = granted
        message = if (granted) "通知权限已开启" else "未授予通知权限，提醒无法弹出"
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "设置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { innerPadding ->
        AuroraBackground {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 手表同步说明
                GradientCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(22.dp)) {
                        IconBubble(
                            icon = Icons.Rounded.Watch,
                            tint = Color.White,
                            size = 44.dp,
                            iconSize = 22.dp,
                            backgroundAlpha = 0.18f,
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "同步到 Redmi Watch 5/6",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "小米运动健康 → 设备 → 通知与通话 → App 通知 → 打开「同步所有通知」",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.16f))
                                .clickable {
                                    val intent = MI_FITNESS_PACKAGES
                                        .asSequence()
                                        .mapNotNull {
                                            context.packageManager.getLaunchIntentForPackage(it)
                                        }
                                        .firstOrNull()
                                    if (intent != null) {
                                        context.startActivity(intent)
                                    } else {
                                        message = "没找到小米运动健康，请手动打开并完成上述设置"
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "打开小米运动健康",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                StatusRow(
                    icon = Icons.Rounded.Notifications,
                    title = "通知权限",
                    detail = if (notificationGranted) {
                        "已授予，提醒可以正常弹出"
                    } else {
                        "未授予，Android 13+ 需要手动授权"
                    },
                    ok = notificationGranted,
                    actionLabel = if (notificationGranted) "已开启" else "去授权",
                    actionEnabled = !notificationGranted,
                    onAction = {
                        if (!notificationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )

                StatusRow(
                    icon = Icons.Rounded.Schedule,
                    title = "精确闹钟",
                    detail = if (exactAlarmGranted) {
                        "已允许，提醒会准点触发"
                    } else {
                        "未允许时只能粗略触发，可能延迟几分钟"
                    },
                    ok = exactAlarmGranted,
                    actionLabel = if (exactAlarmGranted) "已允许" else "去允许",
                    actionEnabled = !exactAlarmGranted,
                    onAction = {
                        runCatching { context.startActivity(scheduler.exactAlarmSettingsIntent()) }
                            .onFailure { message = "无法打开系统设置" }
                    },
                )

                StatusRow(
                    icon = Icons.Rounded.BatterySaver,
                    title = "电池优化",
                    detail = if (batteryOptimized) {
                        "已关闭本应用的电池优化，Doze 下仍能准时提醒"
                    } else {
                        "系统可能压制后台闹钟，导致提醒延迟"
                    },
                    ok = batteryOptimized,
                    actionLabel = if (batteryOptimized) "已优化" else "去设置",
                    actionEnabled = !batteryOptimized,
                    onAction = { openBatteryOptimizationSettings(context) },
                )

                // 测试推送
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBubble(
                                icon = Icons.Rounded.Send,
                                tint = BrandTeal,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "测试推送",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "发一条「喝水」测试通知，确认手表能收到并震动",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        GradientButton(
                            text = "发送测试提醒",
                            icon = Icons.Rounded.Send,
                            onClick = {
                                ReminderNotifier.postTest(context)
                                message = "已发送测试通知，看手表是否震动"
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                if (message != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    ) {
                        Text(
                            text = message.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }

                // 工作原理
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBubble(
                                icon = Icons.Rounded.Info,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                backgroundAlpha = 0.12f,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "工作原理",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "提醒的排程全部在手机端完成（AlarmManager 精确闹钟）。到点后手机弹出一条" +
                                "高优先级通知，再由小米运动健康镜像到 Redmi Watch 5/6，手表震动并显示标题与内容。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        BulletLine("手机需要与手表保持蓝牙连接")
                        BulletLine("手表勿扰模式开启时不会震动")
                        BulletLine("手表最多保留最近 10 条消息")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusRow(
    icon: ImageVector,
    title: String,
    detail: String,
    ok: Boolean,
    actionLabel: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = actionEnabled, onClick = onAction),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(
                    icon = icon,
                    tint = if (ok) StatusOk else StatusWarn,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(10.dp))
                StatusPill(text = actionLabel, ok = ok)
            }
            if (actionEnabled) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun BulletLine(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 小米运动健康的包名在不同 ROM 上不一样：
 * 新版 HyperOS 上是 [com.mi.health]，老版本是 [com.xiaomi.hm]。
 */
private val MI_FITNESS_PACKAGES = listOf("com.mi.health", "com.xiaomi.hm")

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(PowerManager::class.java) ?: return false
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun openBatteryOptimizationSettings(context: Context) {
    val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        .setData(Uri.parse("package:${context.packageName}"))
    val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    runCatching { context.startActivity(requestIntent) }
        .recoverCatching { context.startActivity(fallbackIntent) }
}
