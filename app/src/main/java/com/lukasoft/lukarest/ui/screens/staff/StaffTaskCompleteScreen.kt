package com.lukasoft.lukarest.ui.screens.staff

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukasoft.lukarest.data.api.ApiClient
import com.lukasoft.lukarest.data.api.CompleteTaskBody
import com.lukasoft.lukarest.data.model.TaskStatus
import com.lukasoft.lukarest.ui.components.LukaCard
import com.lukasoft.lukarest.ui.components.PrimaryButton
import com.lukasoft.lukarest.ui.components.TopPill
import com.lukasoft.lukarest.ui.theme.*
import com.lukasoft.lukarest.ui.viewmodel.LukaViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

private const val MAX_PROOF_BYTES = 20L * 1024L * 1024L

@Composable
fun StaffTaskCompleteScreen(
    taskId: String,
    viewModel: LukaViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tasks by viewModel.tasks.collectAsState()
    val task = tasks.find { it.id == taskId }
    var note by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaKind by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedUri = uri
        selectedMediaKind = uri?.let { "photo" }
        errorMessage = null
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedUri = uri
        selectedMediaKind = uri?.let { "video" }
        errorMessage = null
    }

    fun submitCompletion() {
        val currentTask = task
        if (currentTask == null) {
            errorMessage = "Görev bilgisi bulunamadı. Görev listesini yenileyip tekrar deneyin."
            return
        }
        if (isSubmitting) return
        scope.launch {
            isSubmitting = true
            errorMessage = null
            try {
                var proofIds = emptyList<String>()
                if (currentTask.mediaRequired) {
                    val uri = selectedUri
                    if (uri == null) {
                        errorMessage = "Bu görevi tamamlamak için kanıt dosyası seçin."
                        return@launch
                    }
                    val media = readProofFile(context, uri)
                    if (media == null) {
                        errorMessage = "Dosya okunamadı veya 20 MB sınırını aşıyor."
                        return@launch
                    }
                    val body = media.bytes.toRequestBody(media.mimeType.toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("file", media.fileName, body)
                    val upload = ApiClient.tasksService.uploadTaskMedia(currentTask.id, part)
                    if (!upload.isSuccessful || upload.body() == null) {
                        errorMessage = "Kanıt yüklenemedi (${upload.code()}): ${upload.errorBody()?.string()?.take(220).orEmpty()}"
                        return@launch
                    }
                    proofIds = listOf(upload.body()!!.id)
                }

                val complete = ApiClient.tasksService.completeTask(
                    currentTask.id,
                    CompleteTaskBody(completionNote = note.trim().ifBlank { null }, mediaProofIds = proofIds)
                )
                if (!complete.isSuccessful) {
                    errorMessage = "Görev tamamlanamadı (${complete.code()}): ${complete.errorBody()?.string()?.take(220).orEmpty()}"
                    return@launch
                }
                viewModel.updateTask(currentTask.copy(status = TaskStatus.DONE, hasMedia = currentTask.mediaRequired || currentTask.hasMedia))
                Toast.makeText(context, "Görev tamamlandı", Toast.LENGTH_SHORT).show()
                onNavigateBack()
            } catch (e: Exception) {
                errorMessage = "İşlem başarısız: ${e.localizedMessage}"
            } finally {
                isSubmitting = false
            }
        }
    }

    Scaffold(
        topBar = {
            Box(modifier = Modifier.padding(16.dp)) {
                TopPill(
                    title = "Görevi Tamamla",
                    subtitle = task?.title ?: "Görev Detay",
                    initials = "AY",
                    gradient = StaffGradient,
                    actionIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onActionClick = onNavigateBack
                )
            }
        },
        containerColor = BgScreenTop
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LukaCard {
                Text(task?.title ?: "", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
                task?.description?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(it, fontSize = 14.sp, color = TextSoft)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Görev saati: ${task?.startTime.orEmpty()}${task?.endTime?.let { " — $it" }.orEmpty()}",
                    fontSize = 13.sp,
                    color = TextMuted
                )
            }

            if (task?.mediaRequired == true) {
                LukaCard {
                    Text("Kanıt dosyası zorunlu", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextMain)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Fotoğraf seç")
                        }
                        OutlinedButton(onClick = { videoPicker.launch("video/*") }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Video seç")
                        }
                    }
                    selectedUri?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Seçilen ${if (selectedMediaKind == "video") "video" else "fotoğraf"}: ${displayName(context, it)}", color = StaffGradStart, fontSize = 12.sp)
                    }
                    Text("JPG, PNG, WEBP veya MP4/MOV · en fazla 20 MB", color = TextMuted, fontSize = 11.sp)
                }
            }

            LukaCard {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(2000) },
                    label = { Text("Tamamlama notu (isteğe bağlı)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    maxLines = 5
                )
            }

            errorMessage?.let { Text(it, color = Color(0xFFDC2626), fontSize = 13.sp) }
            Spacer(modifier = Modifier.weight(1f))
            PrimaryButton(
                text = if (isSubmitting) "Gönderiliyor..." else "Görevi Tamamla",
                onClick = { submitCompletion() },
                enabled = !isSubmitting,
                gradient = StaffGradient
            )
        }
    }
}

private data class ProofFile(val fileName: String, val mimeType: String, val bytes: ByteArray)

private fun readProofFile(context: Context, uri: Uri): ProofFile? = runCatching {
    val size = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
    if (size != null && size > MAX_PROOF_BYTES) return null
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    if (bytes.isEmpty() || bytes.size.toLong() > MAX_PROOF_BYTES) return null
    val rawMime = context.contentResolver.getType(uri)?.lowercase() ?: return null
    val (mime, extension) = when (rawMime) {
        "image/jpeg", "image/jpg" -> "image/jpeg" to ".jpg"
        "image/png" -> "image/png" to ".png"
        "image/webp" -> "image/webp" to ".webp"
        "video/mp4" -> "video/mp4" to ".mp4"
        "video/quicktime" -> "video/quicktime" to ".mov"
        else -> return null
    }
    val chosenName = displayName(context, uri).ifBlank { "kanıt$extension" }
    val fileName = if (chosenName.endsWith(extension, ignoreCase = true)) chosenName
        else chosenName.substringBeforeLast('.', chosenName) + extension
    ProofFile(fileName, mime, bytes)
}.getOrNull()

private fun displayName(context: Context, uri: Uri): String = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else ""
    }.orEmpty()
}.getOrDefault("")
