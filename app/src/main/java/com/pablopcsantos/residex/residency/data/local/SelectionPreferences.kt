package com.pablopcsantos.residex.residency.data.local

import android.content.Context
import androidx.core.content.edit
import com.pablopcsantos.residex.residency.domain.model.SelectionSortMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class SelectionPreferenceState(
    val followedIds: List<String> = emptyList(),
    val selectionOrder: List<String> = emptyList(),
    val sortMode: SelectionSortMode = SelectionSortMode.MANUAL,
    val newSelectionsAutoFollow: Boolean = true
)

@Singleton
class SelectionPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val storage = context.getSharedPreferences("residency_preferences", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<SelectionPreferenceState> = _state.asStateFlow()

    fun setFollowed(ids: List<String>, order: List<String> = _state.value.selectionOrder) = update {
        copy(followedIds = ids.distinct(), selectionOrder = order.distinct())
    }

    fun setOrder(order: List<String>) = update { copy(selectionOrder = order.distinct()) }

    fun setSortMode(mode: SelectionSortMode) = update { copy(sortMode = mode) }

    fun setAutoFollow(enabled: Boolean) = update { copy(newSelectionsAutoFollow = enabled) }

    private fun update(transform: SelectionPreferenceState.() -> SelectionPreferenceState) {
        val next = _state.value.transform()
        storage.edit {
            putStringSet(KEY_FOLLOWED, next.followedIds.toSet())
            putString(KEY_ORDER, next.selectionOrder.joinToString(","))
            putString(KEY_SORT, next.sortMode.name)
            putBoolean(KEY_AUTO, next.newSelectionsAutoFollow)
        }
        _state.value = next
    }

    private fun load(): SelectionPreferenceState {
        val sort = storage.getString(KEY_SORT, SelectionSortMode.MANUAL.name)
            ?.let { runCatching { SelectionSortMode.valueOf(it) }.getOrNull() }
            ?: SelectionSortMode.MANUAL
        return SelectionPreferenceState(
            followedIds = storage.getStringSet(KEY_FOLLOWED, emptySet()).orEmpty().toList(),
            selectionOrder = storage.getString(KEY_ORDER, "").orEmpty().split(",").filter(String::isNotBlank),
            sortMode = sort,
            newSelectionsAutoFollow = storage.getBoolean(KEY_AUTO, true)
        )
    }

    private companion object {
        const val KEY_FOLLOWED = "followed_ids"
        const val KEY_ORDER = "selection_order"
        const val KEY_SORT = "sort_mode"
        const val KEY_AUTO = "new_selections_auto_follow"
    }
}