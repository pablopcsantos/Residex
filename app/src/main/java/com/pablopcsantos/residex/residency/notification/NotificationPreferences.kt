package com.pablopcsantos.residex.residency.notification

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationPreferences @Inject constructor(@ApplicationContext context: Context) {
    private val storage = context.getSharedPreferences("residency_notifications", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<NotificationPreferencesState> = _state.asStateFlow()

    fun update(next: NotificationPreferencesState) {
        storage.edit {
            putBoolean("enabled", next.enabled)
            putString("frequency", next.frequency.name)
            putString("repeat", next.repeat.name)
            putBoolean("enrollment_start", next.enrollmentStartEnabled)
            putBoolean("enrollment_end", next.enrollmentEndEnabled)
            putBoolean("objective", next.objectiveExamEnabled)
            putBoolean("curriculum_analysis", next.curriculumAnalysisEnabled)
            putBoolean("practical", next.practicalExamEnabled)
            putBoolean("interview", next.interviewEnabled)
            putBoolean("result", next.finalResultEnabled)
            putInt("start_days", next.enrollmentStartDays)
            putInt("end_days", next.enrollmentEndDays)
            putInt("stage_days", next.stageDays)
        }
        _state.value = next
    }

    private fun load() = NotificationPreferencesState(
        enabled = storage.getBoolean("enabled", true),
        frequency = enum("frequency", NotificationFrequency.DAILY),
        repeat = enum("repeat", AlertRepeat.ONCE),
        enrollmentStartEnabled = storage.getBoolean("enrollment_start", true),
        enrollmentEndEnabled = storage.getBoolean("enrollment_end", true),
        objectiveExamEnabled = storage.getBoolean("objective", true),
        curriculumAnalysisEnabled = storage.getBoolean("curriculum_analysis", true),
        practicalExamEnabled = storage.getBoolean("practical", true),
        interviewEnabled = storage.getBoolean("interview", true),
        finalResultEnabled = storage.getBoolean("result", true),
        enrollmentStartDays = storage.getInt("start_days", 7),
        enrollmentEndDays = storage.getInt("end_days", 3),
        stageDays = storage.getInt("stage_days", 7)
    )

    private inline fun <reified T : Enum<T>> enum(key: String, fallback: T): T =
        storage.getString(key, null)?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback
}
