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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pablopcsantos.residex.ui.theme.residexTopAppBarColors
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pablopcsantos.residex.residency.domain.model.Selection

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ManageSelectionsScreen(
    viewModel: CalendarViewModel,
    onOpenDetails: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val ordered = orderedSelections(state)
    val followed = ordered.filter { it.id in state.followedIds }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Minhas seleções") },
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Acompanhar novas seleções",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Adiciona automaticamente novos processos vindos da planilha.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.autoFollow,
                            onCheckedChange = viewModel::setAutoFollow
                        )
                    }
                }
            }

            item {
                Column(Modifier.padding(top = 6.dp)) {
                    Text(
                        text = "${followed.size} de ${state.allSelections.size} acompanhadas",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Use as setas para definir a opção “Minha ordem” do calendário.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(ordered, key = { it.id }) { selection ->
                val followedIndex = followed.indexOfFirst { it.id == selection.id }
                SelectionManagementCard(
                    selection = selection,
                    followed = followedIndex >= 0,
                    canMoveUp = followedIndex > 0,
                    canMoveDown = followedIndex >= 0 && followedIndex < followed.lastIndex,
                    onToggle = { viewModel.toggleFollow(selection.id) },
                    onMoveUp = { viewModel.moveFollowed(selection.id, -1) },
                    onMoveDown = { viewModel.moveFollowed(selection.id, 1) },
                    onOpenDetails = { onOpenDetails(selection.id) }
                )
            }
        }
    }
}

@Composable
private fun SelectionManagementCard(
    selection: Selection,
    followed: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onOpenDetails: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = followed,
                    onCheckedChange = { onToggle() }
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = selection.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = selection.uf.ifBlank { "UF não informada" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onOpenDetails) {
                    Text("Detalhes")
                }
            }

            if (followed) {
                HorizontalDivider(Modifier.padding(horizontal = 12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Posição na ordem manual",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "Mover ${selection.name} para cima"
                        )
                    }
                    IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Mover ${selection.name} para baixo"
                        )
                    }
                }
            }
        }
    }
}

internal fun orderedSelections(state: CalendarUiState): List<Selection> {
    val orderIndex = state.selectionOrder
        .mapIndexed { index, id -> id to index }
        .toMap()

    return state.allSelections.sortedWith(
        compareBy<Selection> { it.id !in state.followedIds }
            .thenBy {
                if (it.id in state.followedIds) orderIndex[it.id] ?: Int.MAX_VALUE
                else Int.MAX_VALUE
            }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    )
}
