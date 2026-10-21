package com.lizl.bandremind

import android.app.Application
import com.lizl.bandremind.data.ReminderStore
import com.lizl.bandremind.notify.ReminderNotifier
import com.lizl.bandremind.schedule.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BandRemindApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ReminderNotifier.ensureChannel(this)

        // 进程被杀后 AlarmManager 里的闹钟可能已经失效，启动时统一重建一次
        AppScope.scope.launch {
            val rules = ReminderStore.repository(this@BandRemindApp).rules.first()
            val scheduler = ReminderScheduler(this@BandRemindApp)
            rules.forEach { scheduler.schedule(it) }
        }
    }
}

/** 进程级作用域，供 BroadcastReceiver 里的异步收尾使用。 */
object AppScope {
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
