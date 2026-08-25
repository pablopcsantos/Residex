package com.pablopcsantos.residex.residency.ui

import com.pablopcsantos.residex.residency.domain.model.Selection
import com.pablopcsantos.residex.residency.domain.model.SelectionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ManageSelectionsOrderingTest {
    @Test
    fun `contador do drawer considera apenas os filtros ativos`() {
        val base = CalendarUiState()

        assertEquals(0, activeDrawerFilterCount(base))
        assertEquals(1, activeDrawerFilterCount(base.copy(selectedUf = "SP")))
        assertEquals(
            2,
            activeDrawerFilterCount(
                base.copy(
                    selectedUf = "SP",
                    selectedStatus = SelectionStatus.STAGE_SOON
                )
            )
        )
    }

    @Test
    fun `lista mostra acompanhadas na ordem manual antes das demais`() {
        val state = CalendarUiState(
            allSelections = listOf(
                Selection("a", "SP", "Alpha"),
                Selection("b", "SP", "Beta"),
                Selection("c", "SP", "Charlie"),
                Selection("d", "SP", "Delta")
            ),
            followedIds = setOf("a", "c"),
            selectionOrder = listOf("c", "d", "a")
        )

        assertEquals(
            listOf("c", "a", "b", "d"),
            orderedSelections(state).map { it.id }
        )
    }
}
