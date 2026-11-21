package com.lukasoft.lukarest.data.api

fun apiErrorMessage(body: String?, fallback: String): String = runCatching {
    val json = org.json.JSONObject(body.orEmpty())
    when (json.optString("code").ifBlank { json.optString("title") }) {
        "shift_ended" -> "Vardiyanın bitiş saati geçtiği için giriş yapılamaz."
        "shift_not_started", "shift_too_early" -> "Vardiya başlangıç saati henüz gelmedi."
        "no_shift_today" -> "Bugün için planlanmış bir vardiyanız yok."
        "task_requires_clock_in" -> "Göreve başlamak için önce vardiyanızı başlatın."
        "profanity_detected" -> "Metinde argo veya küfür tespit edildi. Lütfen ifadeyi düzenleyin."
        else -> fallback
    }
}.getOrDefault(fallback)
