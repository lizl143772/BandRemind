package com.lizl.bandremind

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lizl.bandremind.ui.ReminderApp
import com.lizl.bandremind.ui.theme.BandRemindTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    /**
     * 应用已在前台时收到的深链 Intent。
     *
     * 冷启动时 Navigation 会自动读取 Activity 的 intent，
     * 但 `singleTask` 下再次收到深链只会走 onNewIntent，需要显式转交给导航层。
     */
    private val incomingIntents = MutableStateFlow<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            BandRemindTheme {
                ReminderApp(incomingIntents = incomingIntents)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingIntents.value = intent
    }
}
