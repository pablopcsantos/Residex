package com.pablopcsantos.residex.residency.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import okhttp3.RequestBody
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Url

interface ResidencyApiService {
    @GET
    suspend fun getSelections(@Url url: String): Response<SelectionResponseDto>

    @POST
    suspend fun postAction(
        @Url url: String,
        @Body body: RequestBody
    ): Response<PostActionResponseDto>
}

@JsonClass(generateAdapter = true)
data class SelectionResponseDto(
    val ok: Boolean = false,
    val selecoes: List<SelectionDto> = emptyList(),
    val generatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SelectionDto(
    @param:Json(name = "ID") val id: String = "",
    @param:Json(name = "UF") val uf: String = "",
    @param:Json(name = "SELEÇÃO") val name: String = "",
    @param:Json(name = "EDITAL_INFORMAÇÃO") val editalInfo: String = "",
    @param:Json(name = "EDITAL_LINK") val editalLink: String = "",
    @param:Json(name = "INSCRIÇÕES") val inscriptions: String = "",
    @param:Json(name = "TAXA") val fee: String = "",
    @param:Json(name = "PROVA_OBJETIVA") val objectiveExam: String = "",
    @param:Json(name = "ANÁLISE_CURRICULAR") val curriculumAnalysis: String = "",
    @param:Json(name = "PROVA_PRÁTICA") val practicalExam: String = "",
    @param:Json(name = "ENTREVISTA") val interview: String = "",
    @param:Json(name = "RESULTADO_FINAL") val finalResult: String = "",
    @param:Json(name = "INFORMAÇÕES_LINK") val informationLink: String = "",
    @param:Json(name = "ATIVA") val active: String = "TRUE",
    @param:Json(name = "OBSERVAÇÕES") val notes: String = ""
)

@JsonClass(generateAdapter = true)
data class PostActionResponseDto(
    val ok: Boolean = false,
    val authenticated: Boolean? = null,
    val data: AdminDataDto? = null,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class AdminDataDto(
    val selecoes: List<SelectionDto> = emptyList(),
    val generatedAt: String? = null,
    val saved: Boolean? = null,
    val deleted: Boolean? = null,
    val id: String? = null,
    val data: AdminDataDto? = null
)

@JsonClass(generateAdapter = true)
data class AdminActionPayload(
    val action: String,
    val password: String,
    val item: AdminSelectionPayload? = null,
    val id: String? = null
)

@JsonClass(generateAdapter = true)
data class AdminSelectionPayload(
    @param:Json(name = "ID") val id: String = "",
    @param:Json(name = "UF") val uf: String = "",
    @param:Json(name = "SELEÇÃO") val name: String = "",
    @param:Json(name = "EDITAL_INFORMAÇÃO") val editalInfo: String = "",
    @param:Json(name = "EDITAL_LINK") val editalLink: String = "",
    @param:Json(name = "INSCRIÇÕES") val inscriptions: String = "",
    @param:Json(name = "TAXA") val fee: String = "",
    @param:Json(name = "PROVA_OBJETIVA") val objectiveExam: String = "",
    @param:Json(name = "ANÁLISE_CURRICULAR") val curriculumAnalysis: String = "",
    @param:Json(name = "PROVA_PRÁTICA") val practicalExam: String = "",
    @param:Json(name = "ENTREVISTA") val interview: String = "",
    @param:Json(name = "RESULTADO_FINAL") val finalResult: String = "",
    @param:Json(name = "INFORMAÇÕES_LINK") val informationLink: String = "",
    @param:Json(name = "ATIVA") val active: Boolean = true,
    @param:Json(name = "OBSERVAÇÕES") val notes: String = ""
)