package com.lizl.bandremind.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lizl.bandremind.AppScope
import com.lizl.bandremind.data.ReminderStore
import com.lizl.bandremind.schedule.ReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 开机 / 应用更新 / 改系统时间后重建所有闹钟。
 *
 * AlarmManager 的闹钟在设备重启后会全部丢失，所以必须重建。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        AppScope.scope.launch {
            try {
                val rules = ReminderStore.repository(context).rules.first()
                val scheduler = ReminderScheduler(context)
                rules.forEach { scheduler.schedule(it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
