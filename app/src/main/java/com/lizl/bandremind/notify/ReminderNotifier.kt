package com.lizl.bandremind.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lizl.bandremind.MainActivity
import com.lizl.bandremind.R
import com.lizl.bandremind.data.Reminder
import com.lizl.bandremind.receiver.ReminderActionReceiver

/**
 * 提醒的最终投递：发一条高优先级本地通知。
 *
 * 手机上的通知会被小米运动健康（Mi Fitness）捕获并镜像到 Redmi Watch 5/6，
 * 手表侧震动并显示通知标题与正文。所以标题写什么，手表上就显示什么。
 */
object ReminderNotifier {

    const val CHANNEL_ID = "band_remind_channel"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
            enableLights(true)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    fun post(context: Context, reminder: Reminder) {
        ensureChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val text = reminder.body.ifBlank { "该「${reminder.title}」啦" }

        val contentIntent = PendingIntent.getActivity(
            context,
            reminder.id.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val completeIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode() + 1,
            Intent(context, ReminderActionReceiver::class.java).apply {
                action = ReminderActionReceiver.ACTION_COMPLETE
                putExtra(ReminderActionReceiver.EXTRA_ID, reminder.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode() + 2,
            Intent(context, ReminderActionReceiver::class.java).apply {
                action = ReminderActionReceiver.ACTION_SNOOZE
                putExtra(ReminderActionReceiver.EXTRA_ID, reminder.id)
                putExtra(
                    ReminderActionReceiver.EXTRA_SNOOZE_MINUTES,
                    reminder.snoozeMinutes,
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setContentIntent(contentIntent)
            .addAction(0, "已完成", completeIntent)
            .addAction(0, "${reminder.snoozeMinutes} 分钟后提醒", snoozeIntent)
            .build()

        NotificationManagerCompat.from(context).notify(reminder.id.hashCode(), notification)
    }

    fun cancel(context: Context, reminderId: String) {
        NotificationManagerCompat.from(context).cancel(reminderId.hashCode())
    }

    /** 供「测试推送」用的假规则。 */
    fun postTest(context: Context) {
        post(
            context,
            Reminder(
                id = "test-${System.currentTimeMillis()}",
                title = "喝水",
                body = "这是一条测试提醒，能看到我就说明同步正常 ✅",
            ),
        )
    }
}
