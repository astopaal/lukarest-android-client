package com.lukasoft.lukarest.ui.components

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
import androidx.compose.ui.unit.dp
import com.lukasoft.lukarest.data.api.NoteMentionableUserDto
import com.lukasoft.lukarest.ui.theme.*

private val mentionPattern = Regex("(?<![\\p{L}\\p{N}_.@])@([a-zA-Z0-9_.]+)")
private val generatedHandleSuffix = Regex("_[0-9a-f]{8}$", RegexOption.IGNORE_CASE)

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

private data class DisplayMention(val source: IntRange, val label: String)

private fun displayMentions(text: String, people: List<NoteMentionableUserDto>): List<DisplayMention> {
    val names = people.associate { it.handle.lowercase() to it.fullName }
    return mentionPattern.findAll(text).mapNotNull { match ->
        if (text.getOrNull(match.range.last + 1) == '@') return@mapNotNull null
        val handle = match.groupValues[1].trimEnd('.')
        val label = when {
            handle.equals("all", ignoreCase = true) -> "Herkes"
            handle.lowercase() in names -> names.getValue(handle.lowercase())
            generatedHandleSuffix.containsMatchIn(handle) -> handle.replace(generatedHandleSuffix, "").replace('.', ' ')
            else -> return@mapNotNull null
        }
        DisplayMention(match.range.first until match.range.first + handle.length + 1, "@$label")
    }.toList()
}

private fun highlightedNote(text: String, mentions: List<DisplayMention>) = buildAnnotatedString {
    var end = 0
    mentions.forEach { mention ->
        append(text.substring(end, mention.source.first))
        val start = length
        append(mention.label)
        addStyle(SpanStyle(color = StaffGradStart, fontWeight = FontWeight.SemiBold), start, length)
        end = mention.source.last + 1
    }
    append(text.substring(end))
}

private fun mentionOffsetMapping(mentions: List<DisplayMention>): OffsetMapping = object : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        var delta = 0
        for ((source, label) in mentions) {
            if (offset <= source.first) break
            if (offset <= source.last) return source.first + delta + (offset - source.first).coerceAtMost(label.length)
            delta += label.length - source.count()
        }
        return offset + delta
    }

    override fun transformedToOriginal(offset: Int): Int {
        var delta = 0
        for ((source, label) in mentions) {
            val start = source.first + delta
            if (offset <= start) break
            if (offset < start + label.length) return source.first + (offset - start).coerceAtMost(source.count())
            delta += label.length - source.count()
        }
        return offset - delta
    }
}

fun noteDisplayText(text: String, people: List<NoteMentionableUserDto> = emptyList()): String =
    highlightedNote(text, displayMentions(text, people)).text

@Composable
fun NoteContent(text: String, people: List<NoteMentionableUserDto>) {
    Text(highlightedNote(text, displayMentions(text, people)), color = TextMain, style = MaterialTheme.typography.bodyLarge)
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
    val showEveryone = query != null && ("all".startsWith(query, ignoreCase = true) || "Herkes".startsWith(query, ignoreCase = true))
    OutlinedTextField(
        value = value, onValueChange = onValueChange, enabled = enabled,
        label = { Text("Not") }, minLines = 3, maxLines = 8,
        supportingText = { Text("Birinden bahsetmek için @ yazın. ${value.text.length}/2000") },
        isError = value.text.length > 2000,
        visualTransformation = {
            val mentions = displayMentions(it.text, people)
            TransformedText(highlightedNote(it.text, mentions), mentionOffsetMapping(mentions))
        },
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
                    matches.isEmpty() && !showEveryone -> Text("Eşleşen kişi bulunamadı.", color = TextMuted, modifier = Modifier.padding(12.dp))
                    else -> {
                        val options = (if (showEveryone) listOf("all" to "Herkes") else emptyList()) +
                            matches.map { it.handle to it.fullName }
                        options.forEach { (handle, name) ->
                            TextButton(
                                enabled = enabled,
                                modifier = Modifier.heightIn(min = 48.dp),
                                onClick = {
                                    val replacement = "@$handle "
                                    val end = value.selection.start
                                    val tailEnd = value.text.indexOfFirstFrom(end) { !it.isLetterOrDigit() && it !in "_." }
                                    val text = value.text.replaceRange(range.first, tailEnd, replacement)
                                    onValueChange(TextFieldValue(text, TextRange(range.first + replacement.length)))
                                }
                            ) { Text(name) }
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
