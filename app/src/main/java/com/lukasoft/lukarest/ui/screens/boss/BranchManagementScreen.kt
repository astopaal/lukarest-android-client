package com.lukasoft.lukarest.ui.screens.boss

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lukasoft.lukarest.data.api.ApiClient
import com.lukasoft.lukarest.data.api.BranchDto
import com.lukasoft.lukarest.data.api.CreateBranchCommand
import com.lukasoft.lukarest.data.api.UpdateBranchCommand
import com.lukasoft.lukarest.ui.components.LukaCard
import com.lukasoft.lukarest.ui.components.PrimaryButton
import com.lukasoft.lukarest.ui.components.PullToRefreshBox
import com.lukasoft.lukarest.ui.components.StatusPill
import com.lukasoft.lukarest.ui.components.TopPill
import com.lukasoft.lukarest.ui.theme.BgScreenTop
import com.lukasoft.lukarest.ui.theme.BossGradient
import com.lukasoft.lukarest.ui.theme.BossGradStart
import com.lukasoft.lukarest.ui.theme.PillBlue
import com.lukasoft.lukarest.ui.theme.PillErr
import com.lukasoft.lukarest.ui.theme.TextMain
import com.lukasoft.lukarest.ui.theme.TextMuted
import com.lukasoft.lukarest.ui.theme.TextSoft
import com.lukasoft.lukarest.ui.theme.White
import kotlinx.coroutines.launch
import java.io.IOException

private data class BranchDraft(
    val name: String = "",
    val code: String = "",
    val address: String = "",
    val city: String = "",
    val opening: String = "09:00",
    val closing: String = "23:00"
)

@Composable
fun BranchManagementScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var branches by remember { mutableStateOf<List<BranchDto>>(emptyList()) }
    var draft by remember { mutableStateOf<BranchDraft?>(null) }
    var editing by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var revision by remember { mutableIntStateOf(0) }

    suspend fun load() {
        val response = ApiClient.branchService.getBranches()
        if (response.isSuccessful) branches = response.body().orEmpty()
        else error = "Şube listesi alınamadı (${response.code()})."
    }

    LaunchedEffect(revision) {
        try { load() } catch (_: IOException) { error = "Sunucuya ulaşılamadı." }
    }

    fun saveDraft() {
        val value = draft ?: return
        if (busy) return
        if (value.name.isBlank() || value.code.isBlank() || value.city.isBlank() ||
            !value.opening.matches(Regex("\\d{2}:\\d{2}")) || !value.closing.matches(Regex("\\d{2}:\\d{2}"))) {
            error = "Şube adı, kodu, şehri ve saatleri kontrol edin. Saat biçimi 09:30 olmalı."
            return
        }
        busy = true
        error = null
        scope.launch {
            try {
                val id = editing
                val result = if (id == null) ApiClient.branchService.createBranch(
                    CreateBranchCommand(value.name.trim(), value.code.trim(), value.address.trim(), value.city.trim(), "${value.opening}:00", "${value.closing}:00")
                ) else ApiClient.branchService.updateBranch(
                    id,
                    UpdateBranchCommand(value.name.trim(), value.code.trim(), value.address.trim(), value.city.trim(), "${value.opening}:00", "${value.closing}:00")
                )
                if (result.isSuccessful) {
                    draft = null
                    editing = null
                    revision++
                    snackbar.showSnackbar(if (id == null) "Şube oluşturuldu" else "Şube güncellendi")
                } else error = "Kaydedilemedi (${result.code()}). Bilgileri ve şirket şube sınırını kontrol edin."
            } catch (_: IOException) { error = "Sunucuya ulaşılamadı." }
            finally { busy = false }
        }
    }

    Scaffold(
        containerColor = BgScreenTop,
        snackbarHost = { com.lukasoft.lukarest.ui.components.LukaSnackbarHost(snackbar) },
        topBar = {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                TopPill(
                    title = "Şube yönetimi",
                    subtitle = "Şubeleri düzenle ve yeni şube ekle",
                    initials = "Ş",
                    gradient = BossGradient,
                    actionIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    actionDescription = "Geri",
                    onActionClick = onBack
                )
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    try { load() } catch (_: IOException) { error = "Sunucuya ulaşılamadı." }
                    finally { isRefreshing = false }
                }
            },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Şube listesi", style = MaterialTheme.typography.headlineSmall, color = BossGradStart)
                    Text("Çalışma saatleri ve temel şube bilgileri burada yönetilir.", color = TextSoft)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
                }
                item {
                    PrimaryButton(
                        text = "Yeni şube ekle",
                        onClick = { error = null; editing = null; draft = BranchDraft() },
                        enabled = !busy,
                        gradient = BossGradient
                    )
                }
                if (branches.isEmpty()) item {
                    LukaCard {
                        Text("Henüz şube yok", style = MaterialTheme.typography.titleMedium, color = TextMain)
                        Text("İlk şubenizi ekleyerek başlayın.", color = TextMuted)
                    }
                }
                items(branches, key = { it.id }) { branch ->
                    LukaCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(branch.name, style = MaterialTheme.typography.titleMedium, color = TextMain)
                                Text("${branch.city.orEmpty()}${branch.address?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()}", color = TextSoft)
                                Text("${branch.openingTime.take(5)} – ${branch.closingTime.take(5)} · ${branch.branchCode}", color = TextMuted)
                            }
                            StatusPill(
                                text = if (branch.isActive) "Aktif" else "Pasif",
                                pillColor = if (branch.isActive) PillBlue else PillErr
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(enabled = !busy, onClick = {
                                editing = branch.id
                                error = null
                                draft = BranchDraft(branch.name, branch.branchCode, branch.address.orEmpty(), branch.city.orEmpty(), branch.openingTime.take(5), branch.closingTime.take(5))
                            }) {
                                androidx.compose.material3.Icon(Icons.Default.Edit, null)
                                Text("Düzenle")
                            }
                            TextButton(enabled = !busy, onClick = {
                                busy = true
                                scope.launch {
                                    try {
                                        val result = if (branch.isActive) ApiClient.branchService.deactivateBranch(branch.id)
                                            else ApiClient.branchService.activateBranch(branch.id)
                                        if (result.isSuccessful) revision++ else error = "Şube durumu değiştirilemedi (${result.code()})."
                                    } catch (_: IOException) { error = "Sunucuya ulaşılamadı." }
                                    finally { busy = false }
                                }
                            }) { Text(if (branch.isActive) "Pasife al" else "Etkinleştir") }
                        }
                    }
                }
            }
        }
    }

    draft?.let { value ->
        AlertDialog(
            onDismissRequest = { if (!busy) { draft = null; editing = null } },
            title = { Text(if (editing == null) "Şube ekle" else "Şubeyi düzenle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(420.dp).verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value.name, { draft = value.copy(name = it) }, label = { Text("Şube adı") }, singleLine = true)
                    OutlinedTextField(value.code, { draft = value.copy(code = it) }, label = { Text("Şube kodu") }, singleLine = true)
                    OutlinedTextField(value.city, { draft = value.copy(city = it) }, label = { Text("Şehir") }, singleLine = true)
                    OutlinedTextField(value.address, { draft = value.copy(address = it) }, label = { Text("Adres") }, minLines = 2)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value.opening, { draft = value.copy(opening = it.take(5)) }, label = { Text("Açılış · SS:DD") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
                        OutlinedTextField(value.closing, { draft = value.copy(closing = it.take(5)) }, label = { Text("Kapanış · SS:DD") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { TextButton(enabled = !busy, onClick = { saveDraft() }) { Text(if (busy) "Kaydediliyor…" else "Kaydet") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { draft = null; editing = null }) { Text("Vazgeç") } },
            shape = RoundedCornerShape(24.dp),
            containerColor = White
        )
    }
}
