package com.lizl.bandremind.ui

import com.lizl.bandremind.data.RuleType
import com.lizl.bandremind.logic.NextTriggerCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 内置模板的参数校验。
 *
 * 模板只是「填好的新建表单」，这里保证模板内容与产品约定一致：
 * 喝水 = 工作日 08:30–16:30 每 45 分钟，12:00–13:30 午休不提醒；
 * 站立 = 工作日每小时。
 */
class RuleTemplatesTest {

    private val workdays = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
    )

    @Test
    fun 喝水模板参数正确() {
        val water = RuleTemplates.byId(RuleTemplates.WATER)!!.build()

        assertEquals("喝水", water.title)
        assertEquals(RuleType.INTERVAL, water.type)
        assertEquals(45, water.intervalMinutes)
        assertEquals(LocalTime.of(8, 30), water.windowStart)
        assertEquals(LocalTime.of(16, 30), water.windowEnd)
        assertEquals(workdays, water.daysOfWeek)
        assertEquals(1, water.pausedWindows.size)
        assertEquals(LocalTime.of(12, 0), water.pausedWindows[0].start)
        assertEquals(LocalTime.of(13, 30), water.pausedWindows[0].end)
    }

    @Test
    fun 站立模板参数正确() {
        val stand = RuleTemplates.byId(RuleTemplates.STAND)!!.build()

        assertEquals("站立活动", stand.title)
        assertEquals(RuleType.INTERVAL, stand.type)
        assertEquals(60, stand.intervalMinutes)
        assertEquals(LocalTime.of(9, 0), stand.windowStart)
        assertEquals(LocalTime.of(18, 0), stand.windowEnd)
        assertEquals(workdays, stand.daysOfWeek)
        assertTrue(stand.pausedWindows.isEmpty())
    }

    @Test
    fun 喝水模板在午休期间不触发() {
        val water = RuleTemplates.byId(RuleTemplates.WATER)!!.build()

        // 午休 12:00–13:30 之间不应该有任何触发点
        val duringLunch = listOf(
            LocalDateTime.of(2026, 10, 20, 12, 0),
            LocalDateTime.of(2026, 10, 20, 12, 45),
            LocalDateTime.of(2026, 10, 20, 13, 15),
        )
        duringLunch.forEach { now ->
            val next = NextTriggerCalculator.next(water, now)
            assertTrue(
                "午休期间 $now 不应有触发点，实际为 $next",
                next == null || next.toLocalTime() < LocalTime.of(12, 0) ||
                    next.toLocalTime() >= LocalTime.of(13, 30),
            )
        }
    }

    @Test
    fun 喝水模板午休结束立即恢复提醒() {
        val water = RuleTemplates.byId(RuleTemplates.WATER)!!.build()

        // next() 不含当前时刻，所以 13:29 的下一次触发就是 13:30 本身，
        // 而不是等下一个 45 分钟刻度 14:15
        val next = NextTriggerCalculator.next(
            water,
            LocalDateTime.of(2026, 10, 20, 13, 29),
        )
        assertEquals(LocalDateTime.of(2026, 10, 20, 13, 30), next)
    }

    @Test
    fun 喝水模板午休开始后直接跳到午休结束() {
        val water = RuleTemplates.byId(RuleTemplates.WATER)!!.build()

        listOf(12 to 0, 12 to 45, 13 to 15).forEach { (hour, minute) ->
            val next = NextTriggerCalculator.next(
                water,
                LocalDateTime.of(2026, 10, 20, hour, minute),
            )
            assertEquals(
                "午休期间的下一次触发应为 13:30",
                LocalDateTime.of(2026, 10, 20, 13, 30),
                next,
            )
        }
    }

    @Test
    fun 喝水模板在下午四点半结束() {
        val water = RuleTemplates.byId(RuleTemplates.WATER)!!.build()

        val next = NextTriggerCalculator.next(
            water,
            LocalDateTime.of(2026, 10, 20, 16, 30),
        )
        // 16:30 当刻是当天最后一次，到点后应排到明天同一时刻
        assertEquals(LocalDateTime.of(2026, 10, 21, 8, 30), next)
    }

    @Test
    fun 模板清单包含两个内置模板() {
        assertEquals(2, RuleTemplates.all.size)
        assertEquals(listOf(RuleTemplates.WATER, RuleTemplates.STAND), RuleTemplates.all.map { it.id })
    }
}
