package com.pablopcsantos.residex.residency.data.local

import android.content.Context
import androidx.core.content.edit
import com.pablopcsantos.residex.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class ResidencySettingsState(
    val apiUrl: String = Constants.RESIDENCY_API_URL,
    val lastSyncAt: String? = null,
    val usingCache: Boolean = false
)

@Singleton
class ResidencySettings @Inject constructor(
    @ApplicationContext context: Context
) {
    private val storage = context.getSharedPreferences("residency_settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<ResidencySettingsState> = _state.asStateFlow()

    fun setApiUrl(value: String) = update(_state.value.copy(apiUrl = value.trim()))
    fun markSynced(timestamp: String?) = update(_state.value.copy(lastSyncAt = timestamp, usingCache = false))
    fun markUsingCache() = update(_state.value.copy(usingCache = true))

    private fun update(next: ResidencySettingsState) {
        storage.edit {
            putString(KEY_API_URL, next.apiUrl)
            putString(KEY_LAST_SYNC, next.lastSyncAt)
            putBoolean(KEY_CACHE, next.usingCache)
        }
        _state.value = next
    }

    private fun load() = ResidencySettingsState(
        apiUrl = storage.getString(KEY_API_URL, Constants.RESIDENCY_API_URL).orEmpty(),
        lastSyncAt = storage.getString(KEY_LAST_SYNC, null),
        usingCache = storage.getBoolean(KEY_CACHE, false)
    )

    private companion object {
        const val KEY_API_URL = "api_url"
        const val KEY_LAST_SYNC = "last_sync_at"
        const val KEY_CACHE = "using_cache"
    }
}