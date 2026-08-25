package com.pablopcsantos.residex.residency.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pablopcsantos.residex.ui.theme.residexTopAppBarColors
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pablopcsantos.residex.residency.domain.model.Selection

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AdministrationScreen(viewModel: AdministrationViewModel) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Administração") },
                colors = residexTopAppBarColors()
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (!state.authenticated) {
            AdminLogin(
                error = state.error,
                busy = state.busy,
                onLogin = viewModel::login,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            AdminContent(
                state = state,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    }
}

@Composable
private fun AdminLogin(
    error: String?,
    busy: Boolean,
    onLogin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var password by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Área administrativa",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Entre com a senha para editar os dados sincronizados com a planilha.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Senha administrativa") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onLogin(password) },
                        enabled = !busy && password.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (busy) "Autenticando…" else "Entrar")
                    }
                    error?.let {
                        StatusText(it, isError = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminContent(
    state: AdministrationUiState,
    viewModel: AdministrationViewModel,
    modifier: Modifier = Modifier
) {
    var pendingDelete by remember { mutableStateOf<Selection?>(null) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Gerenciar seleções",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "As alterações salvas aqui são enviadas para a planilha.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = viewModel::newSelection,
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Nova seleção")
                    }
                    OutlinedButton(
                        onClick = viewModel::logout,
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sair da administração")
                    }
                }
            }
        }

        state.message?.let { message ->
            item { StatusText(message, isError = false) }
        }
        state.error?.let { error ->
            item { StatusText(error, isError = true) }
        }

        state.editing?.let { selection ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    SelectionForm(
                        selection = selection,
                        busy = state.busy,
                        onSave = viewModel::save,
                        onCancel = viewModel::cancelEdit
                    )
                }
            }
        }

        item {
            Text(
                text = "Seleções cadastradas (${state.selections.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (state.selections.isEmpty()) {
            item {
                Text(
                    text = "Nenhuma seleção disponível.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }

        items(state.selections, key = { it.id }) { selection ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = selection.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${selection.uf.ifBlank { "UF não informada" }} · ${if (selection.active) "Ativa" else "Inativa"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { viewModel.edit(selection) },
                            enabled = !state.busy
                        ) {
                            Text("Editar")
                        }
                        TextButton(
                            onClick = { pendingDelete = selection },
                            enabled = !state.busy
                        ) {
                            Text(
                                text = "Excluir",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { selection ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Excluir seleção?") },
            text = {
                Text("A seleção “${selection.name}” será excluída da planilha.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        viewModel.delete(selection)
                    }
                ) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun SelectionForm(
    selection: Selection,
    busy: Boolean,
    onSave: (Selection) -> Unit,
    onCancel: () -> Unit
) {
    var value by remember(selection.id) { mutableStateOf(selection) }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = if (value.id.isBlank()) "Nova seleção" else "Editar seleção",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        FormField("UF", value.uf) { value = value.copy(uf = it) }
        FormField("Seleção / instituição", value.name) { value = value.copy(name = it) }
        FormField("Informação sobre edital", value.editalInfo) {
            value = value.copy(editalInfo = it)
        }
        FormField("Link do edital", value.editalLink) {
            value = value.copy(editalLink = it)
        }
        FormField("Inscrições", value.inscriptions) {
            value = value.copy(inscriptions = it)
        }
        FormField("Taxa", value.fee) { value = value.copy(fee = it) }
        FormField("Prova objetiva", value.objectiveExam) {
            value = value.copy(objectiveExam = it)
        }
        FormField("Análise curricular", value.curriculumAnalysis) {
            value = value.copy(curriculumAnalysis = it)
        }
        FormField("Prova prática", value.practicalExam) {
            value = value.copy(practicalExam = it)
        }
        FormField("Entrevista", value.interview) {
            value = value.copy(interview = it)
        }
        FormField("Resultado final", value.finalResult) {
            value = value.copy(finalResult = it)
        }
        FormField("Link de informações", value.informationLink) {
            value = value.copy(informationLink = it)
        }
        FormField("Observações", value.notes) {
            value = value.copy(notes = it)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Seleção ativa",
                modifier = Modifier.weight(1f)
            )
            Checkbox(
                checked = value.active,
                onCheckedChange = { value = value.copy(active = it) }
            )
        }
        Button(
            onClick = { onSave(value) },
            enabled = !busy && value.name.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (busy) "Salvando…" else "Salvar")
        }
        OutlinedButton(
            onClick = onCancel,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancelar")
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun StatusText(message: String, isError: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            color = if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
            modifier = Modifier.padding(12.dp)
        )
    }
}
