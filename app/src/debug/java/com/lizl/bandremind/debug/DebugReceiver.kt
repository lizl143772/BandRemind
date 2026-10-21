package com.lizl.bandremind.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.lizl.bandremind.AppScope
import com.lizl.bandremind.data.Reminder
import com.lizl.bandremind.data.ReminderStore
import com.lizl.bandremind.data.RuleType
import com.lizl.bandremind.logic.NextTriggerCalculator
import com.lizl.bandremind.receiver.ReminderAlarmReceiver
import com.lizl.bandremind.schedule.ReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 仅存在于 debug 构建的调试入口，用来在无法注入触摸事件的环境下做端到端验证。
 *
 * 可用动作：
 * - `com.lizl.bandremind.debug.SEED` 建一条规则并立刻排程
 * - `com.lizl.bandremind.debug.FIRE` 手动触发第一条规则的闹钟回调
 * - `com.lizl.bandremind.debug.LIST` 打印全部规则与下一次触发时刻
 * - `com.lizl.bandremind.debug.CLEAR` 清空全部规则
 */
class DebugReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SEED -> seed(context, intent)
            ACTION_FIRE -> fire(context)
            ACTION_LIST -> list(context)
            ACTION_CLEAR -> clear(context)
            else -> Log.w(TAG, "未知 action: ${intent.action}")
        }
    }

    /** 清空全部规则，方便重新做一遍干净的验证。 */
    private fun clear(context: Context) {
        val pending = goAsync()
        AppScope.scope.launch {
            try {
                val repository = ReminderStore.repository(context)
                val ids = repository.rules.first().map { it.id }
                ids.forEach { repository.delete(it) }
                Log.i(TAG, "已清空 ${ids.size} 条规则")
            } finally {
                pending.finish()
            }
        }
    }

    private fun seed(context: Context, intent: Intent) {
        val now = LocalDateTime.now()
        val type = if (intent.getStringExtra(EXTRA_TYPE) == TYPE_TIMES) {
            RuleType.DAILY_TIMES
        } else {
            RuleType.INTERVAL
        }
        val times = intent.getStringExtra(EXTRA_TIMES)
            ?.split(",")
            ?.mapNotNull { runCatching { LocalTime.parse(it.trim()) }.getOrNull() }
            ?: emptyList()

        val rule = Reminder(
            id = "debug-${now.toLocalTime().toString().replace(":", "")}-${now.nano}",
            title = intent.getStringExtra(EXTRA_TITLE) ?: "喝水",
            body = intent.getStringExtra(EXTRA_BODY) ?: "调试规则触发",
            type = type,
            intervalMinutes = intent.getIntExtra(EXTRA_INTERVAL, 1).coerceIn(1, 24 * 60),
            times = times,
            daysOfWeek = DayOfWeek.entries.toSet(),
            windowStart = intent.getStringExtra(EXTRA_WINDOW_START)
                ?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
                ?: now.toLocalTime().minusMinutes(1),
            windowEnd = intent.getStringExtra(EXTRA_WINDOW_END)
                ?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
                ?: now.toLocalTime().plusMinutes(30),
            enabled = intent.getBooleanExtra(EXTRA_ENABLED, true),
            snoozeMinutes = 1,
            createdAt = System.currentTimeMillis(),
        )
        val pending = goAsync()
        AppScope.scope.launch {
            try {
                ReminderStore.repository(context).upsert(rule)
                ReminderScheduler(context).schedule(rule)
                val next = NextTriggerCalculator.next(rule, LocalDateTime.now())
                Log.i(TAG, "SEEDED id=${rule.id} type=${rule.type} next=$next")
            } finally {
                pending.finish()
            }
        }
    }

    private fun fire(context: Context) {
        val pending = goAsync()
        AppScope.scope.launch {
            try {
                val rule = ReminderStore.repository(context).rules.first().firstOrNull()
                if (rule == null) {
                    Log.w(TAG, "FIRE 失败：没有规则，请先 SEED")
                    return@launch
                }
                Log.i(TAG, "FIRE id=${rule.id} title=${rule.title}")
                context.sendBroadcast(
                    Intent(context, ReminderAlarmReceiver::class.java).apply {
                        action = ReminderAlarmReceiver.ACTION_FIRE
                        putExtra(ReminderAlarmReceiver.EXTRA_ID, rule.id)
                    },
                )
            } finally {
                pending.finish()
            }
        }
    }

    private fun list(context: Context) {
        val pending = goAsync()
        AppScope.scope.launch {
            try {
                val rules = ReminderStore.repository(context).rules.first()
                Log.i(TAG, "共 ${rules.size} 条规则")
                rules.forEach { rule ->
                    Log.i(
                        TAG,
                        "  id=${rule.id} title=${rule.title} enabled=${rule.enabled} " +
                            "next=${NextTriggerCalculator.next(rule, LocalDateTime.now())}",
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val TAG = "BandRemindDebug"
        const val ACTION_SEED = "com.lizl.bandremind.debug.SEED"
        const val ACTION_FIRE = "com.lizl.bandremind.debug.FIRE"
        const val ACTION_LIST = "com.lizl.bandremind.debug.LIST"
        const val ACTION_CLEAR = "com.lizl.bandremind.debug.CLEAR"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val EXTRA_TYPE = "type"
        const val EXTRA_TIMES = "times"
        const val EXTRA_INTERVAL = "interval"
        const val EXTRA_WINDOW_START = "windowStart"
        const val EXTRA_WINDOW_END = "windowEnd"
        const val EXTRA_ENABLED = "enabled"

        const val TYPE_TIMES = "times"
    }
}
