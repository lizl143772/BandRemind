package com.lizl.bandremind.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.reminderDataStore: DataStore<Preferences> by preferencesDataStore(name = "band_remind")

/**
 * 规则的持久化仓库。
 *
 * 整份规则列表以 JSON 存在一个 DataStore key 里 —— 规则数量是个位数到几十，
 * 整体读写比拆成多条 key 更不容易出现部分写入导致的状态不一致。
 */
class ReminderRepository(private val dataStore: DataStore<Preferences>) {

    private val rulesKey = stringPreferencesKey("reminder_rules_v1")
    private val completionsKey = stringPreferencesKey("completion_events_v1")

    val rules: Flow<List<Reminder>> = dataStore.data.map { prefs ->
        ReminderCodec.decodeReminders(prefs[rulesKey])
    }

    val completions: Flow<List<CompletionEvent>> = dataStore.data.map { prefs ->
        ReminderCodec.decodeCompletions(prefs[completionsKey])
    }

    suspend fun upsert(reminder: Reminder) {
        dataStore.edit { prefs ->
            val current = ReminderCodec.decodeReminders(prefs[rulesKey])
            val index = current.indexOfFirst { it.id == reminder.id }
            val updated = if (index >= 0) {
                current.toMutableList().apply { set(index, reminder) }
            } else {
                current + reminder
            }
            prefs[rulesKey] = ReminderCodec.encodeReminders(updated)
        }
    }

    suspend fun delete(id: String) {
        dataStore.edit { prefs ->
            val updated = ReminderCodec.decodeReminders(prefs[rulesKey]).filterNot { it.id == id }
            prefs[rulesKey] = ReminderCodec.encodeReminders(updated)
        }
    }

    suspend fun addCompletion(event: CompletionEvent) {
        dataStore.edit { prefs ->
            val current = ReminderCodec.decodeCompletions(prefs[completionsKey])
            val updated = (current + event).takeLast(MAX_COMPLETION_HISTORY)
            prefs[completionsKey] = ReminderCodec.encodeCompletions(updated)
        }
    }

    private companion object {
        const val MAX_COMPLETION_HISTORY = 500
    }
}

/** DataStore 是单例，整个进程共用一份。 */
object ReminderStore {
    fun repository(context: Context): ReminderRepository =
        ReminderRepository(context.applicationContext.reminderDataStore)
}
