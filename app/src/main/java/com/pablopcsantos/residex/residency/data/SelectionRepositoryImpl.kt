package com.pablopcsantos.residex.residency.data

import com.pablopcsantos.residex.residency.data.local.SelectionDao
import com.pablopcsantos.residex.residency.data.remote.ResidencyApiService
import com.pablopcsantos.residex.residency.data.remote.AdminActionPayload
import com.pablopcsantos.residex.residency.data.remote.AdminSelectionPayload
import com.pablopcsantos.residex.residency.domain.repository.AdminResult
import com.pablopcsantos.residex.residency.domain.repository.AuthResult
import com.pablopcsantos.residex.residency.domain.repository.RefreshResult
import com.pablopcsantos.residex.residency.domain.repository.SelectionRepository
import com.pablopcsantos.residex.residency.domain.model.Selection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import com.squareup.moshi.Moshi
import javax.inject.Inject

class SelectionRepositoryImpl @Inject constructor(
    private val api: ResidencyApiService,
    private val dao: SelectionDao,
    private val settings: com.pablopcsantos.residex.residency.data.local.ResidencySettings,
    private val moshi: Moshi
) : SelectionRepository {
    override fun observeSelections(): Flow<List<Selection>> = dao.observeActive().map { items ->
        items.map { it.toDomain() }
    }

    override suspend fun refresh(): RefreshResult = try {
        val response = api.getSelections(ResidencyApiUrl.withResource(settings.state.value.apiUrl, "data"))
        val body = response.body()
        if (!response.isSuccessful || body == null || !body.ok) {
            settings.markUsingCache()
            RefreshResult.Failed("A API não retornou dados válidos (HTTP ${response.code()}).", dao.getAll().isNotEmpty())
        } else {
            val cachedAt = System.currentTimeMillis()
            val entities = body.selecoes.filter { it.id.isNotBlank() }.map { it.toEntity(cachedAt) }
            dao.replaceAll(entities)
            settings.markSynced(java.time.Instant.now().toString())
            RefreshResult.Updated(entities.count { it.active }, body.generatedAt)
        }
    } catch (error: Exception) {
        settings.markUsingCache()
        RefreshResult.Failed(error.message ?: "Não foi possível atualizar os dados.", dao.getAll().isNotEmpty())
    }

    override suspend fun authenticate(password: String): AuthResult {
        val response = post(AdminActionPayload("authenticate", password))
        if (!response.ok) return AuthResult.Rejected(response.error ?: "Falha na autenticação.")
        return if (response.authenticated == true) AuthResult.Authenticated
        else AuthResult.Rejected("Senha administrativa inválida.")
    }

    override suspend fun getAdminData(password: String): AdminResult = adminResult(
        post(AdminActionPayload("getAdminData", password))
    )

    override suspend fun saveSelection(password: String, selection: Selection): AdminResult = adminResult(
        post(AdminActionPayload("saveSelection", password, selection.toPayload()))
    )

    override suspend fun deleteSelection(password: String, id: String): AdminResult = adminResult(
        post(AdminActionPayload("deleteSelection", password, id = id))
    )

    private suspend fun post(payload: AdminActionPayload) = try {
        val json = moshi.adapter(AdminActionPayload::class.java).toJson(payload)
        val body = json.toRequestBody("text/plain; charset=utf-8".toMediaType())
        val response = api.postAction(ResidencyApiUrl.forPost(settings.state.value.apiUrl), body)
        if (!response.isSuccessful) {
            com.pablopcsantos.residex.residency.data.remote.PostActionResponseDto(error = "HTTP ${response.code()}.")
        } else {
            response.body() ?: com.pablopcsantos.residex.residency.data.remote.PostActionResponseDto(error = "Resposta JSON vazia.")
        }
    } catch (error: Exception) {
        com.pablopcsantos.residex.residency.data.remote.PostActionResponseDto(error = error.message ?: "Falha de comunicação.")
    }

    private fun adminResult(response: com.pablopcsantos.residex.residency.data.remote.PostActionResponseDto): AdminResult {
        if (!response.ok) return AdminResult.Failure(response.error ?: "Operação administrativa recusada.")
        val data = response.data?.data ?: response.data
        return AdminResult.Success(data?.selecoes.orEmpty().map { it.toEntity(System.currentTimeMillis()).toDomain() })
    }
}

internal object ResidencyApiUrl {
    fun withResource(apiUrl: String, resource: String): String {
        val cleanUrl = apiUrl.trim()
        val parsed = cleanUrl.toHttpUrlOrNull() ?: return cleanUrl
        return parsed.newBuilder()
            .setQueryParameter("resource", resource)
            .build()
            .toString()
    }

    fun forPost(apiUrl: String): String {
        val cleanUrl = apiUrl.trim()
        val parsed = cleanUrl.toHttpUrlOrNull() ?: return cleanUrl
        return parsed.newBuilder()
            .removeAllQueryParameters("resource")
            .build()
            .toString()
    }
}

private fun Selection.toPayload() = AdminSelectionPayload(
    id = id, uf = uf, name = name, editalInfo = editalInfo, editalLink = editalLink,
    inscriptions = inscriptions, fee = fee, objectiveExam = objectiveExam,
    curriculumAnalysis = curriculumAnalysis, practicalExam = practicalExam, interview = interview,
    finalResult = finalResult, informationLink = informationLink, active = active, notes = notes
)