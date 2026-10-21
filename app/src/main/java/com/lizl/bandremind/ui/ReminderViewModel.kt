package com.lizl.bandremind.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lizl.bandremind.data.CompletionEvent
import com.lizl.bandremind.data.Reminder
import com.lizl.bandremind.data.ReminderStore
import com.lizl.bandremind.schedule.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReminderViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ReminderStore.repository(app)
    private val scheduler = ReminderScheduler(app)

    val rules: StateFlow<List<Reminder>> = repository.rules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val completions: StateFlow<List<CompletionEvent>> = repository.completions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // 规则集合每次变化都重新排程，顺带修复被系统清掉的闹钟
        viewModelScope.launch {
            repository.rules.collect { list -> list.forEach { scheduler.schedule(it) } }
        }
    }

    fun ruleById(id: String?): Reminder? =
        if (id.isNullOrBlank()) null else rules.value.firstOrNull { it.id == id }

    fun save(reminder: Reminder) {
        viewModelScope.launch { repository.upsert(reminder) }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repository.delete(id)
            scheduler.cancel(id)
        }
    }

    fun setEnabled(reminder: Reminder, enabled: Boolean) {
        save(reminder.copy(enabled = enabled))
    }

    fun logCompletion(ruleId: String) {
        viewModelScope.launch {
            repository.addCompletion(CompletionEvent(ruleId, System.currentTimeMillis()))
        }
    }

    fun canScheduleExactAlarms(): Boolean = scheduler.canScheduleExact()
}
