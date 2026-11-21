package com.lukasoft.lukarest.ui.screens.boss

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.lukasoft.lukarest.data.api.ApiClient
import com.lukasoft.lukarest.data.api.AssignStaffBody
import com.lukasoft.lukarest.data.api.BranchDto
import com.lukasoft.lukarest.data.api.SwitchBranchCommand
import com.lukasoft.lukarest.data.api.ChangeStaffRoleBody
import com.lukasoft.lukarest.data.api.StaffDetailDto
import com.lukasoft.lukarest.data.api.StaffItemDto
import com.lukasoft.lukarest.data.store.AuthStore
import com.lukasoft.lukarest.ui.components.PullToRefreshBox
import com.lukasoft.lukarest.ui.components.LukaEmptyState
import com.lukasoft.lukarest.ui.components.LukaErrorState
import com.lukasoft.lukarest.ui.components.LukaLoadingState
import com.lukasoft.lukarest.ui.components.LukaCard
import com.lukasoft.lukarest.ui.components.StatusPill
import com.lukasoft.lukarest.ui.components.TopPill
import com.lukasoft.lukarest.ui.theme.BgScreenTop
import com.lukasoft.lukarest.ui.theme.BossGradient
import com.lukasoft.lukarest.ui.theme.BossGradStart
import com.lukasoft.lukarest.ui.theme.BorderSoft
import com.lukasoft.lukarest.ui.theme.PillNeutral
import com.lukasoft.lukarest.ui.theme.PillOk
import com.lukasoft.lukarest.ui.theme.TextMuted
import com.lukasoft.lukarest.ui.theme.White
import kotlinx.coroutines.launch
import java.io.IOException

private val staffRoleLabels = mapOf(
    "Owner" to "Patron", "Waiter" to "Garson", "Barista" to "Barista", "Bartender" to "Barmen",
    "KitchenStaff" to "Mutfak", "ShiftSupervisor" to "Vardiya sorumlusu", "GeneralManager" to "Müdür"
)

@Composable
fun StaffManagementScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val globalRole by AuthStore.globalRole.collectAsState()
    val branchId by AuthStore.activeBranchId.collectAsState()
    val service = ApiClient.staffAdminService
    var includeInactive by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    var staff by remember(branchId) { mutableStateOf(emptyList<StaffItemDto>()) }
    var detail by remember(branchId) { mutableStateOf<StaffDetailDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var branches by remember { mutableStateOf(emptyList<BranchDto>()) }
    var busy by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(true) }

    fun perform(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                action()
            } catch (_: IOException) {
                error = "Sunucuya ulaşılamadı."
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(branchId, includeInactive, revision) {
        isRefreshing = true
        error = null
        try {
            val result = service.getStaff(includeInactive)
            if (result.isSuccessful) staff = result.body().orEmpty()
            else error = "Personel listesi alınamadı."
        } catch (_: IOException) {
            error = "Sunucuya ulaşılamadı."
        } finally {
            isRefreshing = false
        }
    }

    Scaffold(
        containerColor = BgScreenTop,
        topBar = {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                TopPill(
                    title = "Personel yönetimi",
                    subtitle = AuthStore.activeBranchName.collectAsState().value.orEmpty(),
                    initials = "P",
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
        onRefresh = { revision++ },
        modifier = Modifier.fillMaxSize().padding(padding)
      ) {
       LazyColumn(
           contentPadding = PaddingValues(16.dp),
           verticalArrangement = Arrangement.spacedBy(10.dp)
       ) {
        item {
            LukaCard(elevation = 0.dp, contentPadding = PaddingValues(0.dp)) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        .toggleable(value = includeInactive, role = Role.Checkbox, onValueChange = { includeInactive = it })
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Pasif personeli göster", modifier = Modifier.weight(1f))
                    Checkbox(includeInactive, onCheckedChange = null)
                }
            }
        }
        if (detail == null) error?.let { message ->
            item { LukaErrorState(message, onRetry = { revision++ }) }
        }
        if (busy && detail == null) {
            item { LukaLoadingState("Personel bilgileri yükleniyor.") }
        }
        if (staff.isEmpty() && error == null) {
            item {
                if (isRefreshing) LukaLoadingState("Personel listesi yükleniyor.")
                else LukaEmptyState("Personel bulunamadı", "Bu şubede filtreye uyan personel yok.")
            }
        }
        items(staff, key = { it.userId }) { person ->
            OutlinedCard(enabled = !busy, onClick = {
                perform {
                    val result = service.getStaffDetail(person.userId)
                    if (result.isSuccessful) detail = result.body()
                    else error = "Personel kartı alınamadı."
                }
            }, modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = White),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSoft)
            ) {
                ListItem(
                    headlineContent = { Text(person.fullName, color = BossGradStart, style = MaterialTheme.typography.titleMedium) },
                    supportingContent = {
                        person.roleInBranch?.let { Text(staffRoleLabels[it] ?: it, color = TextMuted) }
                        StatusPill(if (person.isActive) "Aktif" else "Pasif",
                            pillColor = if (person.isActive) PillOk else PillNeutral)
                    },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = BossGradStart) },
                    colors = ListItemDefaults.colors(containerColor = White)
                )
            }
        }
       }
      }
    }

    detail?.let { card ->
        val person = card.staff
        val roles = staffRoleLabels.filterKeys { it != "Owner" }.toList()
        var choosingRole by remember(person.userId) { mutableStateOf(false) }
        var targetBranch by remember(person.userId) { mutableStateOf<BranchDto?>(null) }
        var makePrimary by remember(person.userId) { mutableStateOf(false) }
        AlertDialog(
            containerColor = White,
            onDismissRequest = { if (!busy) detail = null },
            title = { Text(person.fullName) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text(person.phoneNumber, color = TextMuted)
                        Spacer(Modifier.height(12.dp))
                        Text("Şube atamaları", style = MaterialTheme.typography.titleSmall, color = BossGradStart)
                    }
                    if (card.assignments.isEmpty()) item { Text("Şube ataması yok.", color = TextMuted) }
                    items(card.assignments) { assignment ->
                        val role = assignment.roleInBranch
                        Text(listOfNotNull(assignment.branchName, role?.let { staffRoleLabels[it] ?: it }).joinToString(" · "))
                    }
                    if (globalRole == "CompanyOwner") item {
                        HorizontalDivider(color = BorderSoft)
                        TextButton(enabled = !busy, onClick = {
                            targetBranch = null
                            choosingRole = !choosingRole
                        }) { Text("Rol değiştir") }
                        TextButton(enabled = !busy, onClick = {
                            perform {
                                val result = ApiClient.branchService.getBranches()
                                if (result.isSuccessful) branches = result.body().orEmpty().filter { it.isActive }
                                else error = "Şubeler alınamadı."
                            }
                        }) { Text("Şubeye ata") }
                    }
                    items(branches.filter { branch -> card.assignments.none { it.branchId == branch.id } }) { branch ->
                        TextButton(enabled = !busy, onClick = {
                            targetBranch = branch
                            choosingRole = true
                        }) { Text("Ata: " + branch.name) }
                    }
                    if (targetBranch != null) item {
                        Row {
                            Checkbox(makePrimary, { makePrimary = it }, enabled = !busy)
                            Text("Birincil şube yap")
                        }
                    }
                    if (choosingRole) items(roles) { (code, label) ->
                        TextButton(enabled = !busy, onClick = {
                            perform {
                                val target = targetBranch
                                if (target != null && target.id != AuthStore.activeBranchId.value) {
                                    val refresh = AuthStore.refreshToken.value
                                    val tokens = refresh?.let {
                                        ApiClient.authService.switchBranch(SwitchBranchCommand(target.id, it)).body()
                                    }
                                    if (tokens == null) {
                                        error = "Hedef şubeye geçilemedi."
                                        return@perform
                                    }
                                    AuthStore.setTokens(tokens.accessToken, tokens.refreshToken)
                                    AuthStore.updateActiveBranch(target.id, target.name)
                                }
                                val result = if (target != null)
                                    service.assignStaff(person.userId, AssignStaffBody(code, makePrimary))
                                else service.changeRole(person.userId, ChangeStaffRoleBody(code))
                                if (result.isSuccessful) {
                                    detail = null
                                    revision++
                                } else error = "Personel işlemi tamamlanamadı (${result.code()})."
                            }
                        }) { Text(label) }
                    }
                    if (globalRole == "CompanyOwner") item {
                        if (person.primaryBranchId == branchId) {
                            TextButton(enabled = !busy, onClick = {
                                perform {
                                    val result = if (person.isActive) service.deactivateStaff(person.userId)
                                        else service.activateStaff(person.userId)
                                    if (result.isSuccessful) {
                                        detail = null
                                        revision++
                                    } else error = "Personel durumu değiştirilemedi (${result.code()})."
                                }
                            }) { Text(if (person.isActive) "Pasife al" else "Etkinleştir") }
                        } else {
                            TextButton(enabled = !busy, onClick = {
                                perform {
                                    val result = service.unassignStaff(person.userId)
                                    if (result.isSuccessful) {
                                        detail = null
                                        revision++
                                    } else error = "Atama kaldırılamadı (${result.code()})."
                                }
                            }) { Text("Bu şubeden çıkar") }
                        }
                    }
                    item {
                        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !busy, onClick = { detail = null }) { Text("Kapat") }
            }
        )
    }
}

