package com.lizl.bandremind

import android.content.Context
import android.graphics.Rect
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.lizl.bandremind.data.Reminder
import com.lizl.bandremind.data.ReminderStore
import com.lizl.bandremind.data.RuleType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * 真机 UI 测试。
 *
 * 走真实交互：用 shell 身份启动页面、注入点击，再从无障碍树里读界面上的真实内容。
 * 之所以不用 ActivityScenario：App 进程内启动 Activity 在部分国产 ROM 上会被系统拦下
 * （MIUI 返回 ActivityTaskManager result code=102），而 shell 启动始终是允许的。
 */
@RunWith(AndroidJUnit4::class)
class BandRemindUiTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val packageName = context.packageName

    private companion object {
        const val DUMP_TAG = "BandRemindUiTest"
    }

    @Before
    fun seedRule() {
        // 每 10 分钟一次、覆盖全天，保证任何时刻「今日提醒」都远多于两条
        val rule = Reminder(
            id = "ui-test-rule",
            title = "测试提醒",
            type = RuleType.INTERVAL,
            intervalMinutes = 10,
            windowStart = LocalTime.of(0, 0),
            windowEnd = LocalTime.of(23, 59),
            daysOfWeek = DayOfWeek.entries.toSet(),
        )
        runBlocking { ReminderStore.repository(context).upsert(rule) }
    }

    @Test
    fun 今日提醒默认只显示两条() {
        launchHome()

        // 折叠按钮上写着总条数
        val toggleText = awaitText { it.startsWith("查看全部") }
        assertNotNull("折叠状态下应该出现「查看全部 N 条」", toggleText)
        val total = Regex("查看全部 (\\d+) 条").find(toggleText!!)!!.groupValues[1].toInt()
        assertTrue("今天的提醒应该不止两条，实际 $total 条", total > 2)

        // 徽标上的隐藏条数 = 总条数 - 展示条数，反证展示的正好是两条
        val badge = awaitText { it.startsWith("+") }
        assertNotNull("应该出现「+N」隐藏条数徽标", badge)
        val hidden = badge!!.removePrefix("+").toInt()
        assertEquals("折叠时应当只展示两条", 2, total - hidden)
    }

    @Test
    fun 点击查看全部可以展开所有条目() {
        launchHome()

        val before = countTimelineRows()
        tap(awaitNode { it.text?.toString()?.startsWith("查看全部") == true }!!)

        // 展开后屏幕上能看到的条目变多了
        val after = countTimelineRows()
        assertTrue("展开后可见条目应该变多，之前 $before，之后 $after", after > before)
    }

    @Test
    fun 展开后再点一次可以收起() {
        launchHome()

        tap(awaitNode { it.text?.toString()?.startsWith("查看全部") == true }!!)
        assertTrue("展开后可见条目应该变多", countTimelineRows() > 2)

        // 「收起」按钮固定在「今日提醒」标题行右侧，不用滚到长长的列表末尾
        val collapse = awaitNode { it.text?.toString() == "收起" }
        assertNotNull("展开后标题行右侧应该出现「收起」", collapse)
        tap(collapse!!)

        assertNotNull(
            "再点一次应该回到「查看全部」",
            awaitText { it.startsWith("查看全部") },
        )
    }

    @Test
    fun 从首页新建按钮进入模板页() {
        launchHome()

        tap(awaitNode { it.contentDescription?.toString() == "新建提醒" }!!)

        assertNotNull("模板页应该列出「喝水」", awaitText { it == "喝水" })
        assertNotNull("模板页应该列出「站立活动」", awaitText { it == "站立活动" })
        assertNotNull("模板页应该列出「空白规则」", awaitText { it == "空白规则" })
    }

    @Test
    fun 模板页选中喝水后预填的参数正确() {
        launchTemplates()

        tap(awaitNode { it.text?.toString() == "喝水" }!!)

        // 编辑页：标题、时段、免打扰都已经填好
        assertNotNull("应该进入新建提醒页", awaitText { it == "新建提醒" })
        assertNotNull("标题应预填为「喝水」", awaitText { it == "喝水" })
        assertTrue("应有免打扰时段区块", scrollUntilText { it == "免打扰时段" })
        assertTrue("时段起点应为 08:30", scrollUntilText { it.contains("08:30") })
        assertTrue(
            "时段终点应为 16:30",
            scrollUntilText { it.contains("16:30") || it.contains("04:30") },
        )
        assertTrue("免打扰开始应为 12:00", scrollUntilText { it.contains("12:00") })
        assertTrue(
            "免打扰结束应为 13:30",
            scrollUntilText { it.contains("13:30") || it.contains("01:30") },
        )
    }

    @Test
    fun 模板页选中站立活动后预填的参数正确() {
        launchTemplates()

        tap(awaitNode { it.text?.toString() == "站立活动" }!!)

        assertNotNull("应该进入新建提醒页", awaitText { it == "新建提醒" })
        assertNotNull("标题应预填为「站立活动」", awaitText { it == "站立活动" })
        assertTrue(
            "站立模板没有免打扰时段",
            scrollUntilText { it.contains("还没有免打扰时段") },
        )
    }

    // ---------------------------------------------------------------- 工具方法

    private fun launchHome() {
        prepare()
        shell("am start -W -n $packageName/com.lizl.bandremind.MainActivity")
        awaitAppForeground()
        keepScreenOn()
    }

    private fun launchTemplates() {
        launchHome()
        tap(awaitNode { it.contentDescription?.toString() == "新建提醒" }!!)
        assertNotNull("应该进入模板页", awaitText { it == "喝水" })
    }

    /** 收起通知栏并等界面稳定。 */
    private fun prepare() {
        shell("cmd statusbar collapse")
        SystemClock.sleep(500)
    }

    /** 以 shell 身份执行命令并等待结束，返回输出。 */
    private fun shell(command: String): String {
        val fd = instrumentation.uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(fd).use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        }
    }

    /** 等待本 App 成为前台窗口，否则无障碍树拿到的是通知栏。 */
    private fun awaitAppForeground(timeoutMs: Long = 15_000) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        var lastPackage: String? = null
        while (SystemClock.uptimeMillis() < deadline) {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            lastPackage = root?.packageName?.toString()
            if (lastPackage == packageName) {
                SystemClock.sleep(300)
                return
            }
            SystemClock.sleep(250)
        }
        throw AssertionError("等待超时：App 没有成为前台窗口（当前焦点窗口是 $lastPackage）")
    }

    /** 屏幕常亮，避免息屏后无障碍树拿不到内容。 */
    private fun keepScreenOn() {
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (SystemClock.uptimeMillis() < deadline) {
            var handled = false
            // ActivityLifecycleMonitor 只能在主线程查询
            instrumentation.runOnMainSync {
                val activity = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .firstOrNull()
                if (activity != null) {
                    activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    handled = true
                }
            }
            if (handled) {
                SystemClock.sleep(500)
                return
            }
            SystemClock.sleep(200)
        }
    }

    /**
     * 点击节点。
     *
     * 用 shell 的 `input tap`：它以 shell 身份注入，权限比 App 进程内注入更宽。
     */
    private fun tap(node: AccessibilityNodeInfo) {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        assertTrue("节点没有可见区域：${node.text}", rect.width() > 0 && rect.height() > 0)

        val x = rect.exactCenterX().toInt()
        val y = rect.exactCenterY().toInt()
        val output = shell("input tap $x $y")
        if (output.contains("Exception") || output.contains("Error")) {
            throw AssertionError("点击注入失败：$output")
        }
        SystemClock.sleep(900)
    }

    /** 无障碍树里所有非空文本。 */
    private fun allTexts(): List<String> {
        val root = instrumentation.uiAutomation.rootInActiveWindow ?: return emptyList()
        val result = mutableListOf<String>()
        collectTexts(root, result)
        return result
    }

    private fun collectTexts(node: AccessibilityNodeInfo, out: MutableList<String>) {
        node.text?.toString()?.takeIf { it.isNotEmpty() }?.let { out += it }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectTexts(it, out) }
        }
    }

    private fun findNode(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        val root = instrumentation.uiAutomation.rootInActiveWindow ?: return null
        return search(root, predicate)
    }

    private fun search(
        node: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        if (predicate(node)) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = search(child, predicate)
            if (found != null) return found
        }
        return null
    }

    private fun awaitNode(
        timeoutMs: Long = 15_000,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            findNode(predicate)?.let { return it }
            SystemClock.sleep(250)
        }
        return null
    }

    private fun awaitText(
        timeoutMs: Long = 15_000,
        predicate: (String) -> Boolean,
    ): String? {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            val hit = allTexts().firstOrNull(predicate)
            if (hit != null) {
                Log.i(DUMP_TAG, "命中节点：$hit")
                return hit
            }
            SystemClock.sleep(250)
        }
        Log.i(DUMP_TAG, "当前界面文本：${allTexts().joinToString(" | ")}")
        return null
    }

    private fun countText(predicate: (String) -> Boolean): Int = allTexts().count(predicate)

    /** 当前屏幕上能看到的「今日提醒」条目数（按 HH:mm 文本统计，减去主卡上的那个时间）。 */
    private fun countTimelineRows(): Int {
        val times = allTexts().count { it.matches(Regex("^\\d{2}:\\d{2}$")) }
        return (times - 1).coerceAtLeast(0)
    }

    /** 向下滚动直到找到目标文本。 */
    private fun scrollUntilText(
        maxSteps: Int = 20,
        predicate: (String) -> Boolean,
    ): Boolean {
        val metrics = context.resources.displayMetrics
        val x = metrics.widthPixels / 2
        val fromY = (metrics.heightPixels * 0.75).toInt()
        val toY = (metrics.heightPixels * 0.3).toInt()

        repeat(maxSteps) {
            if (allTexts().any(predicate)) return true
            // 真实滑动最稳：编辑页的 verticalScroll 不响应无障碍滚动动作
            shell("input swipe $x $fromY $x $toY 300")
            SystemClock.sleep(350)
        }
        return allTexts().any(predicate)
    }
}
