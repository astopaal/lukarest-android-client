package com.lukasoft.lukarest.ui.screens.boss

import com.lukasoft.lukarest.ui.components.LukaBottomSheet

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.lukasoft.lukarest.data.api.*
import com.lukasoft.lukarest.data.store.AuthStore
import com.lukasoft.lukarest.data.util.DateUtils
import com.lukasoft.lukarest.ui.components.*
import com.lukasoft.lukarest.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale

private val noteCategories = linkedMapOf("Reservation" to "Rezervasyon", "DailyNotice" to "Duyuru", "HandoverNote" to "Devir teslim")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationalNotesScreen(onNavigateBack: () -> Unit, initialNoteId: String? = null, service: OperationalNotesService = ApiClient.operationalNotesService) {
    val me by AuthStore.me.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val today = DateUtils.getTurkeyTodayIso()
    var date by remember { mutableStateOf(today) }
    var category by remember { mutableStateOf("DailyNotice") }
    var notes by remember { mutableStateOf<List<OperationalNoteDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var highlightedId by remember(initialNoteId) { mutableStateOf(initialNoteId) }
    var resolvingLink by remember(initialNoteId) { mutableStateOf(initialNoteId != null) }
    var linkError by remember(initialNoteId) { mutableStateOf<String?>(null) }
    var linkRetry by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<OperationalNoteDto?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<OperationalNoteDto?>(null) }
    var deleteBusy by remember { mutableStateOf(false) }

    LaunchedEffect(initialNoteId, linkRetry) {
        if (initialNoteId == null) return@LaunchedEffect
        resolvingLink = true
        linkError = null
        try {
            val response = service.getNote(initialNoteId)
            val note = response.body()
            if (response.isSuccessful && note != null) { date = note.targetDate; category = note.category }
            else linkError = "Bildirimin bağlı olduğu not açılamadı. Silinmiş olabilir veya erişiminiz olmayabilir."
        } catch (e: CancellationException) { throw e
        } catch (_: IOException) { linkError = "Not açılamadı. Bağlantınızı kontrol edin."
        } finally { resolvingLink = false }
    }
    LaunchedEffect(date, refreshKey, resolvingLink) {
        if (resolvingLink) return@LaunchedEffect
        loading = true
        error = null
        notes = emptyList()
        try {
            val response = service.getNotes(date = date)
            if (response.isSuccessful) notes = response.body().orEmpty()
            else error = "Kayıtlar yüklenemedi (${response.code()})."
        } catch (e: CancellationException) { throw e
        } catch (_: IOException) { error = "Sunucuya ulaşılamadı. Bağlantınızı kontrol edin."
        } finally { loading = false }
    }
    val visible = notes.filter { it.category == category }.sortedWith(compareByDescending<OperationalNoteDto> { it.id == highlightedId }.thenByDescending { it.createdAtUtc })
    val userId = me?.userId
    val manager = me?.globalRole == "CompanyOwner" || me?.roleInBranch in listOf("Owner", "GeneralManager", "ShiftSupervisor")
    fun chooseDate() {
        val parts = date.split('-').map { it.toInt() }
        DatePickerDialog(context, { _, year, month, day ->
            date = String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, day)
        }, parts[0], parts[1] - 1, parts[2]).show()
    }
    Scaffold(
        topBar = { Box(Modifier.padding(16.dp)) {
            TopPill(title = "Operasyon defteri", subtitle = me?.activeBranchName ?: "Ekibin ortak notları", initials = "OD",
                gradient = BossGradient, actionIcon = Icons.AutoMirrored.Filled.ArrowBack, onActionClick = onNavigateBack)
        } }, containerColor = BgScreenTop
    ) { padding ->
        PullToRefreshBox(isRefreshing = loading || resolvingLink, onRefresh = { refreshKey++ }, modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { chooseDate() }, modifier = Modifier.weight(1f)) { Text(if (date == today) "Bugün · $date" else date) }
                        if (date != today) TextButton(onClick = { date = today }) { Text("Bugüne dön") }
                    }
                }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        noteCategories.forEach { (code, label) ->
                            FilterChip(selected = category == code, onClick = { category = code },
                                colors = FilterChipDefaults.filterChipColors(containerColor = White, labelColor = TextMain,
                                    selectedContainerColor = PillBlue.bg, selectedLabelColor = PillBlue.fg), label = {
                                Text("$label (${notes.count { it.category == code }})", color = if (category == code) PillBlue.fg else TextMain)
                            })
                        }
                    }
                }
                item {
                    PrimaryButton(text = "${noteCategories[category]} ekle", onClick = { editing = null; editorOpen = true }, modifier = Modifier.fillMaxWidth(), gradient = BossGradient)
                }
                linkError?.let { message -> item {
                    Text(message, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { linkRetry++ }) { Text("Notu tekrar aç") }
                } }
                when {
                    loading || resolvingLink -> item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    error != null -> item {
                        Text(error!!, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { refreshKey++ }) { Text("Tekrar dene") }
                    }
                    visible.isEmpty() -> item { LukaCard {
                        Text("Bu tarihte ${noteCategories[category]?.lowercase()} yok", style = MaterialTheme.typography.titleMedium, color = TextMain)
                        Text("Yeni bir kayıt ekleyin. İlgili kişilere haber vermek için notta @ kullanabilirsiniz.", color = TextMuted)
                    } }
                    else -> items(visible, key = { it.id }) { note ->
                        LukaCard {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusPill(noteCategories[note.category].orEmpty(), pillColor = PillNeutral)
                                if (userId in note.mentionedUserIds.orEmpty()) StatusPill("Senden bahsedildi", pillColor = PillBlue)
                                if (note.id == highlightedId) StatusPill("Bildirimdeki not", pillColor = PillWarn)
                            }
                            if (note.category == "Reservation") {
                                Spacer(Modifier.height(8.dp))
                                Text("${note.guestName} · ${note.guestCount} kişi", style = MaterialTheme.typography.titleMedium, color = TextMain)
                                Text(listOfNotNull(note.guestPhone, note.tableNo?.let { "Masa $it" }).joinToString(" · "), color = TextMuted)
                            }
                            Spacer(Modifier.height(10.dp))
                            NoteContent(note.noteContent)
                            Spacer(Modifier.height(12.dp))
                            Text(note.authorFullName, color = TextMain, style = MaterialTheme.typography.labelLarge)
                            Text(DateUtils.formatUtcTimestamp(note.createdAtUtc), color = TextMuted, style = MaterialTheme.typography.bodySmall)
                            if (note.branchId == me?.activeBranchId && (note.authorUserId == userId || manager)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { editing = note; editorOpen = true }) { Text("Düzenle") }
                                    IconButton(onClick = { deleting = note }) { Icon(Icons.Default.Delete, contentDescription = "Notu sil", tint = PillErr.fg) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (editorOpen) NoteEditor(editing, category, date, service, onClose = { editorOpen = false }, onSaved = { editorOpen = false; refreshKey++ })
    deleting?.let { note ->
        AlertDialog(onDismissRequest = { if (!deleteBusy) deleting = null }, title = { Text("Bu not silinsin mi?") },
            text = { Text("Not defterden kaldırılacak. Bu işlem geri alınamaz.") },
            confirmButton = { TextButton(enabled = !deleteBusy, onClick = {
                scope.launch {
                    deleteBusy = true
                    try {
                        val response = service.deleteNote(note.id)
                        if (response.isSuccessful) { deleting = null; refreshKey++; LukaFeedback.show("Not silindi") }
                        else LukaFeedback.show("Not silinemedi (${response.code()}).")
                    } catch (e: CancellationException) { throw e
                    } catch (_: IOException) { LukaFeedback.show("Sunucuya ulaşılamadı.")
                    } finally { deleteBusy = false }
                }
            }) { Text(if (deleteBusy) "Siliniyor…" else "Notu sil", color = PillErr.fg) } },
            dismissButton = { TextButton(enabled = !deleteBusy, onClick = { deleting = null }) { Text("Vazgeç") } })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditor(note: OperationalNoteDto?, category: String, date: String, service: OperationalNotesService, onClose: () -> Unit, onSaved: () -> Unit) {
    var content by remember { mutableStateOf(TextFieldValue(note?.noteContent.orEmpty(), TextRange(note?.noteContent?.length ?: 0))) }
    var guest by remember { mutableStateOf(note?.guestName.orEmpty()) }
    var phone by remember { mutableStateOf(note?.guestPhone.orEmpty()) }
    var count by remember { mutableStateOf(note?.guestCount?.toString() ?: "2") }
    var table by remember { mutableStateOf(note?.tableNo.orEmpty()) }
    var people by remember { mutableStateOf<List<NoteMentionableUserDto>>(emptyList()) }
    var peopleLoading by remember { mutableStateOf(true) }
    var peopleError by remember { mutableStateOf<String?>(null) }
    var peopleRetry by remember { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var discard by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val changed = content.text != note?.noteContent.orEmpty() || guest != note?.guestName.orEmpty() || phone != note?.guestPhone.orEmpty() ||
        table != note?.tableNo.orEmpty() || count != (note?.guestCount?.toString() ?: "2")
    fun dismiss() { if (!saving) { if (changed) discard = true else onClose() } }
    LaunchedEffect(peopleRetry) {
        peopleLoading = true
        peopleError = null
        try {
            val response = service.getMentionableUsers()
            if (response.isSuccessful) people = response.body().orEmpty() else peopleError = "Kişiler yüklenemedi."
        } catch (e: CancellationException) { throw e
        } catch (_: IOException) { peopleError = "Kişiler yüklenemedi. Bağlantınızı kontrol edin."
        } finally { peopleLoading = false }
    }
    LukaBottomSheet(onDismissRequest = { dismiss() }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
        confirmValueChange = { value ->
            if (saving) false else if (value == SheetValue.Hidden && changed) { discard = true; false } else true
        }), containerColor = BgScreenTop) {
        Column(Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.82f).dp).imePadding().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (note == null) "${noteCategories[category]} ekle" else "Notu düzenle", style = MaterialTheme.typography.titleLarge, color = TextMain)
            Text("$date · @ ile ekipten birine haber verin", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (category == "Reservation") {
                    OutlinedTextField(guest, { guest = it }, label = { Text("Misafir adı") }, enabled = !saving, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(phone, { phone = it }, label = { Text("Telefon (+905…) ") }, enabled = !saving, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(count, { count = it }, label = { Text("Kişi sayısı") }, enabled = !saving, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(table, { table = it }, label = { Text("Masa") }, enabled = !saving, singleLine = true, modifier = Modifier.weight(1f))
                    }
                }
                NoteMentionField(content, { content = it }, people, peopleLoading, peopleError, { peopleRetry++ }, enabled = !saving)
                saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { dismiss() }, enabled = !saving, modifier = Modifier.weight(1f)) { Text("Vazgeç") }
                Button(onClick = {
                    scope.launch {
                        saving = true
                        saveError = null
                        try {
                            val command = CreateOperationalNoteCommand(category, note?.targetDate ?: date,
                                guestName = guest.trim().takeIf { category == "Reservation" }, guestPhone = phone.trim().takeIf { category == "Reservation" },
                                guestCount = count.toIntOrNull().takeIf { category == "Reservation" }, tableNo = table.trim().ifBlank { null },
                                noteContent = content.text.trim(), mentionedUserIds = noteMentionIds(content.text, people))
                            val response = if (note == null) service.createNote(command) else service.updateNote(note.id, command)
                            if (response.isSuccessful) { LukaFeedback.show(if (note == null) "Not eklendi" else "Not güncellendi"); onSaved() }
                            else saveError = apiErrorMessage(response.errorBody()?.string(), "Not kaydedilemedi. Alanları kontrol edin (${response.code()}).")
                        } catch (e: CancellationException) { throw e
                        } catch (_: IOException) { saveError = "Sunucuya ulaşılamadı. Yazdıklarınız korunuyor."
                        } finally { saving = false }
                    }
                }, enabled = !saving && !peopleLoading && peopleError == null && content.text.trim().length in 3..2000 &&
                    (category != "Reservation" || (guest.isNotBlank() && phone.isNotBlank() && (count.toIntOrNull() ?: 0) >= 1)), modifier = Modifier.weight(1f)) {
                    Text(if (saving) "Kaydediliyor…" else "Kaydet", color = LocalContentColor.current)
                }
            }
            if (peopleError != null) TextButton(onClick = { peopleRetry++ }, enabled = !saving) { Text("Kaydetmek için kişi listesini tekrar yükle") }
            Spacer(Modifier.height(12.dp))
        }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Değişikliklerden vazgeçilsin mi?") },
        text = { Text("Kaydedilmemiş değişiklikler silinecek.") },
        confirmButton = { TextButton(onClick = onClose) { Text("Değişiklikleri sil") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Yazmaya devam et") } })
}
