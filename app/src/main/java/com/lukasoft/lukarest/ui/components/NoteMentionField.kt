package com.lukasoft.lukarest.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lukasoft.lukarest.data.api.NoteMentionableUserDto
import com.lukasoft.lukarest.ui.theme.*

private val mentionPattern = Regex("(?<![\\p{L}\\p{N}_.@])@([a-zA-Z0-9_.]+)")

fun activeNoteMention(text: String, cursor: Int): IntRange? {
    if (cursor !in 0..text.length) return null
    val prefix = text.take(cursor)
    val start = prefix.lastIndexOf('@')
    if (start < 0 || (start > 0 && (prefix[start - 1].isLetterOrDigit() || prefix[start - 1] in "_.@"))) return null
    return if (prefix.substring(start + 1).all { it.isLetterOrDigit() || it in "_." }) start until cursor else null
}

fun noteMentionIds(text: String, people: List<NoteMentionableUserDto>): List<String> {
    val handles = mentionPattern.findAll(text).map { it.groupValues[1].trimEnd('.').lowercase() }.toSet()
    return people.filter { it.handle.lowercase() in handles }.map { it.userId }.distinct()
}

private fun highlightedNote(text: String) = buildAnnotatedString {
    append(text)
    mentionPattern.findAll(text).forEach { match ->
        addStyle(SpanStyle(color = StaffGradStart, fontWeight = FontWeight.SemiBold), match.range.first, match.range.last + 1)
    }
}

@Composable
fun NoteContent(text: String) {
    Text(highlightedNote(text), color = TextMain, style = MaterialTheme.typography.bodyLarge)
}

@Composable
fun NoteMentionField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    people: List<NoteMentionableUserDto>,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    enabled: Boolean = true
) {
    val range = if (value.selection.collapsed) activeNoteMention(value.text, value.selection.start) else null
    val query = range?.let { value.text.substring(it.first + 1, value.selection.start) }
    val matches = if (query == null) emptyList() else people.filter {
        it.fullName.contains(query, ignoreCase = true) || it.handle.contains(query, ignoreCase = true)
    }
    OutlinedTextField(
        value = value, onValueChange = onValueChange, enabled = enabled,
        label = { Text("Not") }, minLines = 3, maxLines = 8,
        supportingText = { Text("Birinden bahsetmek için @ yazın. ${value.text.length}/2000") },
        isError = value.text.length > 2000,
        visualTransformation = { TransformedText(highlightedNote(it.text), OffsetMapping.Identity) },
        modifier = Modifier.fillMaxWidth()
    )
    if (range != null) {
        Surface(shape = MaterialTheme.shapes.medium, color = White, tonalElevation = 2.dp) {
            Column(Modifier.fillMaxWidth().heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                Text("Personel ve patronlar", style = MaterialTheme.typography.labelMedium, color = TextMuted, modifier = Modifier.padding(12.dp))
                when {
                    loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                    error != null -> {
                        Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp))
                        TextButton(onClick = onRetry, enabled = enabled) { Text("Kişileri tekrar yükle") }
                    }
                    matches.isEmpty() -> Text("Eşleşen kişi bulunamadı.", color = TextMuted, modifier = Modifier.padding(12.dp))
                    else -> matches.forEach { person ->
                        Column(Modifier.fillMaxWidth().clickable(enabled = enabled) {
                            val replacement = "@${person.handle} "
                            val end = value.selection.start
                            val tailEnd = value.text.indexOfFirstFrom(end) { !it.isLetterOrDigit() && it !in "_." }
                            val text = value.text.replaceRange(range.first, tailEnd, replacement)
                            onValueChange(TextFieldValue(text, TextRange(range.first + replacement.length)))
                        }.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(person.fullName, color = TextMain, fontWeight = FontWeight.SemiBold)
                            Text("@${person.handle} · ${if (person.isOwner) "Patron" else "Personel"}", color = TextMuted,
                                style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

private fun String.indexOfFirstFrom(start: Int, predicate: (Char) -> Boolean): Int {
    for (index in start until length) if (predicate(this[index])) return index
    return length
}
