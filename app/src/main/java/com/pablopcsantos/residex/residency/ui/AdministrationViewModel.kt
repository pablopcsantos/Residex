package com.pablopcsantos.residex.residency.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pablopcsantos.residex.residency.domain.model.Selection
import com.pablopcsantos.residex.residency.domain.repository.AdminResult
import com.pablopcsantos.residex.residency.domain.repository.AuthResult
import com.pablopcsantos.residex.residency.domain.repository.SelectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdministrationUiState(
    val authenticated: Boolean = false,
    val selections: List<Selection> = emptyList(),
    val editing: Selection? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class AdministrationViewModel @Inject constructor(
    private val repository: SelectionRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AdministrationUiState())
    val state: StateFlow<AdministrationUiState> = _state.asStateFlow()
    private var adminPassword: String? = null

    fun login(password: String) {
        if (password.isBlank()) {
            _state.value = _state.value.copy(error = "Informe a senha administrativa.")
            return
        }
        viewModelScope.launch {
            setBusy()
            when (val result = repository.authenticate(password)) {
                AuthResult.Authenticated -> {
                    adminPassword = password
                    _state.value = _state.value.copy(authenticated = true, busy = false, error = null)
                    load()
                }
                is AuthResult.Rejected -> {
                    adminPassword = null
                    _state.value = _state.value.copy(authenticated = false, busy = false, error = result.message)
                }
            }
        }
    }

    fun logout() {
        adminPassword = null
        _state.value = AdministrationUiState()
    }

    fun edit(selection: Selection) { _state.value = _state.value.copy(editing = selection, message = null, error = null) }
    fun newSelection() { _state.value = _state.value.copy(editing = Selection(id = "", uf = "", name = ""), message = null, error = null) }
    fun cancelEdit() { _state.value = _state.value.copy(editing = null) }

    fun save(selection: Selection) {
        val password = adminPassword ?: return rejectSession()
        viewModelScope.launch {
            setBusy()
            when (val result = repository.saveSelection(password, selection)) {
                is AdminResult.Success -> _state.value = _state.value.copy(selections = result.selections, editing = null, busy = false, message = "Seleção salva com sucesso.", error = null)
                is AdminResult.Failure -> fail(result.message)
            }
        }
    }

    fun delete(selection: Selection) {
        val password = adminPassword ?: return rejectSession()
        viewModelScope.launch {
            setBusy()
            when (val result = repository.deleteSelection(password, selection.id)) {
                is AdminResult.Success -> _state.value = _state.value.copy(selections = result.selections, busy = false, message = "Seleção excluída.", error = null)
                is AdminResult.Failure -> fail(result.message)
            }
        }
    }

    private fun load() {
        val password = adminPassword ?: return
        viewModelScope.launch {
            when (val result = repository.getAdminData(password)) {
                is AdminResult.Success -> _state.value = _state.value.copy(selections = result.selections, busy = false, error = null)
                is AdminResult.Failure -> fail(result.message)
            }
        }
    }

    private fun setBusy() { _state.value = _state.value.copy(busy = true, error = null, message = null) }
    private fun fail(message: String) { _state.value = _state.value.copy(busy = false, error = message) }
    private fun rejectSession() { _state.value = _state.value.copy(authenticated = false, error = "Sessão administrativa expirada.") }
}