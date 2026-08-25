package com.pablopcsantos.residex.residency.data

import com.pablopcsantos.residex.residency.data.local.ResidencySettings
import com.pablopcsantos.residex.residency.data.local.SelectionDao
import com.pablopcsantos.residex.residency.data.remote.AdminDataDto
import com.pablopcsantos.residex.residency.data.remote.ResidencyApiService
import com.pablopcsantos.residex.residency.data.remote.SelectionDto
import com.pablopcsantos.residex.residency.data.remote.PostActionResponseDto
import com.pablopcsantos.residex.residency.domain.model.Selection
import com.pablopcsantos.residex.residency.domain.repository.AdminResult
import com.pablopcsantos.residex.residency.domain.repository.AuthResult
import com.squareup.moshi.Moshi
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class SelectionRepositoryImplTest {
    private val api = mockk<ResidencyApiService>()
    private val dao = mockk<SelectionDao>(relaxed = true)
    private val settings = mockk<ResidencySettings>(relaxed = true)
    private lateinit var repository: SelectionRepositoryImpl

    @Before
    fun setUp() {
        every { settings.state } returns MutableStateFlow(
            com.pablopcsantos.residex.residency.data.local.ResidencySettingsState(
                apiUrl = "https://example.test/exec"
            )
        )
        repository = SelectionRepositoryImpl(api, dao, settings, Moshi.Builder().build())
    }

    @Test
    fun `authenticate aceita somente ok e authenticated verdadeiros`() = runTest {
        coEvery { api.postAction(any(), any()) } returns Response.success(PostActionResponseDto(ok = true, authenticated = true))

        assertEquals(AuthResult.Authenticated, repository.authenticate("senha"))
    }

    @Test
    fun `authenticate rejeita authenticated falso mesmo com ok verdadeiro`() = runTest {
        coEvery { api.postAction(any(), any()) } returns Response.success(PostActionResponseDto(ok = true, authenticated = false))

        val result = repository.authenticate("senha")

        assertTrue(result is AuthResult.Rejected)
    }

    @Test
    fun `getAdminData retorna ativos e inativos`() = runTest {
        coEvery { api.postAction(any(), any()) } returns Response.success(PostActionResponseDto(ok = true, data = AdminDataDto(listOf(selectionDto("TRUE"), selectionDto("FALSE")))))

        val result = repository.getAdminData("senha")

        assertEquals(2, (result as AdminResult.Success).selections.size)
        assertTrue(result.selections.any { !it.active })
    }

    @Test
    fun `saveSelection retorna estado devolvido pela API`() = runTest {
        coEvery { api.postAction(any(), any()) } returns Response.success(PostActionResponseDto(
            ok = true,
            data = AdminDataDto(saved = true, id = "SEL-001", data = AdminDataDto(listOf(selectionDto("TRUE"))))
        ))

        val result = repository.saveSelection("senha", selection())

        assertEquals("Hospital", (result as AdminResult.Success).selections.single().name)
    }

    @Test
    fun `deleteSelection retorna estado devolvido pela API`() = runTest {
        coEvery { api.postAction(any(), any()) } returns Response.success(PostActionResponseDto(
            ok = true,
            data = AdminDataDto(deleted = true, id = "SEL-001", data = AdminDataDto(emptyList()))
        ))

        val result = repository.deleteSelection("senha", "SEL-001")

        assertEquals(emptyList<Selection>(), (result as AdminResult.Success).selections)
    }

    @Test
    fun `resposta HTTP ou JSON invalido vira falha administrativa`() = runTest {
        coEvery { api.postAction(any(), any()) } returns Response.error(500, "erro".toResponseBody())

        val result = repository.getAdminData("senha")

        assertTrue(result is AdminResult.Failure)
    }

    @Test
    fun `excecao de JSON invalido vira falha administrativa`() = runTest {
        coEvery { api.postAction(any(), any()) } throws IOException("JSON inválido")

        val result = repository.getAdminData("senha")

        assertTrue(result is AdminResult.Failure)
    }

    @Test
    fun `monta URL de dados sem query string`() {
        assertEquals("https://example.test/exec?resource=data", ResidencyApiUrl.withResource("https://example.test/exec", "data"))
    }

    @Test
    fun `preserva query string e substitui resource`() {
        assertEquals(
            "https://example.test/exec?token=abc&resource=health",
            ResidencyApiUrl.withResource("https://example.test/exec?token=abc&resource=data", "health")
        )
    }

    @Test
    fun `remove resource da URL usada em POST e preserva outros parametros`() {
        assertEquals(
            "https://example.test/exec?token=abc",
            ResidencyApiUrl.forPost("https://example.test/exec?resource=data&token=abc")
        )
    }

    private fun selection() = Selection("SEL-001", "SP", "Hospital")

    private fun selectionDto(active: String) = SelectionDto(
        id = "SEL-001", uf = "SP", name = "Hospital", active = active
    )
}