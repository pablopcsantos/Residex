package com.pablopcsantos.residex.residency.notification

enum class NotificationFrequency { DAILY, TWICE_DAILY, EVERY_SIX_HOURS }
enum class AlertRepeat { ONCE, DAILY_UNTIL }
enum class NotificationPostResult { POSTED, PERMISSION_REQUIRED, DISABLED, FAILED }
enum class NotificationEventType {
    ENROLLMENT_START, ENROLLMENT_END, OBJECTIVE_EXAM, CURRICULUM_ANALYSIS,
    PRACTICAL_EXAM, INTERVIEW, FINAL_RESULT
}

data class NotificationPreferencesState(
    val enabled: Boolean = true,
    val frequency: NotificationFrequency = NotificationFrequency.DAILY,
    val repeat: AlertRepeat = AlertRepeat.ONCE,
    val enrollmentStartEnabled: Boolean = true,
    val enrollmentEndEnabled: Boolean = true,
    val objectiveExamEnabled: Boolean = true,
    val curriculumAnalysisEnabled: Boolean = true,
    val practicalExamEnabled: Boolean = true,
    val interviewEnabled: Boolean = true,
    val finalResultEnabled: Boolean = true,
    val enrollmentStartDays: Int = 7,
    val enrollmentEndDays: Int = 3,
    val stageDays: Int = 7
)

data class NotificationCandidate(
    val key: String,
    val type: NotificationEventType,
    val selectionId: String,
    val selectionName: String,
    val date: java.time.LocalDate,
    val daysUntil: Long,
    val title: String,
    val fee: String
)