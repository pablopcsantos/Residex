package com.pablopcsantos.residex.residency.notification

import com.pablopcsantos.residex.residency.domain.SelectionRules
import com.pablopcsantos.residex.residency.domain.model.Selection
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object NotificationRules {
    fun eligible(
        selections: List<Selection>,
        followedIds: Set<String>,
        preferences: NotificationPreferencesState,
        today: LocalDate = LocalDate.now()
    ): List<NotificationCandidate> = selections
        .filter { it.active && it.id in followedIds }
        .flatMap { selection -> candidatesFor(selection, preferences, today) }
        .sortedBy { it.date }

    private fun candidatesFor(selection: Selection, preferences: NotificationPreferencesState, today: LocalDate): List<NotificationCandidate> {
        val result = mutableListOf<NotificationCandidate>()
        if (preferences.enrollmentStartEnabled) SelectionRules.enrollmentRanges(selection.inscriptions).forEach { range ->
            range.start?.let { add(result, selection, NotificationEventType.ENROLLMENT_START, it, preferences.enrollmentStartDays, today, "Inscrições se aproximando") }
        }
        if (preferences.enrollmentEndEnabled) SelectionRules.enrollmentRanges(selection.inscriptions).forEach { range ->
            range.end?.let { add(result, selection, NotificationEventType.ENROLLMENT_END, it, preferences.enrollmentEndDays, today, "Inscrições terminando") }
        }
        val fields = listOf(
            Triple(NotificationEventType.OBJECTIVE_EXAM, selection.objectiveExam, preferences.objectiveExamEnabled),
            Triple(NotificationEventType.CURRICULUM_ANALYSIS, selection.curriculumAnalysis, preferences.curriculumAnalysisEnabled),
            Triple(NotificationEventType.PRACTICAL_EXAM, selection.practicalExam, preferences.practicalExamEnabled),
            Triple(NotificationEventType.INTERVIEW, selection.interview, preferences.interviewEnabled),
            Triple(NotificationEventType.FINAL_RESULT, selection.finalResult, preferences.finalResultEnabled)
        )
        fields.forEach { (type, text, enabled) ->
            if (enabled) SelectionRules.allDates(text, today).forEach { date ->
                add(result, selection, type, date, preferences.stageDays, today, type.title())
            }
        }
        return result
    }

    private fun add(result: MutableList<NotificationCandidate>, selection: Selection, type: NotificationEventType, date: LocalDate, threshold: Int, today: LocalDate, title: String) {
        val days = ChronoUnit.DAYS.between(today, date)
        if (days !in 0..threshold.toLong()) return
        result += NotificationCandidate(
            key = "${selection.id}|${type.name}|$date",
            type = type,
            selectionId = selection.id,
            selectionName = selection.name,
            date = date,
            daysUntil = days,
            title = title,
            fee = selection.fee
        )
    }

    fun NotificationEventType.title() = when (this) {
        NotificationEventType.ENROLLMENT_START -> "Inscrições se aproximando"
        NotificationEventType.ENROLLMENT_END -> "Inscrições terminando"
        NotificationEventType.OBJECTIVE_EXAM -> "Prova objetiva próxima"
        NotificationEventType.CURRICULUM_ANALYSIS -> "Análise curricular próxima"
        NotificationEventType.PRACTICAL_EXAM -> "Prova prática próxima"
        NotificationEventType.INTERVIEW -> "Entrevista próxima"
        NotificationEventType.FINAL_RESULT -> "Resultado final próximo"
    }
}
