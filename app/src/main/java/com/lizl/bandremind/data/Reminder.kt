package com.lizl.bandremind.data

import java.time.DayOfWeek
import java.time.LocalTime

/** 提醒规则类型。 */
enum class RuleType {
    /** 在时间窗内每隔 N 分钟提醒一次，例如每 45 分钟喝水。 */
    INTERVAL,

    /** 每天在若干个固定时点提醒，例如 08:00 / 12:00 / 18:00 吃药。 */
    DAILY_TIMES,
}

/** 一段时间区间。用于「免打扰时段」。 */
data class TimeRange(
    val start: LocalTime,
    val end: LocalTime,
) {
    val isValid: Boolean get() = end.isAfter(start)
}

/**
 * 一条提醒规则。
 *
 * 规则只描述「什么时候提醒、提醒什么」，具体怎么排程由
 * [com.lizl.bandremind.logic.NextTriggerCalculator] 计算，
 * 怎么送达由 [com.lizl.bandremind.notify.ReminderNotifier] 负责。
 */
data class Reminder(
    val id: String,
    val title: String,
    val body: String = "",
    val type: RuleType = RuleType.INTERVAL,
    /** INTERVAL 专用：间隔分钟数。 */
    val intervalMinutes: Int = 60,
    /** DAILY_TIMES 专用：每天的触发时点，按时间升序保存。 */
    val times: List<LocalTime> = emptyList(),
    /** 生效星期，默认周一到周日。 */
    val daysOfWeek: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    /** INTERVAL 专用：活跃时间窗起点。 */
    val windowStart: LocalTime = LocalTime.of(9, 0),
    /** INTERVAL 专用：活跃时间窗终点。 */
    val windowEnd: LocalTime = LocalTime.of(21, 0),
    /** INTERVAL 专用：活跃时间窗内需要跳过的区间（例如午休）。 */
    val pausedWindows: List<TimeRange> = emptyList(),
    val enabled: Boolean = true,
    /** 通知上「稍后提醒」按钮推迟的分钟数。 */
    val snoozeMinutes: Int = 10,
    val createdAt: Long = 0L,
) {
    val sortedTimes: List<LocalTime> get() = times.sorted()

    val scheduleSummary: String
        get() = when (type) {
            RuleType.INTERVAL -> buildString {
                append("每 $intervalMinutes 分钟")
                append(" · ${windowStart}–${windowEnd}")
                if (pausedWindows.isNotEmpty()) {
                    append(" · 免打扰 ${pausedWindows.joinToString("、") { "${it.start}–${it.end}" }}")
                }
            }

            RuleType.DAILY_TIMES -> if (times.isEmpty()) {
                "未设置时点"
            } else {
                // 时点太多时只展示前几个，否则摘要会把整张卡片撑爆
                val head = sortedTimes.take(MAX_SUMMARY_TIMES).joinToString("、") { it.toString() }
                if (sortedTimes.size > MAX_SUMMARY_TIMES) {
                    "$head 等 ${sortedTimes.size} 个时点"
                } else {
                    head
                }
            }
        }

    private companion object {
        /** 摘要里最多列出几个时点。 */
        const val MAX_SUMMARY_TIMES = 3
    }
}

/** 一次「已完成」记录，用于统计提醒的完成情况。 */
data class CompletionEvent(
    val ruleId: String,
    val timestamp: Long,
)
