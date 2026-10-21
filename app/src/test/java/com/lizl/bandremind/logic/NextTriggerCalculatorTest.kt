package com.lizl.bandremind.logic

import com.lizl.bandremind.data.Reminder
import com.lizl.bandremind.data.RuleType
import com.lizl.bandremind.data.TimeRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class NextTriggerCalculatorTest {

    /** 2026-10-19 是周一，2026-10-20 是周二。 */
    private val monday: LocalDate = LocalDate.of(2026, 10, 19)
    private val tuesday: LocalDate = LocalDate.of(2026, 10, 20)

    private fun intervalRule(
        start: LocalTime = LocalTime.of(9, 0),
        end: LocalTime = LocalTime.of(12, 0),
        minutes: Int = 60,
        days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
        enabled: Boolean = true,
    ) = Reminder(
        id = "r1",
        title = "喝水",
        type = RuleType.INTERVAL,
        intervalMinutes = minutes,
        daysOfWeek = days,
        windowStart = start,
        windowEnd = end,
        enabled = enabled,
    )

    private fun dailyRule(
        times: List<LocalTime>,
        days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
        enabled: Boolean = true,
    ) = Reminder(
        id = "r2",
        title = "吃药",
        type = RuleType.DAILY_TIMES,
        times = times,
        daysOfWeek = days,
        enabled = enabled,
    )

    // ---------- 间隔提醒 ----------

    @Test
    fun `窗口开始之前触发在窗口起点`() {
        val rule = intervalRule()
        val from = LocalDateTime.of(monday, LocalTime.of(8, 0))
        assertEquals(
            LocalDateTime.of(monday, LocalTime.of(9, 0)),
            NextTriggerCalculator.next(rule, from),
        )
    }

    @Test
    fun `窗口内对齐到下一个间隔刻度`() {
        val rule = intervalRule()
        val from = LocalDateTime.of(monday, LocalTime.of(9, 30))
        assertEquals(
            LocalDateTime.of(monday, LocalTime.of(10, 0)),
            NextTriggerCalculator.next(rule, from),
        )
    }

    @Test
    fun `窗口终点仍然算一次触发`() {
        val rule = intervalRule()
        val from = LocalDateTime.of(monday, LocalTime.of(11, 30))
        assertEquals(
            LocalDateTime.of(monday, LocalTime.of(12, 0)),
            NextTriggerCalculator.next(rule, from),
        )
    }

    @Test
    fun `窗口结束后跳到次日的窗口起点`() {
        val rule = intervalRule()
        val from = LocalDateTime.of(monday, LocalTime.of(12, 0))
        assertEquals(
            LocalDateTime.of(tuesday, LocalTime.of(9, 0)),
            NextTriggerCalculator.next(rule, from),
        )
    }

    @Test
    fun `非 60 分钟的间隔也能对齐`() {
        val rule = intervalRule(start = LocalTime.of(9, 0), end = LocalTime.of(12, 0), minutes = 45)
        val from = LocalDateTime.of(monday, LocalTime.of(10, 10))
        // 09:00 / 09:45 / 10:30 / 11:15 / 12:00
        assertEquals(
            LocalDateTime.of(monday, LocalTime.of(10, 30)),
            NextTriggerCalculator.next(rule, from),
        )
    }

    @Test
    fun `只在指定星期生效时跳到那个星期`() {
        val rule = intervalRule(days = setOf(DayOfWeek.MONDAY))
        val from = LocalDateTime.of(tuesday, LocalTime.of(8, 0))
        assertEquals(
            LocalDateTime.of(monday.plusDays(7), LocalTime.of(9, 0)),
            NextTriggerCalculator.next(rule, from),
        )
    }

    @Test
    fun `时间窗非法时返回 null`() {
        val rule = intervalRule(start = LocalTime.of(21, 0), end = LocalTime.of(9, 0))
        val from = LocalDateTime.of(monday, LocalTime.of(8, 0))
        assertNull(NextTriggerCalculator.next(rule, from))
    }

    @Test
    fun `停用的规则没有下一次触发`() {
        val rule = intervalRule(enabled = false)
        val from = LocalDateTime.of(monday, LocalTime.of(8, 0))
        assertNull(NextTriggerCalculator.next(rule, from))
    }

    // ---------- 定时提醒 ----------

    @Test
    fun `定时提醒取当天最近的一个未来时点`() {
        val rule = dailyRule(listOf(LocalTime.of(8, 0), LocalTime.of(12, 0), LocalTime.of(18, 0)))
        val from = LocalDateTime.of(monday, LocalTime.of(10, 0))
        assertEquals(
            LocalDateTime.of(monday, LocalTime.of(12, 0)),
            NextTriggerCalculator.next(rule, from),
        )
    }

    @Test
    fun `当天时点都过了则顺延到次日`() {
        val rule = dailyRule(listOf(LocalTime.of(8, 0), LocalTime.of(12, 0)))
        val from = LocalDateTime.of(monday, LocalTime.of(19, 0))
        assertEquals(
            LocalDateTime.of(tuesday, LocalTime.of(8, 0)),
            NextTriggerCalculator.next(rule, from),
        )
    }

    @Test
    fun `没有设置时点则不触发`() {
        val rule = dailyRule(emptyList())
        val from = LocalDateTime.of(monday, LocalTime.of(8, 0))
        assertNull(NextTriggerCalculator.next(rule, from))
    }

    @Test
    fun `定时提醒只在选中的星期触发`() {
        val rule = dailyRule(listOf(LocalTime.of(8, 0)), days = setOf(DayOfWeek.WEDNESDAY))
        val from = LocalDateTime.of(tuesday, LocalTime.of(9, 0))
        assertEquals(
            LocalDateTime.of(monday.plusDays(2), LocalTime.of(8, 0)),
            NextTriggerCalculator.next(rule, from),
        )
    }

    // ---------- 今日时间线 ----------

    @Test
    fun `今日时间线列出窗口内剩余的时点`() {
        val rule = intervalRule(start = LocalTime.of(9, 0), end = LocalTime.of(12, 0), minutes = 60)
        val from = LocalDateTime.of(monday, LocalTime.of(9, 30))
        val occurrences = NextTriggerCalculator.todayOccurrences(rule, monday, from)
        assertEquals(
            listOf(
                LocalDateTime.of(monday, LocalTime.of(10, 0)),
                LocalDateTime.of(monday, LocalTime.of(11, 0)),
                LocalDateTime.of(monday, LocalTime.of(12, 0)),
            ),
            occurrences,
        )
    }

    @Test
    fun `今日时间线不含已过去的时点`() {
        val rule = dailyRule(listOf(LocalTime.of(8, 0), LocalTime.of(12, 0)))
        val from = LocalDateTime.of(monday, LocalTime.of(10, 0))
        val occurrences = NextTriggerCalculator.todayOccurrences(rule, monday, from)
        assertEquals(listOf(LocalDateTime.of(monday, LocalTime.of(12, 0))), occurrences)
    }

    // ---------- 上一次触发与进度 ----------

    @Test
    fun `间隔提醒的上一次触发是上一个刻度`() {
        val rule = intervalRule()
        val at = LocalDateTime.of(monday, LocalTime.of(10, 30))
        assertEquals(
            LocalDateTime.of(monday, LocalTime.of(10, 0)),
            NextTriggerCalculator.previous(rule, at),
        )
    }

    @Test
    fun `间隔提醒的上一次触发在窗口起点`() {
        val rule = intervalRule()
        val at = LocalDateTime.of(monday, LocalTime.of(9, 10))
        assertEquals(
            LocalDateTime.of(monday, LocalTime.of(9, 0)),
            NextTriggerCalculator.previous(rule, at),
        )
    }

    @Test
    fun `过了窗口之后上一次触发是窗口内最后一个刻度`() {
        val rule = intervalRule()
        val at = LocalDateTime.of(monday, LocalTime.of(15, 0))
        assertEquals(
            LocalDateTime.of(monday, LocalTime.of(12, 0)),
            NextTriggerCalculator.previous(rule, at),
        )
    }

    @Test
    fun `定时提醒的上一次触发是当天上一个时点`() {
        val rule = dailyRule(listOf(LocalTime.of(8, 0), LocalTime.of(12, 0), LocalTime.of(18, 0)))
        val at = LocalDateTime.of(monday, LocalTime.of(10, 0))
        assertEquals(
            LocalDateTime.of(monday, LocalTime.of(8, 0)),
            NextTriggerCalculator.previous(rule, at),
        )
    }

    @Test
    fun `定时提醒在当天所有时点之前回退到前一天`() {
        val rule = dailyRule(listOf(LocalTime.of(8, 0), LocalTime.of(12, 0)))
        // 周一 07:00 —— 当天 08:00 还没到，上一次是周日 12:00
        val at = LocalDateTime.of(monday, LocalTime.of(7, 0))
        assertEquals(
            LocalDateTime.of(monday.minusDays(1), LocalTime.of(12, 0)),
            NextTriggerCalculator.previous(rule, at),
        )
    }

    @Test
    fun `定时提醒在上一次时点之后取当天更早的时点`() {
        val rule = dailyRule(listOf(LocalTime.of(8, 0), LocalTime.of(12, 0)))
        // 周一 09:00 —— 当天 08:00 已经过了
        val at = LocalDateTime.of(monday, LocalTime.of(9, 0))
        assertEquals(
            LocalDateTime.of(monday, LocalTime.of(8, 0)),
            NextTriggerCalculator.previous(rule, at),
        )
    }

    @Test
    fun `进度是上一次到下一次之间走过的比例`() {
        val rule = intervalRule()
        val at = LocalDateTime.of(monday, LocalTime.of(10, 30))
        // 上一次 10:00，下一次 11:00，走到一半
        assertEquals(0.5f, NextTriggerCalculator.progress(rule, at), 0.001f)
    }

    @Test
    fun `定时提醒的进度按相邻两个时点计算`() {
        val rule = dailyRule(listOf(LocalTime.of(8, 0), LocalTime.of(12, 0)))
        val at = LocalDateTime.of(monday, LocalTime.of(10, 0))
        // 上一次 08:00，下一次 12:00，走过一半
        assertEquals(0.5f, NextTriggerCalculator.progress(rule, at), 0.001f)
    }

    @Test
    fun `没有下一次时进度为 0`() {
        val rule = dailyRule(emptyList())
        val at = LocalDateTime.of(monday, LocalTime.of(10, 0))
        assertEquals(0f, NextTriggerCalculator.progress(rule, at), 0.001f)
    }

    // ---------- 免打扰时段（午休） ----------

    /** 喝水模板：工作日 08:30–16:30，每 45 分钟，12:00–13:30 午休免打扰。 */
    private fun lunchRule(
        start: LocalTime = LocalTime.of(8, 30),
        end: LocalTime = LocalTime.of(16, 30),
        minutes: Int = 45,
        pauses: List<TimeRange> = listOf(
            TimeRange(LocalTime.of(12, 0), LocalTime.of(13, 30)),
        ),
    ) = Reminder(
        id = "lunch",
        title = "喝水",
        type = RuleType.INTERVAL,
        intervalMinutes = minutes,
        daysOfWeek = DayOfWeek.entries.toSet(),
        windowStart = start,
        windowEnd = end,
        pausedWindows = pauses,
    )

    @Test
    fun `免打扰时段把活跃窗口切成两段`() {
        val segments = NextTriggerCalculator.activeSegments(lunchRule())
        assertEquals(2, segments.size)
        assertEquals(LocalTime.of(8, 30), segments[0].start)
        assertEquals(LocalTime.of(12, 0), segments[0].end)
        assertEquals(LocalTime.of(13, 30), segments[1].start)
        assertEquals(LocalTime.of(16, 30), segments[1].end)
    }

    @Test
    fun `午休期间的提醒被跳过并在午休结束时恢复`() {
        val rule = lunchRule()
        // 11:45 —— 本该是 12:15，被午休吃掉，下一次是 13:30
        val from = LocalDateTime.of(monday, LocalTime.of(11, 45))
        assertEquals(
            LocalDateTime.of(monday, LocalTime.of(13, 30)),
            NextTriggerCalculator.next(rule, from),
        )
    }

    @Test
    fun `午休结束前不会提醒`() {
        val rule = lunchRule()
        val from = LocalDateTime.of(monday, LocalTime.of(12, 0))
        assertEquals(
            LocalDateTime.of(monday, LocalTime.of(13, 30)),
            NextTriggerCalculator.next(rule, from),
        )
    }

    @Test
    fun `今日时间线里没有午休时段的提醒`() {
        val rule = lunchRule()
        val from = LocalDateTime.of(monday, LocalTime.of(0, 0))
        val occurrences = NextTriggerCalculator.todayOccurrences(rule, monday, from)
        assertEquals(
            listOf(
                LocalTime.of(8, 30),
                LocalTime.of(9, 15),
                LocalTime.of(10, 0),
                LocalTime.of(10, 45),
                LocalTime.of(11, 30),
                LocalTime.of(13, 30),
                LocalTime.of(14, 15),
                LocalTime.of(15, 0),
                LocalTime.of(15, 45),
                LocalTime.of(16, 30),
            ).map { LocalDateTime.of(monday, it) },
            occurrences,
        )
    }

    @Test
    fun `没有免打扰时段的旧行为保持不变`() {
        val rule = lunchRule(pauses = emptyList())
        val from = LocalDateTime.of(monday, LocalTime.of(0, 0))
        val occurrences = NextTriggerCalculator.todayOccurrences(rule, monday, from)
        // 连续 45 分钟网格：8:30 9:15 10:00 ... 16:00（下一个 16:45 已超出时段）
        assertEquals(11, occurrences.size)
        assertEquals(LocalDateTime.of(monday, LocalTime.of(8, 30)), occurrences.first())
        assertEquals(LocalDateTime.of(monday, LocalTime.of(16, 0)), occurrences.last())
    }

    @Test
    fun `免打扰时段在活跃窗口之外时被忽略`() {
        val rule = lunchRule(
            start = LocalTime.of(9, 0),
            end = LocalTime.of(12, 0),
            pauses = listOf(TimeRange(LocalTime.of(13, 0), LocalTime.of(14, 0))),
        )
        val segments = NextTriggerCalculator.activeSegments(rule)
        assertEquals(1, segments.size)
        assertEquals(LocalTime.of(9, 0), segments[0].start)
        assertEquals(LocalTime.of(12, 0), segments[0].end)
    }

    @Test
    fun `重叠的免打扰时段会被合并`() {
        val rule = lunchRule(
            pauses = listOf(
                TimeRange(LocalTime.of(12, 0), LocalTime.of(13, 0)),
                TimeRange(LocalTime.of(12, 30), LocalTime.of(13, 30)),
            ),
        )
        val segments = NextTriggerCalculator.activeSegments(rule)
        assertEquals(2, segments.size)
        assertEquals(LocalTime.of(12, 0), segments[0].end)
        assertEquals(LocalTime.of(13, 30), segments[1].start)
    }

    @Test
    fun `免打扰时段吃掉了整段时窗口则没有可触发时间`() {
        val rule = lunchRule(
            start = LocalTime.of(12, 0),
            end = LocalTime.of(13, 30),
        )
        assertTrue(NextTriggerCalculator.activeSegments(rule).isEmpty())
        val from = LocalDateTime.of(monday, LocalTime.of(11, 0))
        assertNull(NextTriggerCalculator.next(rule, from))
    }

    @Test
    fun `上一次触发也会跳过午休时段`() {
        val rule = lunchRule()
        // 13:45 —— 上一次应该是 13:30，而不是午休里的 13:00
        val at = LocalDateTime.of(monday, LocalTime.of(13, 45))
        assertEquals(
            LocalDateTime.of(monday, LocalTime.of(13, 30)),
            NextTriggerCalculator.previous(rule, at),
        )
    }
}
