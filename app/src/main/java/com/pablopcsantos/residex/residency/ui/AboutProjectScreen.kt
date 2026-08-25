package com.pablopcsantos.residex.residency.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pablopcsantos.residex.ui.theme.residexTopAppBarColors

private const val LATTES_URL = "http://lattes.cnpq.br/9500873674712528"

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AboutProjectScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sobre o projeto") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = residexTopAppBarColors()
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Residex",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Calendário de Processos Seletivos de Residência",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "O Residex é um aplicativo desenvolvido de forma independente por Pablo Phillipe Cândido dos Santos.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Seu objetivo é facilitar a consulta e a organização de informações sobre processos seletivos de programas de residência, ajudando no acompanhamento de oportunidades, prazos, etapas e critérios de cada seleção.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Durante o desenvolvimento, ferramentas de inteligência artificial generativa foram utilizadas como recurso auxiliar.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            TextButton(
                onClick = { uriHandler.openUri(LATTES_URL) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Lattes: lattes.cnpq.br/9500873674712528")
            }
        }
    }
}
