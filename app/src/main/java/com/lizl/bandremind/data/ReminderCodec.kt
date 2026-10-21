package com.lizl.bandremind.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * 持久化用的扁平结构。
 *
 * [DayOfWeek] / [LocalTime] 不是 kotlinx.serialization 支持的类型，
 * 所以落盘时统一转成 Int 与 "HH:mm" 字符串。
 */
@Serializable
internal data class ReminderDto(
    val id: String,
    val title: String,
    val body: String = "",
    val type: String = "INTERVAL",
    val intervalMinutes: Int = 60,
    val times: List<String> = emptyList(),
    val days: List<Int> = emptyList(),
    val windowStart: String = "09:00",
    val windowEnd: String = "21:00",
    val pausedWindows: List<String> = emptyList(),
    val enabled: Boolean = true,
    val snoozeMinutes: Int = 10,
    val createdAt: Long = 0L,
)

@Serializable
internal data class CompletionDto(
    val ruleId: String,
    val timestamp: Long,
)

internal object ReminderCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val reminderListSerializer = ListSerializer(ReminderDto.serializer())
    private val completionListSerializer = ListSerializer(CompletionDto.serializer())

    fun encodeReminders(list: List<Reminder>): String =
        json.encodeToString(reminderListSerializer, list.map { it.toDto() })

    fun decodeReminders(raw: String?): List<Reminder> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(reminderListSerializer, raw) }
            .getOrDefault(emptyList())
            .map { it.toReminder() }
            .filter { it.id.isNotBlank() }
    }

    fun encodeCompletions(list: List<CompletionEvent>): String =
        json.encodeToString(completionListSerializer, list.map { CompletionDto(it.ruleId, it.timestamp) })

    fun decodeCompletions(raw: String?): List<CompletionEvent> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(completionListSerializer, raw) }
            .getOrDefault(emptyList())
            .map { CompletionEvent(it.ruleId, it.timestamp) }
    }

    private fun Reminder.toDto() = ReminderDto(
        id = id,
        title = title,
        body = body,
        type = type.name,
        intervalMinutes = intervalMinutes,
        times = times.map { it.toString() },
        days = daysOfWeek.map { it.value }.sorted(),
        windowStart = windowStart.toString(),
        windowEnd = windowEnd.toString(),
        pausedWindows = pausedWindows.map { "${it.start}-${it.end}" },
        enabled = enabled,
        snoozeMinutes = snoozeMinutes,
        createdAt = createdAt,
    )

    private fun ReminderDto.toReminder() = Reminder(
        id = id,
        title = title,
        body = body,
        type = runCatching { RuleType.valueOf(type) }.getOrDefault(RuleType.INTERVAL),
        intervalMinutes = intervalMinutes.coerceIn(1, 24 * 60),
        times = times.mapNotNull { parseTime(it) }.sorted(),
        daysOfWeek = days.mapNotNull { dayOfWeek(it) }.toSet()
            .ifEmpty { DayOfWeek.entries.toSet() },
        windowStart = parseTime(windowStart) ?: LocalTime.of(9, 0),
        windowEnd = parseTime(windowEnd) ?: LocalTime.of(21, 0),
        pausedWindows = pausedWindows.mapNotNull { parseRange(it) },
        enabled = enabled,
        snoozeMinutes = snoozeMinutes.coerceIn(1, 120),
        createdAt = createdAt,
    )

    private fun parseTime(raw: String): LocalTime? =
        runCatching { LocalTime.parse(raw) }.getOrNull()

    /** "09:00-12:00" → TimeRange(09:00, 12:00) */
    private fun parseRange(raw: String): TimeRange? {
        val parts = raw.split("-")
        if (parts.size != 2) return null
        val start = parseTime(parts[0]) ?: return null
        val end = parseTime(parts[1]) ?: return null
        return TimeRange(start, end).takeIf { it.isValid }
    }

    /** DayOfWeek.valueOf 是 1=周一 … 7=周日。 */
    private fun dayOfWeek(value: Int): DayOfWeek? =
        runCatching { DayOfWeek.of(value) }.getOrNull()
}
