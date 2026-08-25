package com.pablopcsantos.residex.residency.domain.repository

import com.pablopcsantos.residex.residency.domain.model.Selection
import kotlinx.coroutines.flow.Flow

interface SelectionRepository {
    fun observeSelections(): Flow<List<Selection>>
    suspend fun refresh(): RefreshResult
    suspend fun authenticate(password: String): AuthResult
    suspend fun getAdminData(password: String): AdminResult
    suspend fun saveSelection(password: String, selection: Selection): AdminResult
    suspend fun deleteSelection(password: String, id: String): AdminResult
}

sealed interface AuthResult {
    data object Authenticated : AuthResult
    data class Rejected(val message: String) : AuthResult
}

sealed interface AdminResult {
    data class Success(val selections: List<Selection>) : AdminResult
    data class Failure(val message: String) : AdminResult
}

sealed interface RefreshResult {
    data class Updated(val count: Int, val generatedAt: String?) : RefreshResult
    data class Failed(val message: String, val usedCache: Boolean) : RefreshResult
}