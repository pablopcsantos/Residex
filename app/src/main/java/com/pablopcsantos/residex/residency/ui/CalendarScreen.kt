package com.pablopcsantos.residex.residency.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pablopcsantos.residex.residency.domain.SelectionRules
import com.pablopcsantos.residex.residency.domain.model.Selection
import com.pablopcsantos.residex.residency.domain.model.SelectionSortMode
import com.pablopcsantos.residex.residency.domain.model.SelectionStatus
import com.pablopcsantos.residex.ui.theme.residexTopAppBarColors
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CalendarScreen(viewModel: CalendarViewModel, onOpenDetails: (String) -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val activeFilterCount = activeDrawerFilterCount(state)
    val openFilters: () -> Unit = {
        scope.launch { drawerState.open() }
        Unit
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                FilterDrawer(
                    state = state,
                    viewModel = viewModel,
                    activeFilterCount = activeFilterCount,
                    onClose = {
                        scope.launch { drawerState.close() }
                        Unit
                    }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            topBar = {
                TopAppBar(
                    title = { Text("Residex") },
                    navigationIcon = {
                        IconButton(onClick = openFilters) {
                            BadgedBox(
                                badge = {
                                    if (activeFilterCount > 0) {
                                        Badge { Text(activeFilterCount.toString()) }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "Abrir filtros"
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = viewModel::refresh,
                            enabled = !state.isRefreshing
                        ) {
                            if (state.isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Atualizar dados")
                            }
                        }
                    },
                    colors = residexTopAppBarColors()
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    label = { Text("Pesquisar instituição ou seleção") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = selectionCountLabel(state.selections.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (activeFilterCount > 0) {
                            Text(
                                text = activeFilterSummary(state),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    TextButton(onClick = openFilters) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (activeFilterCount == 0) {
                                "Filtros"
                            } else {
                                "Filtros · $activeFilterCount"
                            },
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }

                state.error?.let {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (state.selections.isEmpty()) {
                    EmptyCalendarState(
                        hasFilters = state.query.isNotBlank() || activeFilterCount > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.selections, key = { it.id }) { selection ->
                            SelectionCard(
                                selection = selection,
                                onClick = { onOpenDetails(selection.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterDrawer(
    state: CalendarUiState,
    viewModel: CalendarViewModel,
    activeFilterCount: Int,
    onClose: () -> Unit
) {
    Column(Modifier.fillMaxHeight()) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Filtros e ordenação",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = selectionCountLabel(state.selections.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Fechar filtros",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            DrawerSectionTitle("Estado")
            UfMenu(state = state, onSelect = viewModel::setUf)

            DrawerSectionTitle(
                text = "Situação",
                modifier = Modifier.padding(top = 20.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                statusFilters.forEach { filter ->
                    FilterChip(
                        selected = state.selectedStatus == filter.status,
                        onClick = { viewModel.setStatus(filter.status) },
                        label = {
                            Text(
                                text = filter.label,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            DrawerSectionTitle(
                text = "Ordenação",
                modifier = Modifier.padding(top = 20.dp)
            )
            DrawerSortMenu(
                mode = state.sortMode,
                onSelect = viewModel::setSortMode
            )
            if (state.sortMode == SelectionSortMode.MANUAL) {
                Text(
                    text = "Ajuste esta ordem na aba Seleções usando as setas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
            if (activeFilterCount > 0) {
                FilledTonalButton(
                    onClick = {
                        viewModel.setUf("")
                        viewModel.setStatus(null)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Limpar filtros")
                }
                Spacer(Modifier.height(8.dp))
            }
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver ${selectionCountLabel(state.selections.size).lowercase()}")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DrawerSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun UfMenu(state: CalendarUiState, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (state.selectedUf.isBlank()) {
                    "Todas as UFs"
                } else {
                    "UF: ${state.selectedUf}"
                },
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            DropdownMenuItem(
                text = { Text("Todas as UFs") },
                onClick = {
                    onSelect("")
                    expanded = false
                }
            )
            state.ufs.forEach { value ->
                DropdownMenuItem(
                    text = { Text(value) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun DrawerSortMenu(
    mode: SelectionSortMode,
    onSelect: (SelectionSortMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null)
            Text(
                text = mode.label(),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            SelectionSortMode.entries.forEach { value ->
                DropdownMenuItem(
                    text = { Text(value.label()) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SelectionCard(selection: Selection, onClick: () -> Unit) {
    val status = SelectionRules.status(selection)

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = selection.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = selection.uf.ifBlank { "UF —" },
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            StatusBadge(
                status = status,
                modifier = Modifier.padding(top = 8.dp)
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            SelectionField("Inscrições", selection.inscriptions)
            SelectionField("Taxa", selection.fee)
            SelectionField(
                "Próxima etapa",
                selection.objectiveExam.ifBlank {
                    selection.curriculumAnalysis.ifBlank { selection.practicalExam }
                }
            )
            SelectionField("Resultado", selection.finalResult)
        }
    }
}

@Composable
private fun StatusBadge(status: SelectionStatus, modifier: Modifier = Modifier) {
    val containerColor = when (status) {
        SelectionStatus.OPEN -> MaterialTheme.colorScheme.primaryContainer
        SelectionStatus.UPCOMING -> MaterialTheme.colorScheme.tertiaryContainer
        SelectionStatus.STAGE_SOON -> MaterialTheme.colorScheme.secondaryContainer
        SelectionStatus.CLOSED -> MaterialTheme.colorScheme.surfaceContainerHighest
        SelectionStatus.FOLLOWING -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = when (status) {
        SelectionStatus.OPEN -> MaterialTheme.colorScheme.onPrimaryContainer
        SelectionStatus.UPCOMING -> MaterialTheme.colorScheme.onTertiaryContainer
        SelectionStatus.STAGE_SOON -> MaterialTheme.colorScheme.onSecondaryContainer
        SelectionStatus.CLOSED,
        SelectionStatus.FOLLOWING -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Text(
            text = status.label(),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SelectionField(label: String, value: String) {
    if (value.isNotBlank()) {
        Column(Modifier.padding(vertical = 4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmptyCalendarState(hasFilters: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (hasFilters) "Nenhum resultado" else "Nenhuma seleção acompanhada",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (hasFilters) {
                        "Tente remover um filtro ou usar outro termo de busca."
                    } else {
                        "Escolha as seleções que deseja acompanhar na aba Seleções."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

internal fun activeDrawerFilterCount(state: CalendarUiState): Int =
    listOf(
        state.selectedUf.isNotBlank(),
        state.selectedStatus != null
    ).count { it }

private fun activeFilterSummary(state: CalendarUiState): String =
    listOfNotNull(
        state.selectedUf.takeIf { it.isNotBlank() }?.let { "UF $it" },
        state.selectedStatus?.label()
    ).joinToString(" · ")

private data class StatusFilter(val status: SelectionStatus?, val label: String)

private val statusFilters = listOf(
    StatusFilter(null, "Todas"),
    StatusFilter(SelectionStatus.OPEN, "Inscrições abertas"),
    StatusFilter(
        SelectionStatus.UPCOMING,
        "Inscrições em ${SelectionRules.UPCOMING_WINDOW_DAYS} dias"
    ),
    StatusFilter(
        SelectionStatus.STAGE_SOON,
        "Etapas próximas · ${SelectionRules.UPCOMING_WINDOW_DAYS} dias"
    ),
    StatusFilter(SelectionStatus.CLOSED, "Inscrições encerradas")
)

private fun selectionCountLabel(count: Int) = when (count) {
    0 -> "Nenhuma seleção"
    1 -> "1 seleção"
    else -> "$count seleções"
}

private fun SelectionSortMode.label() = when (this) {
    SelectionSortMode.MANUAL -> "Minha ordem"
    SelectionSortMode.OPEN_FIRST -> "Inscrições abertas primeiro"
    SelectionSortMode.NEXT_EVENT -> "Próximo evento"
    SelectionSortMode.ALPHABETICAL -> "Instituição (A–Z)"
}

private fun SelectionStatus.label() = when (this) {
    SelectionStatus.OPEN -> "Inscrições abertas"
    SelectionStatus.UPCOMING -> "Inscrições nos próximos ${SelectionRules.UPCOMING_WINDOW_DAYS} dias"
    SelectionStatus.STAGE_SOON -> "Etapa nos próximos ${SelectionRules.UPCOMING_WINDOW_DAYS} dias"
    SelectionStatus.CLOSED -> "Inscrições encerradas"
    SelectionStatus.FOLLOWING -> "Acompanhando"
}
