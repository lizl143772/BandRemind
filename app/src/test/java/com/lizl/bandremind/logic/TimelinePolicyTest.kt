package com.lizl.bandremind.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 「今日提醒」折叠策略的单元测试。
 *
 * 设备上无法自动化点击（HyperOS 收紧了事件注入权限），
 * 所以折叠/展开的判定规则放在这里，用纯函数覆盖。
 */
class TimelinePolicyTest {

    private val items = (1..10).toList()

    @Test
    fun 折叠时只显示最近两条() {
        assertEquals(listOf(1, 2), TimelinePolicy.visible(items, expanded = false))
    }

    @Test
    fun 展开时显示全部条目() {
        assertEquals(items, TimelinePolicy.visible(items, expanded = true))
    }

    @Test
    fun 折叠时隐藏条数等于总数减二() {
        val visible = TimelinePolicy.visible(items, expanded = false)
        assertEquals(8, TimelinePolicy.hiddenCount(items.size, visible.size))
    }

    @Test
    fun 条目不足两条时全部展示() {
        assertEquals(listOf(1), TimelinePolicy.visible(listOf(1), expanded = false))
        assertEquals(0, TimelinePolicy.hiddenCount(1, 1))
    }

    @Test
    fun 没有条目时不显示切换按钮() {
        assertFalse(TimelinePolicy.showToggle(total = 0, expanded = false))
        assertFalse(TimelinePolicy.showToggle(total = 0, expanded = true))
    }

    @Test
    fun 折叠条目超过两条时显示切换按钮() {
        assertTrue(TimelinePolicy.showToggle(total = 10, expanded = false))
    }

    @Test
    fun 展开状态下仍然显示收起按钮() {
        assertTrue(TimelinePolicy.showToggle(total = 10, expanded = true))
        // 只有两条时，折叠与展开展示一样，不需要切换按钮
        assertFalse(TimelinePolicy.showToggle(total = 2, expanded = false))
    }
}
