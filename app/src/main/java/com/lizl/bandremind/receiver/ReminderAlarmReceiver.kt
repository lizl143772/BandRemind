package com.lizl.bandremind.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lizl.bandremind.AppScope
import com.lizl.bandremind.data.ReminderStore
import com.lizl.bandremind.notify.ReminderNotifier
import com.lizl.bandremind.schedule.ReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 闹钟到点广播。
 *
 * 收到后发通知并把规则的下一次挂上 —— 规则本身只有一个闹钟在链式推进。
 */
class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_ID) ?: return
        val isSnooze = intent.action == ACTION_SNOOZE_FIRE

        val pendingResult = goAsync()
        AppScope.scope.launch {
            try {
                val reminder = ReminderStore.repository(context).rules.first()
                    .firstOrNull { it.id == reminderId }
                if (reminder != null) {
                    ReminderNotifier.post(context, reminder)
                    if (!isSnooze) {
                        // 用当前时间重新计算下一次，避免重复触发同一个时刻
                        ReminderScheduler(context).schedule(reminder)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.lizl.bandremind.action.FIRE"
        const val ACTION_SNOOZE_FIRE = "com.lizl.bandremind.action.SNOOZE_FIRE"
        const val EXTRA_ID = "reminder_id"
    }
}
