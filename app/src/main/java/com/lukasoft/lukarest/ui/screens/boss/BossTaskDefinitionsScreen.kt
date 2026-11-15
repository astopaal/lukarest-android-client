package com.lukasoft.lukarest.ui.screens.boss

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lukasoft.lukarest.data.api.ApiClient
import com.lukasoft.lukarest.data.api.TaskDefinitionDto
import com.lukasoft.lukarest.ui.components.LukaCard
import com.lukasoft.lukarest.ui.components.StatusPill
import com.lukasoft.lukarest.ui.components.TopPill
import com.lukasoft.lukarest.ui.components.LukaErrorState
import com.lukasoft.lukarest.ui.components.LukaEmptyState
import com.lukasoft.lukarest.ui.components.LukaLoadingState
import com.lukasoft.lukarest.ui.theme.*
import java.io.IOException

@Composable
fun BossTaskDefinitionsScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    definitionId: String? = null
) {
    var definitions by remember(definitionId) { mutableStateOf<List<TaskDefinitionDto>?>(null) }
    var failed by remember(definitionId) { mutableStateOf(false) }
    var attempt by remember(definitionId) { mutableIntStateOf(0) }

    LaunchedEffect(definitionId, attempt) {
        failed = false
        try {
            definitions = if (definitionId == null) {
                val response = ApiClient.tasksService.getDefinitions()
                response.body()?.takeIf { response.isSuccessful }
            } else {
                val response = ApiClient.tasksService.getDefinition(definitionId)
                response.body()?.takeIf { response.isSuccessful }?.let(::listOf)
            }
            failed = definitions == null
        } catch (_: IOException) {
            failed = true
        }
    }

    val selected = if (definitionId != null) definitions?.firstOrNull() else null
    if (selected != null) {
        key(selected.id) { BossTaskCreateScreen(onNavigateBack = onBack, definition = selected) }
        return
    }

    Scaffold(
        topBar = {
            TopPill(
                title = "Görev tanımları",
                subtitle = "Vardiya görevleri",
                initials = "P",
                modifier = Modifier.padding(16.dp),
                gradient = BossGradient,
                actionIcon = Icons.AutoMirrored.Filled.ArrowBack,
                actionDescription = "Geri",
                onActionClick = onBack
            )
        },
        containerColor = BgScreenTop
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when {
                failed -> item {
                    LukaErrorState("Görev tanımları yüklenemedi.", onRetry = { attempt++ })
                }
                definitions == null -> item { LukaLoadingState("Görev tanımları yükleniyor.") }
                definitions.orEmpty().isEmpty() -> item {
                    LukaEmptyState("Henüz görev tanımı yok.", "Vardiyalara görev tanımlandığında burada listelenecek.")
                }
                else -> items(definitions.orEmpty(), key = { it.id }) { definition ->
                    LukaCard(modifier = Modifier.clickable(onClickLabel = "Görev tanımını düzenle") { onEdit(definition.id) }) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(definition.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = BossGradStart)
                            if (definition.completionRule == "Quota") StatusPill("Ortak · ${definition.requiredCount ?: 1}")
                        }
                        Text(if (definition.audience == "SelectedStaff") "Seçili personel" else "Vardiyadaki herkes", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text("${com.lukasoft.lukarest.ui.components.priorityLabels[definition.priority] ?: definition.priority} öncelik${if (definition.requiresApproval) " · Yönetici onaylı" else ""}", style = MaterialTheme.typography.bodySmall, color = TextMain)
                    }
                }
            }
        }
    }
}
