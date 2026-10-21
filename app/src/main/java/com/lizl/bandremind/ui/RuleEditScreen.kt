package com.lizl.bandremind.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lizl.bandremind.data.Reminder
import com.lizl.bandremind.data.RuleType
import com.lizl.bandremind.data.TimeRange
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

private val dayLabels = mapOf(
    DayOfWeek.MONDAY to "一",
    DayOfWeek.TUESDAY to "二",
    DayOfWeek.WEDNESDAY to "三",
    DayOfWeek.THURSDAY to "四",
    DayOfWeek.FRIDAY to "五",
    DayOfWeek.SATURDAY to "六",
    DayOfWeek.SUNDAY to "日",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RuleEditScreen(
    rule: Reminder?,
    preset: Reminder?,
    onSave: (Reminder) -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit,
) {
    // 编辑已有规则用 rule，从模板新建用 preset，都没有就是空白
    val initial = rule ?: preset
    val stateKey = rule?.id
        ?: preset?.let { "preset:${it.title}:${it.intervalMinutes}:${it.windowStart}:${it.windowEnd}" }
        ?: "blank"

    var title by remember(stateKey) { mutableStateOf(initial?.title.orEmpty()) }
    var body by remember(stateKey) { mutableStateOf(initial?.body.orEmpty()) }
    var type by remember(stateKey) { mutableStateOf(initial?.type ?: RuleType.INTERVAL) }
    var intervalText by remember(stateKey) { mutableStateOf((initial?.intervalMinutes ?: 45).toString()) }
    var times by remember(stateKey) { mutableStateOf(initial?.sortedTimes ?: listOf(LocalTime.of(9, 0))) }
    var days by remember(stateKey) { mutableStateOf(initial?.daysOfWeek ?: DayOfWeek.entries.toSet()) }
    var windowStart by remember(stateKey) { mutableStateOf(initial?.windowStart ?: LocalTime.of(9, 0)) }
    var windowEnd by remember(stateKey) { mutableStateOf(initial?.windowEnd ?: LocalTime.of(21, 0)) }
    var pausedWindows by remember(stateKey) { mutableStateOf(initial?.pausedWindows ?: emptyList()) }
    var snoozeText by remember(stateKey) { mutableStateOf((initial?.snoozeMinutes ?: 10).toString()) }
    var enabled by remember(stateKey) { mutableStateOf(initial?.enabled ?: true) }

    val intervalMinutes = intervalText.toIntOrNull()?.coerceIn(1, 24 * 60) ?: 45
    val snoozeMinutes = snoozeText.toIntOrNull()?.coerceIn(1, 120) ?: 10
    val canSave = title.isNotBlank()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (rule == null) "新建提醒" else "编辑提醒",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    GradientButton(
                        text = "保存",
                        compact = true,
                        onClick = {
                            onSave(
                                Reminder(
                                    id = rule?.id ?: UUID.randomUUID().toString(),
                                    title = title.trim(),
                                    body = body.trim(),
                                    type = type,
                                    intervalMinutes = intervalMinutes,
                                    times = if (type == RuleType.DAILY_TIMES) times.sorted() else emptyList(),
                                    daysOfWeek = days.ifEmpty { DayOfWeek.entries.toSet() },
                                    windowStart = windowStart,
                                    windowEnd = windowEnd,
                                    pausedWindows = pausedWindows.filter { it.isValid },
                                    enabled = enabled,
                                    snoozeMinutes = snoozeMinutes,
                                    createdAt = rule?.createdAt ?: System.currentTimeMillis(),
                                ),
                            )
                        },
                        enabled = canSave,
                        modifier = Modifier.padding(end = 12.dp),
                    )
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
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SectionCard {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("提醒标题") },
                        placeholder = { Text("例如：喝水 / 吃药") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("提醒内容（可选）") },
                        placeholder = { Text("会显示在手表通知正文里") },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                SectionCard(title = "提醒方式") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TypeOption(
                            icon = Icons.Rounded.Timelapse,
                            title = "间隔提醒",
                            subtitle = "每隔 N 分钟",
                            selected = type == RuleType.INTERVAL,
                            modifier = Modifier.weight(1f),
                            onClick = { type = RuleType.INTERVAL },
                        )
                        TypeOption(
                            icon = Icons.Rounded.Today,
                            title = "定时提醒",
                            subtitle = "每天固定时点",
                            selected = type == RuleType.DAILY_TIMES,
                            modifier = Modifier.weight(1f),
                            onClick = { type = RuleType.DAILY_TIMES },
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (type == RuleType.INTERVAL) {
                            "在时间段内每隔固定时间提醒一次，例如每 45 分钟喝一次水。"
                        } else {
                            "每天在固定的几个时间点提醒，例如早中晚三次吃药。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (type == RuleType.INTERVAL) {
                    SectionCard(title = "间隔设置") {
                        OutlinedTextField(
                            value = intervalText,
                            onValueChange = { intervalText = it.filter(Char::isDigit) },
                            label = { Text("间隔（分钟）") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TimeField(
                                label = "起始",
                                time = windowStart,
                                modifier = Modifier.weight(1f),
                                onTimeChange = { windowStart = it },
                            )
                            TimeField(
                                label = "结束",
                                time = windowEnd,
                                modifier = Modifier.weight(1f),
                                onTimeChange = { windowEnd = it },
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "只在 ${windowStart} – ${windowEnd} 之间提醒，间隔从起始时间开始对齐。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    SectionCard(title = "免打扰时段") {
                        if (pausedWindows.isEmpty()) {
                            Text(
                                text = "还没有免打扰时段。加上之后，这段时间里不会提醒（例如午休）。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))
                        }

                        pausedWindows.forEachIndexed { index, range ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TimeField(
                                    label = "从",
                                    time = range.start,
                                    modifier = Modifier.weight(1f),
                                    onTimeChange = { updated ->
                                        pausedWindows = pausedWindows.toMutableList()
                                            .apply { set(index, range.copy(start = updated)) }
                                    },
                                )
                                TimeField(
                                    label = "到",
                                    time = range.end,
                                    modifier = Modifier.weight(1f),
                                    onTimeChange = { updated ->
                                        pausedWindows = pausedWindows.toMutableList()
                                            .apply { set(index, range.copy(end = updated)) }
                                    },
                                )
                                IconButton(onClick = {
                                    pausedWindows = pausedWindows.toMutableList().apply { removeAt(index) }
                                }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "删除免打扰时段",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        AddChip(
                            text = "添加免打扰时段",
                            onClick = {
                                pausedWindows = pausedWindows + TimeRange(
                                    LocalTime.of(12, 0),
                                    LocalTime.of(13, 30),
                                )
                            },
                        )

                        if (pausedWindows.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "提醒会在免打扰时段结束后重新开始对齐，" +
                                    "例如午休 12:00–13:30 结束后第一次提醒是 13:30。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    SectionCard(title = "提醒时点") {
                        times.forEachIndexed { index, time ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                TimeField(
                                    label = "第 ${index + 1} 次",
                                    time = time,
                                    modifier = Modifier.weight(1f),
                                    onTimeChange = { updated ->
                                        times = times.toMutableList().apply { set(index, updated) }
                                    },
                                )
                                if (times.size > 1) {
                                    IconButton(onClick = {
                                        times = times.toMutableList().apply { removeAt(index) }
                                    }) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = "删除时点",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        AddChip(text = "添加时点", onClick = { times = times + LocalTime.of(12, 0) })
                    }
                }

                SectionCard(title = "生效星期") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DayOfWeek.entries.forEach { day ->
                            val selected = days.contains(day)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        },
                                    )
                                    .clickable {
                                        days = if (selected) days - day else days + day
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = dayLabels[day].orEmpty(),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (days.size == 7) "每天生效" else "仅选中的星期生效",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                SectionCard(title = "其他") {
                    OutlinedTextField(
                        value = snoozeText,
                        onValueChange = { snoozeText = it.filter(Char::isDigit) },
                        label = { Text("稍后提醒（分钟）") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("启用该提醒", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "暂停后不再排程，也不会推送通知",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }
                }

                if (rule != null && onDelete != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onDelete),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 15.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "删除这条规则",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Column(Modifier.padding(18.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
private fun TypeOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (selected) {
                    Modifier.border(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(16.dp),
                    )
                } else {
                    Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(16.dp),
                    )
                },
            )
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                } else {
                    Color.Transparent
                },
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
    ) {
        Column(Modifier.padding(vertical = 14.dp, horizontal = 12.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AddChip(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
