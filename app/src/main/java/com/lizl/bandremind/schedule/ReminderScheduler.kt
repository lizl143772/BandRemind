package com.lizl.bandremind.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.lizl.bandremind.data.Reminder
import com.lizl.bandremind.logic.NextTriggerCalculator
import com.lizl.bandremind.receiver.ReminderAlarmReceiver
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 用 [AlarmManager] 排程提醒。
 *
 * 每条规则只挂一个闹钟：闹钟触发 → 发通知 → 重新计算并挂下一次。
 * 这样「每 45 分钟」不会因为一次性挂 100 个闹钟而丢失，也不会有累积漂移。
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager?
        get() = context.getSystemService(AlarmManager::class.java)

    /** 重新排程某条规则的下一次触发。 */
    fun schedule(reminder: Reminder) {
        val manager = alarmManager ?: return
        if (!reminder.enabled) {
            cancel(reminder.id)
            return
        }
        val next = NextTriggerCalculator.next(reminder, LocalDateTime.now()) ?: return
        val triggerAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val operation = alarmPendingIntent(reminder.id)

        try {
            if (canScheduleExact(manager)) {
                // setAlarmClock 在 Doze 下依然准时，且会在状态栏显示下一个闹钟
                manager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAt, showPendingIntent()),
                    operation,
                )
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "精确闹钟被拒绝，退化为非精确闹钟", e)
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        }
    }

    fun cancel(reminderId: String) {
        val manager = alarmManager ?: return
        manager.cancel(alarmPendingIntent(reminderId))
    }

    /** 「稍后提醒」用的一次性闹钟，不影响规则本身的下一次排程。 */
    fun scheduleSnooze(reminderId: String, minutes: Int) {
        val manager = alarmManager ?: return
        val triggerAt = System.currentTimeMillis() + minutes.coerceAtLeast(1) * 60_000L
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_SNOOZE_FIRE
            putExtra(ReminderAlarmReceiver.EXTRA_ID, reminderId)
        }
        val operation = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode() + SNOOZE_REQUEST_OFFSET,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            if (canScheduleExact(manager)) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "精确闹钟被拒绝，退化为非精确闹钟", e)
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        }
    }

    /** 系统是否允许精确闹钟。 */
    fun canScheduleExact(): Boolean = alarmManager?.let { canScheduleExact(it) } ?: false

    private fun canScheduleExact(manager: AlarmManager): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }

    /** 跳转系统「闹钟和提醒」权限页。 */
    fun exactAlarmSettingsIntent(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .setData(android.net.Uri.parse("package:${context.packageName}"))
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }

    private fun alarmPendingIntent(reminderId: String): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_FIRE
            putExtra(ReminderAlarmReceiver.EXTRA_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun showPendingIntent(): PendingIntent {
        val intent = Intent(context, com.lizl.bandremind.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            SHOW_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val TAG = "ReminderScheduler"
        const val SNOOZE_REQUEST_OFFSET = 100_000
        const val SHOW_REQUEST_CODE = 200_000
    }
}
