package com.lukasoft.lukarest.data.api

fun apiErrorMessage(body: String?, fallback: String): String = runCatching {
    val json = org.json.JSONObject(body.orEmpty())
    if (json.optString("code").ifBlank { json.optString("title") } == "shift_ended")
        "Vardiyanın bitiş saati geçtiği için giriş yapılamaz."
    else if (json.optString("code").ifBlank { json.optString("title") } == "profanity_detected")
        "Metinde argo veya küfür tespit edildi. Lütfen ifadeyi düzenleyin."
    else fallback
}.getOrDefault(fallback)
