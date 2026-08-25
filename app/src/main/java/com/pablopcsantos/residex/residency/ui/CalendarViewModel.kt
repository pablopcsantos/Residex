package com.pablopcsantos.residex.residency.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pablopcsantos.residex.residency.data.local.SelectionPreferences
import com.pablopcsantos.residex.residency.domain.SelectionRules
import com.pablopcsantos.residex.residency.domain.model.Selection
import com.pablopcsantos.residex.residency.domain.model.SelectionSortMode
import com.pablopcsantos.residex.residency.domain.model.SelectionStatus
import com.pablopcsantos.residex.residency.domain.repository.RefreshResult
import com.pablopcsantos.residex.residency.domain.repository.SelectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CalendarUiState(
    val selections: List<Selection> = emptyList(),
    val allSelections: List<Selection> = emptyList(),
    val followedIds: Set<String> = emptySet(),
    val selectionOrder: List<String> = emptyList(),
    val query: String = "",
    val selectedUf: String = "",
    val selectedStatus: SelectionStatus? = null,
    val sortMode: SelectionSortMode = SelectionSortMode.MANUAL,
    val autoFollow: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
) {
    val ufs: List<String> get() = allSelections.map { it.uf }.filter(String::isNotBlank).distinct().sorted()
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: SelectionRepository,
    private val preferences: SelectionPreferences
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val uf = MutableStateFlow("")
    private val status = MutableStateFlow<SelectionStatus?>(null)
    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    private val baseState = combine(
        repository.observeSelections(), preferences.state, query, uf, status
    ) { selections, pref, search, selectedUf, selectedStatus ->
        BaseState(selections, pref, search, selectedUf, selectedStatus)
    }

    val uiState: StateFlow<CalendarUiState> = combine(baseState, refreshing, error) { base, isRefreshing, message ->
        val selections = base.selections
        val pref = base.preferences
        val search = base.search
        val selectedUf = base.selectedUf
        val selectedStatus = base.selectedStatus
        val followed = selections.filter { it.id in pref.followedIds }
        val filtered = followed.filter { selection ->
            val text = listOf(selection.name, selection.uf, selection.editalInfo, selection.notes).joinToString(" ")
            (search.isBlank() || text.contains(search, ignoreCase = true)) &&
                (selectedUf.isBlank() || selection.uf == selectedUf) &&
                (selectedStatus == null || SelectionRules.matchesStatus(selection, selectedStatus))
        }
        CalendarUiState(
            selections = SelectionRules.sort(
                filtered,
                pref.followedIds.toSet(),
                pref.selectionOrder,
                pref.sortMode
            ),
            allSelections = selections,
            followedIds = pref.followedIds.toSet(),
            selectionOrder = pref.selectionOrder,
            query = search,
            selectedUf = selectedUf,
            selectedStatus = selectedStatus,
            sortMode = pref.sortMode,
            autoFollow = pref.newSelectionsAutoFollow,
            isRefreshing = isRefreshing,
            error = message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    init {
        viewModelScope.launch {
            repository.observeSelections().collect { selections ->
                reconcileFollowed(selections)
            }
        }
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        refreshing.value = true
        error.value = when (val result = repository.refresh()) {
            is RefreshResult.Updated -> null
            is RefreshResult.Failed -> if (result.usedCache) result.message else result.message
        }
        refreshing.value = false
    }

    fun setQuery(value: String) { query.value = value }
    fun setUf(value: String) { uf.value = value }
    fun setStatus(value: SelectionStatus?) { status.value = value }
    fun setSortMode(value: SelectionSortMode) { preferences.setSortMode(value) }
    fun setAutoFollow(value: Boolean) { preferences.setAutoFollow(value) }

    fun toggleFollow(id: String) {
        val current = preferences.state.value.followedIds
        preferences.setFollowed(if (id in current) current - id else current + id)
    }

    fun moveFollowed(id: String, direction: Int) {
        val current = preferences.state.value.selectionOrder.toMutableList()
        if (id !in current) current.add(id)
        val from = current.indexOf(id)
        val to = (from + direction).coerceIn(0, current.lastIndex)
        if (from != to) current.add(to, current.removeAt(from))
        preferences.setOrder(current)
    }

    private fun reconcileFollowed(selections: List<Selection>) {
        val ids = selections.map { it.id }.toSet()
        val current = preferences.state.value
        val followed = current.followedIds.filter(ids::contains).toMutableList()
        if (current.newSelectionsAutoFollow) {
            selections.forEach { if (it.id !in followed) followed += it.id }
        }
        val order = current.selectionOrder.filter(ids::contains).toMutableList()
        followed.forEach { if (it !in order) order += it }
        if (followed != current.followedIds || order != current.selectionOrder) preferences.setFollowed(followed, order)
    }

    private data class BaseState(
        val selections: List<Selection>,
        val preferences: com.pablopcsantos.residex.residency.data.local.SelectionPreferenceState,
        val search: String,
        val selectedUf: String,
        val selectedStatus: SelectionStatus?
    )
}