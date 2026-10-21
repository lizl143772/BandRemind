package com.lizl.bandremind.ui

import com.lizl.bandremind.data.Reminder
import com.lizl.bandremind.data.RuleType
import com.lizl.bandremind.data.TimeRange
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * 一个内置规则模板。
 *
 * 模板只是「填好的新建表单」，进去之后所有参数都能改。
 */
data class RuleTemplate(
    val id: String,
    val name: String,
    val tagline: String,
    val detail: String,
    val build: () -> Reminder,
)

/** 内置模板清单。 */
object RuleTemplates {

    const val BLANK = "blank"
    const val WATER = "water"
    const val STAND = "stand"

    /** 周一到周五。 */
    private val WORKDAYS: Set<DayOfWeek> = DayOfWeek.entries
        .filter { it.value <= DayOfWeek.FRIDAY.value }
        .toSet()

    val all: List<RuleTemplate> = listOf(
        RuleTemplate(
            id = WATER,
            name = "喝水",
            tagline = "工作日 · 每 45 分钟 · 午休不打扰",
            detail = "周一~周五 08:30–16:30，每 45 分钟提醒一次；12:00–13:30 午休不打扰",
            build = ::water,
        ),
        RuleTemplate(
            id = STAND,
            name = "站立活动",
            tagline = "工作日 · 每小时 · 久坐提醒",
            detail = "周一~周五 09:00–18:00，每小时提醒起来活动一下",
            build = ::stand,
        ),
    )

    fun byId(id: String?): RuleTemplate? = all.firstOrNull { it.id == id }

    private fun water() = Reminder(
        id = "",
        title = "喝水",
        body = "该喝水啦，起来接一杯",
        type = RuleType.INTERVAL,
        intervalMinutes = 45,
        daysOfWeek = WORKDAYS,
        windowStart = LocalTime.of(8, 30),
        windowEnd = LocalTime.of(16, 30),
        pausedWindows = listOf(TimeRange(LocalTime.of(12, 0), LocalTime.of(13, 30))),
    )

    private fun stand() = Reminder(
        id = "",
        title = "站立活动",
        body = "起来活动一下，放松肩颈和腰背",
        type = RuleType.INTERVAL,
        intervalMinutes = 60,
        daysOfWeek = WORKDAYS,
        windowStart = LocalTime.of(9, 0),
        windowEnd = LocalTime.of(18, 0),
    )
}
