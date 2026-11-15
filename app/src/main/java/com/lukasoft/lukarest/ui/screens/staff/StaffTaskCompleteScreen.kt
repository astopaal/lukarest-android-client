package com.lukasoft.lukarest.ui.screens.staff

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukasoft.lukarest.data.api.ApiClient
import com.google.gson.JsonParser
import com.lukasoft.lukarest.data.api.toTaskItem
import com.lukasoft.lukarest.data.api.CompleteTaskBody
import com.lukasoft.lukarest.data.model.TaskStatus
import com.lukasoft.lukarest.ui.components.LukaCard
import com.lukasoft.lukarest.ui.components.PrimaryButton
import com.lukasoft.lukarest.ui.components.TopPill
import com.lukasoft.lukarest.ui.components.LukaFeedback
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
    val canSubmit = task != null && task.status !in listOf(TaskStatus.DONE, TaskStatus.AWAITING_APPROVAL)
    var note by remember { mutableStateOf("") }
    var selectedUriText by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedUri = selectedUriText?.let(Uri::parse)
    var selectedMediaKind by rememberSaveable { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var refreshRequired by remember { mutableStateOf(false) }

    suspend fun refreshTasks(): Boolean {
        val response = ApiClient.tasksService.getMyTasks()
        val latest = response.body()
        if (!response.isSuccessful || latest == null) return false
        viewModel.setTasks(latest.tasks.map { it.toTaskItem("me") })
        return true
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var cameraUriText by rememberSaveable { mutableStateOf<String?>(null) }
    val cameraUri = cameraUriText?.let(Uri::parse)
    var askCameraPermission by remember { mutableStateOf(false) }
    var captureVideo by rememberSaveable { mutableStateOf(false) }

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) askCameraPermission = true
        else errorMessage = "Fotoğraf çekmek için kamera izni gerekli."
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val uri = cameraUri
        if (captured && uri != null) {
            selectedUriText = uri.toString()
            selectedMediaKind = "photo"
            errorMessage = null
        }
    }
    val videoCamera = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { captured ->
        if (captured && cameraUri != null) {
            selectedUriText = cameraUri.toString()
            selectedMediaKind = "video"
            errorMessage = null
        }
    }
    LaunchedEffect(askCameraPermission) {
        if (askCameraPermission) {
            askCameraPermission = false
            val file = java.io.File(context.cacheDir, "task-proofs").apply { mkdirs() }
            val photo = java.io.File.createTempFile("proof_", if (captureVideo) ".mp4" else ".jpg", file)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photo)
            cameraUriText = uri.toString()
            try {
                if (captureVideo) videoCamera.launch(uri) else camera.launch(uri)
            } catch (_: android.content.ActivityNotFoundException) {
                errorMessage = "Bu cihazda kamera uygulaması bulunamadı."
            }
        }
    }

    fun submitCompletion() {
        val currentTask = task
        if (currentTask == null) {
            errorMessage = "Görev bilgisi bulunamadı. Görev listesini yenileyip tekrar deneyin."
            return
        }
        if (isSubmitting || refreshRequired || currentTask.status in listOf(TaskStatus.DONE, TaskStatus.AWAITING_APPROVAL)) return
        scope.launch {
            isSubmitting = true
            errorMessage = null
            try {
                var proofIds = emptyList<String>()
                if (currentTask.mediaRequired || currentTask.requiresApproval) {
                    val uri = selectedUri
                    if (uri == null) {
                        errorMessage = "Bu görevi tamamlamak için kameradan kanıt çekin."
                        return@launch
                    }
                    val media = readProofFile(context, uri, currentTask.allowedProofType)
                    if (media == null) {
                        errorMessage = "Dosya okunamadı, 20 MB sınırını aşıyor ya da izin verilen kanıt türüne uymuyor."
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
                    val errorTitle = runCatching {
                        JsonParser.parseString(complete.errorBody()?.string().orEmpty())
                            .asJsonObject.get("title")?.asString
                    }.getOrNull()
                    if (complete.code() == 400 && errorTitle == "task_already_completed") {
                        refreshRequired = true
                        if (refreshTasks()) {
                            LukaFeedback.show("Bu görev zaten tamamlandı")
                            onNavigateBack()
                        } else {
                            errorMessage = "Bu görev zaten tamamlandı. Güncel durum için görev listesini yenileyin."
                        }
                    } else {
                        errorMessage = "Tamamlama kaydedilemedi. Görev listesini yenileyip tekrar deneyin."
                    }
                    return@launch
                }
                val result = complete.body()?.toTaskItem(currentTask.assignedTo, currentTask.shiftId)
                if (result == null) {
                    refreshRequired = true
                    errorMessage = "Görevin güncel durumunu görmek için listeyi yenileyin."
                    return@launch
                }
                viewModel.updateTask(result)
                LukaFeedback.show(when {
                    result.status == TaskStatus.AWAITING_APPROVAL -> "Kanıt yönetici onayına gönderildi"
                    !result.isShared -> "Görev tamamlandı"
                    result.status == TaskStatus.DONE -> "Ortak görev tamamlandı"
                    else -> "Katkı kaydedildi"
                })
                onNavigateBack()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                refreshRequired = true
                errorMessage = "Sonuç doğrulanamadı. Tekrar göndermeden önce görev listesini yenileyin."
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
                    initials = com.lukasoft.lukarest.data.store.AuthStore.staffName.value?.take(2)?.uppercase() ?: "P",
                    gradient = StaffGradient,
                    actionIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onActionClick = onNavigateBack
                )
            }
        },
        containerColor = BgScreenTop
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LukaCard {
                Text(task?.title ?: "", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
                if (task?.status == TaskStatus.AWAITING_APPROVAL) Text("Kanıt yönetici onayında. Yeniden gönderilemez.", color = PillWarn.fg)
                if (task?.isShared == true) {
                    Text("Ortak görev · %d / %d tamamlandı".format(task.completedCount, task.requiredCount), color = StaffGradStart)
                }
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

            task?.reviewNote?.let { Text("Yönetici notu: $it", color = MaterialTheme.colorScheme.error) }
            if (task != null && (task.mediaRequired || task.requiresApproval)) {
                LukaCard {
                    Text(
                        "Zorunlu kanıt: ${when (task.allowedProofType) { "PhotoOnly" -> "fotoğraf"; "VideoOnly" -> "video"; else -> "fotoğraf veya video" }}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val canTakePhoto = task.allowedProofType == "PhotoOnly" || task.allowedProofType == "PhotoOrVideo"
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        if (canTakePhoto) OutlinedButton(onClick = {
                            captureVideo = false
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                askCameraPermission = true
                            } else cameraPermission.launch(Manifest.permission.CAMERA)
                        }, enabled = canSubmit && !isSubmitting, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Kamerayı aç")
                        }
                        if (task.allowedProofType in listOf("VideoOnly", "PhotoOrVideo")) OutlinedButton(onClick = {
                            captureVideo = true
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) askCameraPermission = true
                            else cameraPermission.launch(Manifest.permission.CAMERA)
                        }, enabled = canSubmit && !isSubmitting, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Videocam, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Video çek")
                        }
                    }
                    selectedUri?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Seçilen ${if (selectedMediaKind == "video") "video" else "fotoğraf"}: ${displayName(context, it)}", color = StaffGradStart, fontSize = 12.sp)
                    }
                    Text("Yalnız bu ekranda çekilen kamera kanıtı · en fazla 20 MB", color = TextMuted, fontSize = 11.sp)
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

            errorMessage?.let { Text(it, color = PillErr.fg, fontSize = 13.sp) }
            if (refreshRequired) {
                TextButton(
                    enabled = !isSubmitting,
                    onClick = {
                        scope.launch {
                            isSubmitting = true
                            try {
                                if (refreshTasks()) {
                                    onNavigateBack()
                                } else {
                                    errorMessage = "Görevler yenilenemedi. Tekrar deneyin."
                                }
                            } catch (_: java.io.IOException) {
                                errorMessage = "Görevler yenilenemedi. Tekrar deneyin."
                            } finally {
                                isSubmitting = false
                            }
                        }
                    }
                ) { Text("Görev listesini yenile") }
            }
            PrimaryButton(
                text = if (isSubmitting) "Gönderiliyor..." else if (task?.isShared == true) "Bir tamamlama kaydet" else "Görevi Tamamla",
                onClick = { submitCompletion() },
                loading = isSubmitting,
                enabled = !isSubmitting && !refreshRequired && task != null && task.status !in listOf(TaskStatus.DONE, TaskStatus.AWAITING_APPROVAL),
                gradient = StaffGradient
            )
        }
    }
}

private data class ProofFile(val fileName: String, val mimeType: String, val bytes: ByteArray)

private fun readProofFile(context: Context, uri: Uri, allowedProofType: String): ProofFile? = runCatching {
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
    val isImage = mime.startsWith("image/")
    val isVideo = mime.startsWith("video/")
    if ((allowedProofType == "PhotoOnly" && !isImage) ||
        (allowedProofType == "VideoOnly" && !isVideo) ||
        (allowedProofType == "None")) return null
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
