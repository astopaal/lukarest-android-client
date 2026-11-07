package com.lukasoft.lukarest.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

object LukaFeedback {
    private val messages = MutableSharedFlow<String>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    fun show(message: String) {
        messages.tryEmit(message)
    }

    internal val stream get() = messages
}

@Composable
fun BoxScope.LukaFeedbackOverlay(modifier: Modifier = Modifier) {
    val host = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        LukaFeedback.stream.collect { host.showSnackbar(it) }
    }
    androidx.compose.material3.SnackbarHost(
        hostState = host,
        modifier = modifier.padding(bottom = 82.dp),
        snackbar = { data -> LukaSnackbar(data) }
    )
}
