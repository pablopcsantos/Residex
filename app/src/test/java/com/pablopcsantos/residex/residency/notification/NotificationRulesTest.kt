package com.pablopcsantos.residex.residency.notification

import com.pablopcsantos.residex.residency.domain.model.Selection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NotificationRulesTest {
    private val today = LocalDate.of(2026, 8, 19)

    @Test
    fun `seleciona apenas acompanhadas ativas`() {
        val active = selection("1", true, "20/08/2026")
        val inactive = selection("2", false, "20/08/2026")
        val notFollowed = selection("3", true, "20/08/2026")

        val result = NotificationRules.eligible(listOf(active, inactive, notFollowed), setOf("1", "2"), prefs(), today)

        assertEquals(listOf("1"), result.map { it.selectionId }.distinct())
    }

    @Test
    fun `respeita antecedencia configurada`() {
        val selection = selection("1", true, "22/08/2026")

        assertTrue(NotificationRules.eligible(listOf(selection), setOf("1"), prefs(stageDays = 3), today).isNotEmpty())
        assertTrue(NotificationRules.eligible(listOf(selection), setOf("1"), prefs(stageDays = 1), today).isEmpty())
    }

    @Test
    fun `inclui inicio e fim das inscricoes dentro de suas janelas`() {
        val selection = selection("1", true, "") .copy(inscriptions = "19/08 a 22/08/2026")
        val result = NotificationRules.eligible(listOf(selection), setOf("1"), prefs(startDays = 1, endDays = 3), today)

        assertEquals(setOf(NotificationEventType.ENROLLMENT_START, NotificationEventType.ENROLLMENT_END), result.map { it.type }.toSet())
    }

    @Test
    fun `deduplicacao once usa chave fixa e daily usa data`() {
        val candidate = NotificationRules.eligible(listOf(selection("1", true, "20/08/2026")), setOf("1"), prefs(), today).single()
        val onceKey = NotificationDeduplication.key(candidate, AlertRepeat.ONCE, today)
        val dailyKey = NotificationDeduplication.key(candidate, AlertRepeat.DAILY_UNTIL, today)

        assertEquals(0, NotificationDeduplication.unsent(listOf(candidate), setOf(onceKey), AlertRepeat.ONCE, today).size)
        assertEquals(1, NotificationDeduplication.unsent(listOf(candidate), setOf(onceKey), AlertRepeat.DAILY_UNTIL, today).size)
        assertTrue(dailyKey.contains("2026-08-19"))
    }

    private fun prefs(startDays: Int = 7, endDays: Int = 3, stageDays: Int = 7) = NotificationPreferencesState(
        enrollmentStartDays = startDays, enrollmentEndDays = endDays, stageDays = stageDays
    )

    private fun selection(id: String, active: Boolean, exam: String) = Selection(
        id = id, uf = "SP", name = "Seleção $id", objectiveExam = exam, active = active
    )
}