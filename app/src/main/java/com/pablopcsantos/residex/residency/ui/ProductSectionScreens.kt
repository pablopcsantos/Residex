package com.pablopcsantos.residex.residency.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pablopcsantos.residex.ui.theme.residexTopAppBarColors
import androidx.core.content.ContextCompat
import com.pablopcsantos.residex.residency.data.local.ResidencySettingsState
import com.pablopcsantos.residex.residency.notification.AlertRepeat
import com.pablopcsantos.residex.residency.notification.NotificationEventType
import com.pablopcsantos.residex.residency.notification.NotificationFrequency
import com.pablopcsantos.residex.residency.notification.NotificationPreferencesState

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenAbout: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val connectionMessage by viewModel.message.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
                colors = residexTopAppBarColors()
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ConnectionSettingsCard(
                    state = state,
                    message = connectionMessage,
                    onApiUrlChange = viewModel::setApiUrl,
                    onTestConnection = viewModel::testConnection,
                    onRefresh = viewModel::refresh
                )
            }
            item {
                NotificationSettings(viewModel)
            }
            item {
                Text(
                    text = "As preferências de acompanhamento e a ordem das seleções ficam salvas somente neste aparelho.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp)
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = onOpenAbout) {
                        Text("Sobre o projeto")
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionSettingsCard(
    state: ResidencySettingsState,
    message: String?,
    onApiUrlChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onRefresh: () -> Unit
) {
    var apiUrl by remember(state.apiUrl) { mutableStateOf(state.apiUrl) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle(
                title = "Conexão e dados",
                description = "Configure a planilha publicada e controle a sincronização."
            )
            OutlinedTextField(
                value = apiUrl,
                onValueChange = {
                    apiUrl = it
                    onApiUrlChange(it)
                },
                label = { Text("URL da API do Apps Script") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Atualizar dados agora")
            }
            OutlinedButton(
                onClick = onTestConnection,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Testar conexão")
            }
            HorizontalDivider()
            Text(
                text = state.lastSyncAt?.let {
                    "Última sincronização: ${formatSyncTimestamp(it)}"
                } ?: "Ainda não sincronizado",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (state.usingCache) {
                Text(
                    text = "Exibindo dados armazenados no aparelho.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            message?.let { MessageBanner(it) }
        }
    }
}

@Composable
@SuppressLint("InlinedApi")
private fun NotificationSettings(viewModel: SettingsViewModel) {
    val notificationState by viewModel.notificationState.collectAsState()
    val notificationMessage by viewModel.notificationMessage.collectAsState()
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(hasNotificationPermission(context))
    }
    var pendingAction by remember {
        mutableStateOf(NotificationPermissionAction.NONE)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        val action = pendingAction
        pendingAction = NotificationPermissionAction.NONE
        if (!granted) {
            viewModel.notificationPermissionDenied()
        } else {
            when (action) {
                NotificationPermissionAction.ENABLE ->
                    viewModel.setNotificationEnabled(true)
                NotificationPermissionAction.TEST ->
                    viewModel.testNotification()
                NotificationPermissionAction.NONE -> Unit
            }
        }
    }

    fun requestPermission(action: NotificationPermissionAction) {
        pendingAction = action
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle(
                title = "Notificações",
                description = "Receba alertas das seleções acompanhadas e faça um teste antes de confiar neles."
            )

            SettingToggle(
                label = "Ativar notificações",
                supportingText = "Permite verificações automáticas em segundo plano.",
                checked = notificationState.enabled,
                onCheckedChange = { enabled ->
                    if (
                        enabled &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !hasNotificationPermission(context)
                    ) {
                        requestPermission(NotificationPermissionAction.ENABLE)
                    } else {
                        viewModel.setNotificationEnabled(enabled)
                    }
                }
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionGranted) {
                Text(
                    text = "O Android ainda não concedeu permissão para exibir notificações.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            NotificationDropdown(
                title = "Frequência de verificação",
                selected = notificationState.frequency.label(),
                options = NotificationFrequency.entries.map { it.label() }
            ) { label ->
                viewModel.setFrequency(
                    NotificationFrequency.entries.first { it.label() == label }
                )
            }
            NotificationDropdown(
                title = "Repetição do alerta",
                selected = notificationState.repeat.label(),
                options = AlertRepeat.entries.map { it.label() }
            ) { label ->
                viewModel.setRepeat(
                    AlertRepeat.entries.first { it.label() == label }
                )
            }

            HorizontalDivider()
            Text(
                text = "Eventos monitorados",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            EventToggle(
                "Início das inscrições",
                NotificationEventType.ENROLLMENT_START,
                notificationState.enrollmentStartEnabled,
                viewModel
            )
            EventToggle(
                "Fim das inscrições",
                NotificationEventType.ENROLLMENT_END,
                notificationState.enrollmentEndEnabled,
                viewModel
            )
            EventToggle(
                "Prova objetiva",
                NotificationEventType.OBJECTIVE_EXAM,
                notificationState.objectiveExamEnabled,
                viewModel
            )
            EventToggle(
                "Análise curricular",
                NotificationEventType.CURRICULUM_ANALYSIS,
                notificationState.curriculumAnalysisEnabled,
                viewModel
            )
            EventToggle(
                "Prova prática",
                NotificationEventType.PRACTICAL_EXAM,
                notificationState.practicalExamEnabled,
                viewModel
            )
            EventToggle(
                "Entrevista",
                NotificationEventType.INTERVIEW,
                notificationState.interviewEnabled,
                viewModel
            )
            EventToggle(
                "Resultado final",
                NotificationEventType.FINAL_RESULT,
                notificationState.finalResultEnabled,
                viewModel
            )

            HorizontalDivider()
            Text(
                text = "Antecedência",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            DaysDropdown(
                title = "Início das inscrições",
                selectedDays = notificationState.enrollmentStartDays
            ) {
                viewModel.setDays(NotificationEventType.ENROLLMENT_START, it)
            }
            DaysDropdown(
                title = "Fim das inscrições",
                selectedDays = notificationState.enrollmentEndDays
            ) {
                viewModel.setDays(NotificationEventType.ENROLLMENT_END, it)
            }
            DaysDropdown(
                title = "Provas, análises, entrevistas e resultados",
                selectedDays = notificationState.stageDays
            ) {
                viewModel.setDays(NotificationEventType.OBJECTIVE_EXAM, it)
            }

            HorizontalDivider()
            Button(
                onClick = {
                    if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !hasNotificationPermission(context)
                    ) {
                        requestPermission(NotificationPermissionAction.TEST)
                    } else {
                        viewModel.testNotification()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Testar notificações")
            }
            OutlinedButton(
                onClick = { openNotificationSettings(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Abrir configurações de notificações")
            }
            TextButton(
                onClick = viewModel::clearNotificationHistory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Limpar histórico de alertas")
            }
            Text(
                text = "Ao limpar o histórico, alertas ainda dentro da janela poderão ser enviados novamente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            notificationMessage?.let { MessageBanner(it) }
        }
    }
}

@Composable
private fun SettingToggle(
    label: String,
    supportingText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun EventToggle(
    label: String,
    type: NotificationEventType,
    checked: Boolean,
    viewModel: SettingsViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = { viewModel.setEventEnabled(type, it) }
        )
    }
}

@Composable
private fun DaysDropdown(
    title: String,
    selectedDays: Int,
    onSelect: (Int) -> Unit
) {
    val values = listOf(0, 1, 3, 7, 14)
    NotificationDropdown(
        title = title,
        selected = daysLabel(selectedDays),
        options = values.map(::daysLabel)
    ) { selected ->
        onSelect(values.first { daysLabel(it) == selected })
    }
}

@Composable
private fun NotificationDropdown(
    title: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = selected,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MessageBanner(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp)
        )
    }
}

private enum class NotificationPermissionAction {
    NONE,
    ENABLE,
    TEST
}

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

private fun openNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    } else {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
    }
    context.startActivity(intent)
}

private fun formatSyncTimestamp(value: String): String =
    value.take(16).replace('T', ' ').ifBlank { value }

private fun daysLabel(days: Int) = when (days) {
    0 -> "No dia"
    1 -> "1 dia antes"
    else -> "$days dias antes"
}

private fun NotificationFrequency.label() = when (this) {
    NotificationFrequency.DAILY -> "Uma vez por dia"
    NotificationFrequency.TWICE_DAILY -> "Duas vezes por dia"
    NotificationFrequency.EVERY_SIX_HOURS -> "A cada 6 horas"
}

private fun AlertRepeat.label() = when (this) {
    AlertRepeat.ONCE -> "Uma vez por evento"
    AlertRepeat.DAILY_UNTIL -> "Uma vez por dia durante a janela"
}
