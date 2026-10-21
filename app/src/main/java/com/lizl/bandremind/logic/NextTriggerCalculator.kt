package com.lizl.bandremind.logic

import com.lizl.bandremind.data.Reminder
import com.lizl.bandremind.data.RuleType
import com.lizl.bandremind.data.TimeRange
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 计算一条规则的下一次触发时刻。
 *
 * 纯函数，不依赖 Android，方便用单元测试覆盖边界情况。
 */
object NextTriggerCalculator {

    /** 最多往后找 8 天，保证覆盖「每周某一天」的最长间隔。 */
    private const val LOOKAHEAD_DAYS = 8

    /**
     * 从 [from] 开始（不含 [from] 本身）找 [reminder] 的下一次触发时刻。
     *
     * 返回 null 表示这条规则在可预见范围内不会再触发（未启用 / 未设时点 / 时间窗非法）。
     */
    fun next(reminder: Reminder, from: LocalDateTime): LocalDateTime? {
        if (!reminder.enabled) return null
        return when (reminder.type) {
            RuleType.INTERVAL -> nextInterval(reminder, from)
            RuleType.DAILY_TIMES -> nextDailyTimes(reminder, from)
        }
    }

    /**
     * 从 [before] 开始往前找 [reminder] 的上一次触发时刻（不含 [before] 本身）。
     *
     * 用来画倒计时进度：上一次触发 → 下一次触发之间走过多少。
     */
    fun previous(reminder: Reminder, before: LocalDateTime): LocalDateTime? {
        if (!reminder.enabled) return null
        return when (reminder.type) {
            RuleType.INTERVAL -> previousInterval(reminder, before)
            RuleType.DAILY_TIMES -> previousDailyTimes(reminder, before)
        }
    }

    /**
     * 当前正处于「上一次触发」到「下一次触发」之间的进度，取值 0f..1f。
     */
    fun progress(reminder: Reminder, now: LocalDateTime): Float {
        val next = next(reminder, now) ?: return 0f
        val prev = previous(reminder, now) ?: return 0f
        val total = Duration.between(prev, next).toMillis()
        if (total <= 0L) return 0f
        val elapsed = Duration.between(prev, now).toMillis()
        return (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * 列出 [date] 当天 [from] 之后的全部触发时刻，用于「今日时间线」。
     */
    fun todayOccurrences(
        reminder: Reminder,
        date: LocalDate,
        from: LocalDateTime = LocalDateTime.now(),
    ): List<LocalDateTime> {
        if (!reminder.enabled) return emptyList()
        if (!reminder.daysOfWeek.contains(date.dayOfWeek)) return emptyList()

        return when (reminder.type) {
            RuleType.INTERVAL -> {
                val step = reminder.intervalMinutes.coerceAtLeast(1).toLong()
                buildList {
                    for (segment in activeSegments(reminder)) {
                        val segStart = LocalDateTime.of(date, segment.start)
                        val segEnd = LocalDateTime.of(date, segment.end)
                        var candidate = segStart
                        while (!candidate.isAfter(segEnd)) {
                            if (candidate.isAfter(from)) add(candidate)
                            candidate = candidate.plusMinutes(step)
                        }
                    }
                }
            }

            RuleType.DAILY_TIMES -> reminder.sortedTimes
                .map { LocalDateTime.of(date, it) }
                .filter { it.isAfter(from) }
        }
    }

    /**
     * 活跃时间窗扣除免打扰区间后剩下的「活动片段」。
     *
     * 例如时段 08:30–16:30、免打扰 12:00–13:30，会拆成
     * [08:30–12:00] 与 [13:30–16:30] 两段。间隔在每段内部各自对齐段起点。
     */
    fun activeSegments(reminder: Reminder): List<TimeRange> {
        val base = TimeRange(reminder.windowStart, reminder.windowEnd)
        if (!base.isValid) return emptyList()

        var segments = listOf(base)
        for (pause in reminder.pausedWindows) {
            if (!pause.isValid) continue
            val remaining = mutableListOf<TimeRange>()
            for (segment in segments) {
                // 与该免打扰区间不相交，原样保留
                if (!pause.end.isAfter(segment.start) || !pause.start.isBefore(segment.end)) {
                    remaining += segment
                    continue
                }
                // 左侧残留
                if (pause.start.isAfter(segment.start)) {
                    remaining += TimeRange(segment.start, pause.start)
                }
                // 右侧残留
                if (pause.end.isBefore(segment.end)) {
                    remaining += TimeRange(pause.end, segment.end)
                }
            }
            segments = remaining
        }
        return segments.filter { it.isValid }.sortedBy { it.start }
    }

    private fun nextInterval(reminder: Reminder, from: LocalDateTime): LocalDateTime? {
        val segments = activeSegments(reminder)
        if (segments.isEmpty()) return null

        val step = reminder.intervalMinutes.coerceAtLeast(1).toLong()
        var date = from.toLocalDate()

        repeat(LOOKAHEAD_DAYS) {
            if (reminder.daysOfWeek.contains(date.dayOfWeek)) {
                for (segment in segments) {
                    val segStart = LocalDateTime.of(date, segment.start)
                    val segEnd = LocalDateTime.of(date, segment.end)

                    var candidate = segStart
                    if (!candidate.isAfter(from)) {
                        // 从片段起点按步长对齐，跳过已经过去的时间点
                        val elapsedMinutes = Duration.between(segStart, from).toMinutes()
                        candidate = segStart.plusMinutes((elapsedMinutes / step) * step)
                        while (!candidate.isAfter(from)) {
                            candidate = candidate.plusMinutes(step)
                        }
                    }
                    if (!candidate.isAfter(segEnd)) return candidate
                }
            }
            date = date.plusDays(1)
        }
        return null
    }

    private fun nextDailyTimes(reminder: Reminder, from: LocalDateTime): LocalDateTime? {
        if (reminder.times.isEmpty()) return null
        var date = from.toLocalDate()

        repeat(LOOKAHEAD_DAYS) {
            if (reminder.daysOfWeek.contains(date.dayOfWeek)) {
                for (time in reminder.sortedTimes) {
                    val candidate = LocalDateTime.of(date, time)
                    if (candidate.isAfter(from)) return candidate
                }
            }
            date = date.plusDays(1)
        }
        return null
    }

    private fun previousInterval(reminder: Reminder, before: LocalDateTime): LocalDateTime? {
        val segments = activeSegments(reminder)
        if (segments.isEmpty()) return null

        val step = reminder.intervalMinutes.coerceAtLeast(1).toLong()
        var date = before.toLocalDate()

        repeat(LOOKAHEAD_DAYS) {
            if (reminder.daysOfWeek.contains(date.dayOfWeek)) {
                for (segment in segments.reversed()) {
                    val segStart = LocalDateTime.of(date, segment.start)
                    val segEnd = LocalDateTime.of(date, segment.end)
                    if (segStart.isAfter(before)) continue

                    val elapsedMinutes = Duration.between(segStart, before).toMinutes()
                    var candidate = segStart.plusMinutes((elapsedMinutes / step) * step)
                    if (candidate.isAfter(segEnd)) {
                        // 已经走过整个片段，退回到片段内最后一个刻度
                        val spanMinutes = Duration.between(segStart, segEnd).toMinutes()
                        candidate = segStart.plusMinutes((spanMinutes / step) * step)
                    }
                    if (!candidate.isAfter(before)) return candidate
                }
            }
            date = date.minusDays(1)
        }
        return null
    }

    private fun previousDailyTimes(reminder: Reminder, before: LocalDateTime): LocalDateTime? {
        if (reminder.times.isEmpty()) return null
        var date = before.toLocalDate()

        repeat(LOOKAHEAD_DAYS) {
            if (reminder.daysOfWeek.contains(date.dayOfWeek)) {
                for (time in reminder.sortedTimes.reversed()) {
                    val candidate = LocalDateTime.of(date, time)
                    if (candidate.isBefore(before)) return candidate
                }
            }
            date = date.minusDays(1)
        }
        return null
    }
}
