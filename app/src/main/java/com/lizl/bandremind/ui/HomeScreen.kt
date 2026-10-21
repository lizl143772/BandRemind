package com.lizl.bandremind.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lizl.bandremind.data.CompletionEvent
import com.lizl.bandremind.data.Reminder
import com.lizl.bandremind.logic.NextTriggerCalculator
import com.lizl.bandremind.logic.TimelinePolicy
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** 今日时间线每一行的测试标记。 */
const val TIMELINE_ROW_TAG = "timeline-row"

/** 「查看全部 / 收起」按钮的测试标记。 */
const val TIMELINE_TOGGLE_TAG = "timeline-toggle"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    rules: List<Reminder>,
    completions: List<CompletionEvent>,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onSettings: () -> Unit,
    onToggle: (Reminder, Boolean) -> Unit,
    onDelete: (Reminder) -> Unit,
    /**
     * 固定「当前时间」，仅供仪器化测试使用；
     * 为 null 时使用系统时间。
     */
    fixedNow: LocalDateTime? = null,
) {
    val systemNow by rememberNow(tickMillis = if (fixedNow == null) 1_000L else Long.MAX_VALUE)
    val now = fixedNow ?: systemNow

    val nextTrigger = remember(rules, now) {
        rules
            .filter { it.enabled }
            .mapNotNull { rule -> NextTriggerCalculator.next(rule, now)?.let { rule to it } }
            .minByOrNull { it.second }
    }

    val todayItems = remember(rules, now) {
        val date = now.toLocalDate()
        rules
            .flatMap { rule ->
                NextTriggerCalculator.todayOccurrences(rule, date, now).map { rule to it }
            }
            .sortedBy { it.second }
    }

    // 默认只展示最近两条，其余折叠
    var timelineExpanded by remember { mutableStateOf(false) }
    val visibleTimeline = remember(todayItems, timelineExpanded) {
        TimelinePolicy.visible(todayItems, timelineExpanded)
    }
    val hiddenCount = remember(todayItems.size, visibleTimeline.size) {
        TimelinePolicy.hiddenCount(todayItems.size, visibleTimeline.size)
    }

    val todayCompletions = remember(completions, now) {
        val startOfDayMillis = now.toLocalDate()
            .atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        completions.count { it.timestamp >= startOfDayMillis }
    }

    val activeRules = rules.count { it.enabled }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "腕上提醒",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "今天也要好好照顾自己",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable(onClick = onSettings),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = "设置",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            GradientFab(
                onClick = onAdd,
                icon = Icons.Rounded.Add,
                contentDescription = "新建提醒",
            )
        },
    ) { innerPadding ->
        AuroraBackground {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    HeroCard(
                        rule = nextTrigger?.first,
                        time = nextTrigger?.second,
                        now = now,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                item {
                    StatStrip(
                        todayCompletions = todayCompletions,
                        activeRules = activeRules,
                        totalRules = rules.size,
                        upcomingCount = todayItems.size,
                    )
                }

                item {
                    SectionHeader(
                        title = "今日提醒",
                        subtitle = if (todayItems.isEmpty()) null else "共 ${todayItems.size} 次",
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                        // 展开/收起固定在标题行，条目再多也不用滚到列表末尾才能收起
                        trailing = if (TimelinePolicy.showToggle(todayItems.size, timelineExpanded)) {
                            {
                                TimelineToggle(
                                    expanded = timelineExpanded,
                                    hiddenCount = hiddenCount,
                                    totalCount = todayItems.size,
                                    onClick = { timelineExpanded = !timelineExpanded },
                                )
                            }
                        } else {
                            null
                        },
                    )
                }

                if (todayItems.isEmpty()) {
                    item { EmptyHint("今天没有安排\n点右下角 + 新建一条规则，喝水、吃药都能定时提醒") }
                } else {
                    itemsIndexed(
                        items = visibleTimeline,
                        key = { index, item -> "${item.first.id}-${item.second}-$index" },
                    ) { index, (rule, time) ->
                        TimelineRow(
                            rule = rule,
                            time = time,
                            isNext = nextTrigger?.second == time,
                            isLast = index == visibleTimeline.lastIndex && !timelineExpanded,
                        )
                    }
                }

                item {
                    SectionHeader(
                        title = "提醒规则",
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                    )
                }

                if (rules.isEmpty()) {
                    item { EmptyHint("还没有任何规则") }
                } else {
                    items(rules, key = { it.id }) { rule ->
                        RuleCard(
                            rule = rule,
                            nextTime = remember(rule, now) { NextTriggerCalculator.next(rule, now) },
                            onToggle = { onToggle(rule, it) },
                            onEdit = { onEdit(rule.id) },
                            onDelete = { onDelete(rule) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    rule: Reminder?,
    time: LocalDateTime?,
    now: LocalDateTime,
    modifier: Modifier = Modifier,
) {
    GradientCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "下一次提醒",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }

            Spacer(Modifier.height(16.dp))

            if (rule == null || time == null) {
                Box(
                    modifier = Modifier
                        .size(168.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "暂无提醒",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "新建规则后会自动排程，并同步到 Redmi Watch",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f),
                )
                return@Column
            }

            val progress = remember(rule, now) { NextTriggerCalculator.progress(rule, now) }

            CountdownRing(progress = progress) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = countdownText(now, time),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = rule.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.92f),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.16f),
                ) {
                    Text(
                        text = time.format(timeFormat),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.10f),
                ) {
                    Text(
                        text = rule.scheduleSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.88f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

/**
 * 「查看全部 / 收起」切换按钮。
 *
 * 放在「今日提醒」标题行右侧：折叠时显示「查看全部 N 条 + 隐藏条数」，
 * 展开后变成「收起」。位置固定，条目再多也不用滚到列表末尾才能收起。
 */
@Composable
private fun TimelineToggle(
    expanded: Boolean,
    hiddenCount: Int,
    totalCount: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(TIMELINE_TOGGLE_TAG)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = if (expanded) {
                "收起"
            } else {
                "查看全部 $totalCount 条"
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
        if (!expanded && hiddenCount > 0) {
            Spacer(Modifier.width(4.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            ) {
                Text(
                    text = "+$hiddenCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun StatStrip(
    todayCompletions: Int,
    activeRules: Int,
    totalRules: Int,
    upcomingCount: Int,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard(
            value = todayCompletions.toString(),
            label = "今日完成",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            value = upcomingCount.toString(),
            label = "今日待提醒",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            value = "$activeRules/$totalRules",
            label = "启用规则",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TimelineRow(
    rule: Reminder,
    time: LocalDateTime,
    isNext: Boolean,
    isLast: Boolean,
) {
    val accent = accentColorOf(rule.id)
    val rail = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .testTag(TIMELINE_ROW_TAG),
    ) {
        Column(
            modifier = Modifier
                .width(26.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .width(2.dp)
                    .height(12.dp)
                    .background(rail),
            )
            Box(
                modifier = Modifier
                    .size(if (isNext) 13.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (isNext) accent else rail),
            )
            if (!isLast) {
                Box(
                    Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(rail),
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            shape = RoundedCornerShape(18.dp),
            color = if (isNext) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
            border = BorderStroke(
                1.dp,
                if (isNext) {
                    accent.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                },
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = time.format(timeFormat),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                    color = if (isNext) accent else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = rule.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (isNext) {
                    OutlineTag("下一次", tint = accent)
                }
            }
        }
    }
}

@Composable
private fun RuleCard(
    rule: Reminder,
    nextTime: LocalDateTime?,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val accent = accentColorOf(rule.id)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = rule.title.ifBlank { "未命名提醒" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = rule.scheduleSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(7.dp))
                OutlineTag(
                    text = when {
                        !rule.enabled -> "已暂停"
                        nextTime != null -> "下次 ${nextTime.format(timeFormat)}"
                        else -> "暂无可触发时间"
                    },
                    tint = if (rule.enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(19.dp),
                )
            }

            Switch(
                checked = rule.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accent,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = Color.Transparent,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
        )
    }
}

/** 倒计时文本：不足一小时用 mm:ss，超过则用「x 小时 y 分」。 */
private fun countdownText(now: LocalDateTime, target: LocalDateTime): String {
    val duration = Duration.between(now, target)
    if (duration.isNegative) return "00:00"
    val minutes = duration.toMinutes()
    return if (minutes >= 60) {
        "${duration.toHours()}:${"%02d".format(minutes % 60)}"
    } else {
        "%02d:%02d".format(minutes, duration.seconds % 60)
    }
}
