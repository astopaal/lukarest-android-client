package com.lukasoft.lukarest.ui.screens.boss

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukasoft.lukarest.data.api.ApiClient
import com.lukasoft.lukarest.data.api.CreateOperationalNoteCommand
import com.lukasoft.lukarest.data.api.OperationalNoteDto
import com.lukasoft.lukarest.data.util.DateUtils
import com.lukasoft.lukarest.ui.components.LukaCard
import com.lukasoft.lukarest.ui.components.PrimaryButton
import com.lukasoft.lukarest.ui.components.PullToRefreshBox
import com.lukasoft.lukarest.ui.components.TopPill
import com.lukasoft.lukarest.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OperationalNotesScreen(
    onNavigateBack: () -> Unit
) {
    val me by com.lukasoft.lukarest.data.store.AuthStore.me.collectAsState()
    val role by com.lukasoft.lukarest.data.store.AuthStore.role.collectAsState()
    var editing by remember { mutableStateOf<OperationalNoteDto?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val todayIso = remember { DateUtils.getTurkeyTodayIso() }

    var selectedTab by remember { mutableStateOf("Reservation") } // "Reservation", "DailyNotice", "HandoverNote"
    var notes by remember { mutableStateOf<List<OperationalNoteDto>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Dialog state
    var inputNoteContent by remember { mutableStateOf("") }
    var inputGuestName by remember { mutableStateOf("") }
    var inputGuestPhone by remember { mutableStateOf("") }
    var inputGuestCount by remember { mutableStateOf("2") }
    var inputTableNo by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    fun refreshNotes() {
        coroutineScope.launch {
            isRefreshing = true
            try {
                val res = ApiClient.operationalNotesService.getNotes(date = todayIso, category = selectedTab)
                if (res.isSuccessful && res.body() != null) {
                    notes = res.body()!!
                }
            } catch (_: Exception) {
            } finally {
                delay(300)
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(selectedTab) {
        refreshNotes()
    }

    fun handleCreateNote() {
        if (inputNoteContent.trim().length < 3) {
            Toast.makeText(context, "Not en az 3 karakter olmalıdır", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedTab == "Reservation") {
            if (inputGuestName.isBlank() || inputGuestPhone.isBlank()) {
                Toast.makeText(context, "Rezervasyon için ad ve telefon zorunludur", Toast.LENGTH_SHORT).show()
                return
            }
        }

        coroutineScope.launch {
            isSaving = true
            try {
                val command = CreateOperationalNoteCommand(
                    category = selectedTab,
                    targetDate = editing?.targetDate ?: todayIso,
                    guestName = if (selectedTab == "Reservation") inputGuestName.trim() else null,
                    guestPhone = if (selectedTab == "Reservation") inputGuestPhone.trim() else null,
                    guestCount = if (selectedTab == "Reservation") inputGuestCount.toIntOrNull() ?: 1 else null,
                    tableNo = inputTableNo.ifBlank { null },
                    noteContent = inputNoteContent.trim()
                )
                val service = ApiClient.operationalNotesService
                val noteId = editing?.id
                val res = if (noteId == null) service.createNote(command) else service.updateNote(noteId, command)
                if (res.isSuccessful) {
                    Toast.makeText(context, "Kayıt başarıyla eklendi", Toast.LENGTH_SHORT).show()
                    showAddDialog = false
                    inputNoteContent = ""
                    inputGuestName = ""
                    inputGuestPhone = ""
                    inputTableNo = ""
                    refreshNotes()
                } else {
                    Toast.makeText(context, "Eklenemedi: ${res.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Hata: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isSaving = false
            }
        }
    }

    fun handleDeleteNote(id: String) {
        coroutineScope.launch {
            try {
                val res = ApiClient.operationalNotesService.deleteNote(id)
                if (res.isSuccessful) {
                    Toast.makeText(context, "Kayıt silindi", Toast.LENGTH_SHORT).show()
                    refreshNotes()
                }
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                TopPill(
                    title = "Operasyon Defteri",
                    subtitle = "Rezervasyon & Günlük Notlar",
                    initials = "OD",
                    gradient = BossGradient,
                    actionIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onActionClick = onNavigateBack
                )
            }
        },
        containerColor = BgScreenTop
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { refreshNotes() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Kategori Sekmeleri
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Reservation" to "Rezervasyon",
                        "DailyNotice" to "Duyurular",
                        "HandoverNote" to "Vardiya Devir"
                    ).forEach { (cat, label) ->
                        val isSelected = selectedTab == cat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) BossGradStart else White)
                                .clickable { selectedTab = cat },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) White else TextMain
                            )
                        }
                    }
                }

                // Yeni Ekle Butonu
                PrimaryButton(
                    text = when (selectedTab) {
                        "Reservation" -> "+ Yeni Rezervasyon Ekle"
                        "DailyNotice" -> "+ Yeni Duyuru Ekle"
                        else -> "+ Devir Teslim Notu Ekle"
                    },
                    onClick = {
                        editing = null
                        inputNoteContent = ""
                        inputGuestName = ""
                        inputGuestPhone = ""
                        inputGuestCount = "2"
                        inputTableNo = ""
                        showAddDialog = true
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    gradient = BossGradient
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Liste
                if (notes.isEmpty() && !isRefreshing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Bugün için kayıt bulunamadı",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(notes) { note ->
                            LukaCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        if (!note.guestName.isNullOrBlank()) {
                                            Text(
                                                text = "${note.guestName} (${note.guestCount ?: 1} Kişi)",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextMain
                                            )
                                            Text(
                                                text = "${note.guestPhone ?: ""} ${if (!note.tableNo.isNullOrBlank()) "• Masa: ${note.tableNo}" else ""}",
                                                fontSize = 12.sp,
                                                color = StaffGradStart,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = note.noteContent,
                                            fontSize = 13.sp,
                                            color = TextSoft
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Ekleyen: ${note.authorFullName}",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }

                                    val canEdit = note.authorUserId == me?.userId ||
                                        role == com.lukasoft.lukarest.data.store.AppRole.BOSS
                                    if (canEdit) TextButton(onClick = {
                                        editing = note
                                        inputNoteContent = note.noteContent
                                        inputGuestName = note.guestName.orEmpty()
                                        inputGuestPhone = note.guestPhone.orEmpty()
                                        inputGuestCount = note.guestCount?.toString().orEmpty()
                                        inputTableNo = note.tableNo.orEmpty()
                                        showAddDialog = true
                                    }) { Text("Düzenle") }
                                    if (canEdit) IconButton(
                                        onClick = { handleDeleteNote(note.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Sil",
                                            tint = PillErr.fg,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showAddDialog = false },
            title = {
                Text(
                    text = when (selectedTab) {
                        "Reservation" -> "Yeni Rezervasyon"
                        "DailyNotice" -> "Yeni Günlük Duyuru"
                        else -> "Vardiya Devir Notu"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (selectedTab == "Reservation") {
                        OutlinedTextField(
                            value = inputGuestName,
                            onValueChange = { inputGuestName = it },
                            label = { Text("Müşteri Adı") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = inputGuestPhone,
                            onValueChange = { inputGuestPhone = it },
                            label = { Text("Telefon (+905...)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = inputGuestCount,
                                onValueChange = { inputGuestCount = it },
                                label = { Text("Kişi") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            OutlinedTextField(
                                value = inputTableNo,
                                onValueChange = { inputTableNo = it },
                                label = { Text("Masa No") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = inputNoteContent,
                        onValueChange = { inputNoteContent = it },
                        label = { Text("Not Açıklaması") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { handleCreateNote() },
                    colors = ButtonDefaults.buttonColors(containerColor = BossGradStart),
                    enabled = !isSaving
                ) {
                    Text(text = if (isSaving) "Kaydediliyor..." else "Kaydet")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    enabled = !isSaving
                ) {
                    Text("İptal")
                }
            }
        )
    }
}


