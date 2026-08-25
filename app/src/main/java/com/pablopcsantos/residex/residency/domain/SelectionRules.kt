package com.pablopcsantos.residex.residency.domain

import com.pablopcsantos.residex.residency.domain.model.Selection
import com.pablopcsantos.residex.residency.domain.model.SelectionSortMode
import com.pablopcsantos.residex.residency.domain.model.SelectionStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

object SelectionRules {
    const val UPCOMING_WINDOW_DAYS = 7

    private val dateFormatter = DateTimeFormatter.ofPattern("d/M/uuuu")
        .withResolverStyle(ResolverStyle.STRICT)
    private val rangeRegex = Regex(
        "(\\d{1,2}/\\d{1,2}(?:/\\d{2,4})?)\\s*(?:a|até|-|–|—)\\s*(\\d{1,2}/\\d{1,2}(?:/\\d{2,4})?)",
        RegexOption.IGNORE_CASE
    )
    private val dateRegex = Regex("\\d{1,2}/\\d{1,2}(?:/\\d{2,4})?")

    data class DateRange(val start: LocalDate?, val end: LocalDate?)

    fun enrollmentRanges(text: String): List<DateRange> = rangeRegex.findAll(text)
        .map { match ->
            DateRange(
                parseDate(match.groupValues[1], match.groupValues[2]),
                parseDate(match.groupValues[2], match.groupValues[1])
            )
        }
        .toList()

    fun allDates(text: String, today: LocalDate = LocalDate.now()): List<LocalDate> = dateRegex
        .findAll(text)
        .mapNotNull { parseDate(it.value, null, today) }
        .sorted()
        .toList()

    fun status(selection: Selection, today: LocalDate = LocalDate.now()): SelectionStatus = when {
        hasOpenEnrollment(selection, today) -> SelectionStatus.OPEN
        hasUpcomingEnrollment(selection, today) -> SelectionStatus.UPCOMING
        hasUpcomingStage(selection, today) -> SelectionStatus.STAGE_SOON
        hasClosedEnrollment(selection, today) -> SelectionStatus.CLOSED
        else -> SelectionStatus.FOLLOWING
    }

    fun matchesStatus(
        selection: Selection,
        status: SelectionStatus,
        today: LocalDate = LocalDate.now()
    ): Boolean = when (status) {
        SelectionStatus.OPEN -> hasOpenEnrollment(selection, today)
        SelectionStatus.UPCOMING -> hasUpcomingEnrollment(selection, today)
        SelectionStatus.STAGE_SOON -> hasUpcomingStage(selection, today)
        SelectionStatus.CLOSED -> hasClosedEnrollment(selection, today)
        SelectionStatus.FOLLOWING -> status(selection, today) == SelectionStatus.FOLLOWING
    }

    fun hasUpcomingStage(
        selection: Selection,
        today: LocalDate = LocalDate.now(),
        windowDays: Int = UPCOMING_WINDOW_DAYS
    ): Boolean = stageDates(selection, today).any { date ->
        daysBetween(today, date) in 0..windowDays.toLong()
    }

    fun nextRelevantDate(selection: Selection, today: LocalDate = LocalDate.now()): LocalDate? {
        val dates = buildList {
            enrollmentRanges(selection.inscriptions).forEach { range ->
                range.start?.let(::add)
                range.end?.let(::add)
            }
            addAll(stageDates(selection, today))
            listOf(selection.interview, selection.finalResult)
                .forEach { addAll(allDates(it, today)) }
        }
        return dates.filterNot { it.isBefore(today) }.minOrNull()
    }

    fun sort(
        selections: List<Selection>,
        followedIds: Set<String>,
        order: List<String>,
        mode: SelectionSortMode,
        today: LocalDate = LocalDate.now()
    ): List<Selection> {
        val orderIndex = order.mapIndexed { index, id -> id to index }.toMap()
        return selections.filter { it.id in followedIds }.sortedWith { left, right ->
            when (mode) {
                SelectionSortMode.MANUAL -> (orderIndex[left.id] ?: Int.MAX_VALUE)
                    .compareTo(orderIndex[right.id] ?: Int.MAX_VALUE)
                    .takeIf { it != 0 }
                    ?: left.name.compareTo(right.name, ignoreCase = true)
                SelectionSortMode.OPEN_FIRST -> statusRank(status(left, today)).compareTo(statusRank(status(right, today)))
                    .takeIf { it != 0 } ?: compareNext(left, right, today)
                SelectionSortMode.NEXT_EVENT -> compareNext(left, right, today)
                    .takeIf { it != 0 } ?: statusRank(status(left, today)).compareTo(statusRank(status(right, today)))
                SelectionSortMode.ALPHABETICAL -> left.name.lowercase(Locale.forLanguageTag("pt-BR")).compareTo(right.name.lowercase(Locale.forLanguageTag("pt-BR")))
            }
        }
    }

    private fun hasOpenEnrollment(selection: Selection, today: LocalDate): Boolean =
        enrollmentRanges(selection.inscriptions).any { range ->
            range.start != null && range.end != null && today in range.start..range.end
        }

    private fun hasUpcomingEnrollment(selection: Selection, today: LocalDate): Boolean =
        enrollmentRanges(selection.inscriptions).any { range ->
            range.start != null &&
                daysBetween(today, range.start) in 0..UPCOMING_WINDOW_DAYS.toLong()
        }

    private fun hasClosedEnrollment(selection: Selection, today: LocalDate): Boolean {
        val latestEnd = enrollmentRanges(selection.inscriptions).mapNotNull { it.end }.maxOrNull()
        return latestEnd != null && today.isAfter(latestEnd)
    }

    private fun stageDates(selection: Selection, today: LocalDate): List<LocalDate> =
        listOf(selection.objectiveExam, selection.curriculumAnalysis, selection.practicalExam)
            .flatMap { allDates(it, today) }

    private fun compareNext(left: Selection, right: Selection, today: LocalDate): Int {
        val leftDate = nextRelevantDate(left, today)
        val rightDate = nextRelevantDate(right, today)
        return when {
            leftDate == null && rightDate == null -> left.name.compareTo(right.name, ignoreCase = true)
            leftDate == null -> 1
            rightDate == null -> -1
            else -> leftDate.compareTo(rightDate)
        }
    }

    private fun statusRank(status: SelectionStatus): Int = when (status) {
        SelectionStatus.OPEN -> 0
        SelectionStatus.UPCOMING -> 1
        SelectionStatus.STAGE_SOON -> 2
        SelectionStatus.FOLLOWING -> 3
        SelectionStatus.CLOSED -> 4
    }

    private fun daysBetween(from: LocalDate, to: LocalDate): Long = ChronoUnit.DAYS.between(from, to)

    private fun parseDate(token: String, related: String?, today: LocalDate = LocalDate.now()): LocalDate? {
        val parts = token.split('/')
        if (parts.size !in 2..3) return null
        val year = when {
            parts.size == 3 -> parts[2].toIntOrNull()?.let { if (it < 100) it + 2000 else it }
            related != null -> related.substringAfterLast('/').toIntOrNull()?.let { if (it < 100) it + 2000 else it }
            else -> today.year
        } ?: return null
        return runCatching { LocalDate.parse("${parts[0]}/${parts[1]}/$year", dateFormatter) }.getOrNull()
    }
}