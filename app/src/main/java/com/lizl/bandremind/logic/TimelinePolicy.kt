package com.lizl.bandremind.logic

/**
 * 「今日提醒」的折叠策略。
 *
 * 默认只展示最近的两条，其余折叠起来，点「查看全部」再展开。
 * 抽成纯函数是为了能脱离界面做单元测试。
 */
object TimelinePolicy {

    /** 折叠时最多展示几条。 */
    const val COLLAPSED_SIZE = 2

    /**
     * 折叠状态下应该展示哪些条目。
     *
     * @param items 按时间排好序的全部条目
     * @param expanded 是否处于展开状态
     */
    fun <T> visible(items: List<T>, expanded: Boolean): List<T> =
        if (expanded) items else items.take(COLLAPSED_SIZE)

    /**
     * 折叠状态下被藏起来的条数，用于「+N」徽标。
     */
    fun hiddenCount(total: Int, visible: Int): Int = (total - visible).coerceAtLeast(0)

    /**
     * 是否需要显示「查看全部 / 收起」这条切换按钮。
     *
     * 只有一条都没有时不需要显示；有条目但都展示出来了（展开状态）也要显示「收起」。
     */
    fun showToggle(total: Int, expanded: Boolean): Boolean =
        total > 0 && (expanded || total > COLLAPSED_SIZE)
}
