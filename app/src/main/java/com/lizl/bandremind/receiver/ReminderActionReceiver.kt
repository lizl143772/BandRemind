package com.lizl.bandremind.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lizl.bandremind.AppScope
import com.lizl.bandremind.data.CompletionEvent
import com.lizl.bandremind.data.ReminderStore
import com.lizl.bandremind.notify.ReminderNotifier
import com.lizl.bandremind.schedule.ReminderScheduler
import kotlinx.coroutines.launch

/** 处理通知上的「已完成」与「稍后提醒」两个动作。 */
class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_ID) ?: return

        when (intent.action) {
            ACTION_COMPLETE -> {
                ReminderNotifier.cancel(context, reminderId)
                val pendingResult = goAsync()
                AppScope.scope.launch {
                    try {
                        ReminderStore.repository(context).addCompletion(
                            CompletionEvent(reminderId, System.currentTimeMillis()),
                        )
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_SNOOZE -> {
                ReminderNotifier.cancel(context, reminderId)
                val minutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)
                ReminderScheduler(context).scheduleSnooze(reminderId, minutes)
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "com.lizl.bandremind.action.COMPLETE"
        const val ACTION_SNOOZE = "com.lizl.bandremind.action.SNOOZE"
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
    }
}
