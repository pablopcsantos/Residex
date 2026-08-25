package com.pablopcsantos.residex.residency.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pablopcsantos.residex.residency.data.local.ResidencySettings
import com.pablopcsantos.residex.residency.domain.repository.RefreshResult
import com.pablopcsantos.residex.residency.domain.repository.SelectionRepository
import com.pablopcsantos.residex.residency.notification.AlertRepeat
import com.pablopcsantos.residex.residency.notification.NotificationDispatcher
import com.pablopcsantos.residex.residency.notification.NotificationEventType
import com.pablopcsantos.residex.residency.notification.NotificationFrequency
import com.pablopcsantos.residex.residency.notification.NotificationHistory
import com.pablopcsantos.residex.residency.notification.NotificationPostResult
import com.pablopcsantos.residex.residency.notification.NotificationPreferences
import com.pablopcsantos.residex.residency.notification.NotificationPreferencesState
import com.pablopcsantos.residex.residency.work.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: ResidencySettings,
    private val repository: SelectionRepository,
    private val notificationPreferences: NotificationPreferences,
    private val notificationScheduler: NotificationScheduler,
    private val notificationHistory: NotificationHistory,
    private val notificationDispatcher: NotificationDispatcher
) : ViewModel() {
    val state: StateFlow<com.pablopcsantos.residex.residency.data.local.ResidencySettingsState> = settings.state
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _notificationMessage = MutableStateFlow<String?>(null)
    val notificationMessage: StateFlow<String?> = _notificationMessage.asStateFlow()
    val notificationState: StateFlow<NotificationPreferencesState> = notificationPreferences.state

    fun setApiUrl(value: String) { settings.setApiUrl(value) }
    fun updateNotifications(next: NotificationPreferencesState) {
        notificationPreferences.update(next)
        if (next.enabled) notificationScheduler.schedule(next.frequency) else notificationScheduler.cancel()
    }
    fun setNotificationEnabled(enabled: Boolean) = updateNotifications(notificationState.value.copy(enabled = enabled))
    fun setFrequency(value: NotificationFrequency) = updateNotifications(notificationState.value.copy(frequency = value))
    fun setRepeat(value: AlertRepeat) = updateNotifications(notificationState.value.copy(repeat = value))
    fun setEventEnabled(type: NotificationEventType, enabled: Boolean) {
        updateNotifications(notificationState.value.withEvent(type, enabled))
    }
    fun setDays(type: NotificationEventType, days: Int) {
        updateNotifications(notificationState.value.withDays(type, days))
    }
    fun testNotification() {
        _notificationMessage.value = when (notificationDispatcher.test()) {
            NotificationPostResult.POSTED -> "Notificação de teste enviada."
            NotificationPostResult.PERMISSION_REQUIRED ->
                "Permita notificações para concluir o teste."
            NotificationPostResult.DISABLED ->
                "As notificações estão bloqueadas nas configurações do sistema."
            NotificationPostResult.FAILED ->
                "Não foi possível enviar a notificação de teste."
        }
    }

    fun notificationPermissionDenied() {
        _notificationMessage.value = "Permissão de notificações não concedida."
    }

    fun clearNotificationHistory() {
        notificationHistory.clear()
        _notificationMessage.value = "Histórico de notificações limpo."
    }
    fun testConnection() = refresh("Conexão testada.")
    fun refresh() = refresh("Dados atualizados.")

    private fun refresh(successMessage: String) = viewModelScope.launch {
        when (repository.refresh()) {
            is RefreshResult.Updated -> _message.value = successMessage
            is RefreshResult.Failed -> _message.value = "Falha na atualização; usando cache quando disponível."
        }
    }
}

private fun NotificationPreferencesState.withEvent(type: NotificationEventType, enabled: Boolean) = when (type) {
    NotificationEventType.ENROLLMENT_START -> copy(enrollmentStartEnabled = enabled)
    NotificationEventType.ENROLLMENT_END -> copy(enrollmentEndEnabled = enabled)
    NotificationEventType.OBJECTIVE_EXAM -> copy(objectiveExamEnabled = enabled)
    NotificationEventType.CURRICULUM_ANALYSIS -> copy(curriculumAnalysisEnabled = enabled)
    NotificationEventType.PRACTICAL_EXAM -> copy(practicalExamEnabled = enabled)
    NotificationEventType.INTERVIEW -> copy(interviewEnabled = enabled)
    NotificationEventType.FINAL_RESULT -> copy(finalResultEnabled = enabled)
}

private fun NotificationPreferencesState.withDays(type: NotificationEventType, days: Int) = when (type) {
    NotificationEventType.ENROLLMENT_START -> copy(enrollmentStartDays = days)
    NotificationEventType.ENROLLMENT_END -> copy(enrollmentEndDays = days)
    else -> copy(stageDays = days)
}