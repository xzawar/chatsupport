package com.codexce.supportchat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.codexce.supportchat.data.model.ChatMessage
import com.codexce.supportchat.ui.theme.SupportTheme
import com.codexce.supportchat.ui.theme.SupportType

/*
 * Two shapes, built once. A new RoundedCornerShape per bubble per recomposition is a small
 * allocation multiplied by every message on screen.
 */
private val OwnerShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
private val VisitorShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)

/** Default bubble width cap. ChatScreen passes a width derived from the window instead. */
val DefaultBubbleMaxWidth: Dp = 300.dp

/**
 * One chat message.
 *
 * Both bubbles are tints, not solid fills: the outgoing one is the soft brand tint with ink text,
 * the incoming one the neutral tint. Reading a thread is a long, calm task, so neither side is
 * allowed to shout; the send button is the one bright thing on this screen.
 */
@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    maxWidth: Dp = DefaultBubbleMaxWidth,
) {
    if (message.isSystem) {
        SystemNotice(text = message.text, modifier = modifier)
        return
    }

    val fromOwner = message.fromOwner
    val c = SupportTheme.colors
    val time = remember(message.createdAt) { formatTime(message.createdAt) }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = if (fromOwner) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            Modifier
                .widthIn(max = maxWidth)
                .clip(if (fromOwner) OwnerShape else VisitorShape)
                .background(
                    if (fromOwner) c.accentSoft else MaterialTheme.colorScheme.surfaceContainerLow,
                )
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp),
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = message.text,
                    style = SupportType.largeBody,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = time,
                    style = SupportType.nav,
                    color = c.contentTertiary,
                )
            }
        }
    }
}

@Composable
private fun SystemNotice(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            style = SupportType.nav,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}
