package com.pablopcsantos.residex.residency.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pablopcsantos.residex.ui.theme.residexTopAppBarColors
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pablopcsantos.residex.residency.domain.repository.SelectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SelectionDetailsViewModel @Inject constructor(repository: SelectionRepository) : ViewModel() {
    val selections = repository.observeSelections().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SelectionDetailsScreen(id: String, viewModel: SelectionDetailsViewModel, onBack: () -> Unit) {
    val selections by viewModel.selections.collectAsState()
    val selection = selections.firstOrNull { it.id == id }
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selection?.name ?: "Seleção") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Voltar"
                        )
                    }
                },
                colors = residexTopAppBarColors()
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (selection == null) {
            Text("Seleção não encontrada.", modifier = Modifier.padding(padding).padding(16.dp))
        } else {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(selection.name, style = MaterialTheme.typography.headlineSmall)
                DetailField("UF", selection.uf)
                DetailField("Inscrições", selection.inscriptions)
                DetailField("Taxa", selection.fee)
                Text("Etapas", style = MaterialTheme.typography.titleMedium)
                DetailField("Prova objetiva", selection.objectiveExam)
                DetailField("Análise curricular", selection.curriculumAnalysis)
                DetailField("Prova prática", selection.practicalExam)
                DetailField("Entrevista", selection.interview)
                DetailField("Resultado final", selection.finalResult)
                DetailField("Edital", selection.editalInfo)
                DetailField("Observações", selection.notes)
                selection.editalLink.takeIf(::validUrl)?.let { link -> Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }, modifier = Modifier.fillMaxWidth()) { Text("Abrir edital") } }
                selection.informationLink.takeIf(::validUrl)?.let { link -> Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }, modifier = Modifier.fillMaxWidth()) { Text("Mais informações") } }
            }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    if (value.isNotBlank()) Column { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary); Text(value) }
}

private fun validUrl(value: String): Boolean = value.startsWith("https://") || value.startsWith("http://")