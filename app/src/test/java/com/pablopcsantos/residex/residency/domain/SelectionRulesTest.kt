package com.pablopcsantos.residex.residency.domain

import com.pablopcsantos.residex.residency.domain.model.Selection
import com.pablopcsantos.residex.residency.domain.model.SelectionSortMode
import com.pablopcsantos.residex.residency.domain.model.SelectionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SelectionRulesTest {
    private val today = LocalDate.of(2026, 8, 19)

    @Test
    fun `classifica inscricoes abertas`() {
        val selection = Selection(
            "1",
            "SP",
            "Hospital",
            inscriptions = "01/08 a 30/08/2026"
        )

        assertEquals(SelectionStatus.OPEN, SelectionRules.status(selection, today))
    }

    @Test
    fun `preserva texto sem data e calcula ano relacionado`() {
        val selection = Selection(
            "1",
            "SP",
            "Hospital",
            inscriptions = "a confirmar"
        )

        assertEquals(
            emptyList<SelectionRules.DateRange>(),
            SelectionRules.enrollmentRanges(selection.inscriptions)
        )
        assertEquals(
            LocalDate.of(2026, 10, 15),
            SelectionRules.allDates("15/10", today).single()
        )
    }

    @Test
    fun `etapas proximas inclui hoje e o setimo dia mas nao o oitavo`() {
        assertTrue(
            SelectionRules.hasUpcomingStage(
                Selection("today", "SP", "Hoje", objectiveExam = "19/08/2026"),
                today
            )
        )
        assertTrue(
            SelectionRules.hasUpcomingStage(
                Selection("seven", "SP", "Sete", objectiveExam = "26/08/2026"),
                today
            )
        )
        assertFalse(
            SelectionRules.hasUpcomingStage(
                Selection("eight", "SP", "Oito", objectiveExam = "27/08/2026"),
                today
            )
        )
    }

    @Test
    fun `filtro de etapa independe de inscricoes abertas`() {
        val selection = Selection(
            id = "1",
            uf = "SP",
            name = "Hospital",
            inscriptions = "01/08 a 30/08/2026",
            objectiveExam = "22/08/2026"
        )

        assertEquals(SelectionStatus.OPEN, SelectionRules.status(selection, today))
        assertTrue(
            SelectionRules.matchesStatus(
                selection,
                SelectionStatus.STAGE_SOON,
                today
            )
        )
    }

    @Test
    fun `etapas proximas considera datas futuras e todos os tipos de etapa`() {
        val objective = Selection(
            "objective",
            "SP",
            "Objetiva",
            objectiveExam = "10/08/2026 e 23/08/2026"
        )
        val curriculumAnalysis = Selection(
            "curriculum-analysis",
            "SP",
            "Teórica",
            curriculumAnalysis = "24/08/2026"
        )
        val practical = Selection(
            "practical",
            "SP",
            "Prática",
            practicalExam = "25/08/2026"
        )

        assertTrue(SelectionRules.hasUpcomingStage(objective, today))
        assertTrue(SelectionRules.hasUpcomingStage(curriculumAnalysis, today))
        assertTrue(SelectionRules.hasUpcomingStage(practical, today))
    }

    @Test
    fun `ordem manual respeita ids persistidos`() {
        val selections = listOf(
            Selection("a", "SP", "Alpha"),
            Selection("b", "SP", "Beta"),
            Selection("c", "SP", "Charlie")
        )

        val sorted = SelectionRules.sort(
            selections = selections,
            followedIds = selections.map { it.id }.toSet(),
            order = listOf("c", "a"),
            mode = SelectionSortMode.MANUAL,
            today = today
        )

        assertEquals(listOf("c", "a", "b"), sorted.map { it.id })
    }
}
