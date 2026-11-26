package com.codexce.supportchat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.codexce.supportchat.data.model.Conversation
import com.codexce.supportchat.ui.theme.Spacing
import com.codexce.supportchat.ui.theme.SupportTheme
import com.codexce.supportchat.ui.theme.SupportType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * One inbox row, in the list anatomy of the design DNA:
 *
 *   48dp avatar | name (16 SemiBold)          time (12, tertiary)
 *               | preview (14, secondary)      count badge
 *
 * Unread is carried by weight and colour together - the preview steps up to primary ink and
 * SemiBold, and the count badge is the accent - so it is legible to colour-blind users and the
 * newest message is the brightest thing in its row. Two weights only: nothing here is Bold.
 *
 * Status is one small pill next to the name, shown only when it is worth saying: Pending is a
 * call to action and Closed explains why a thread is read-only.
 *
 * The timestamp is a sibling of the text column, pinned to the far right, so it lines up down
 * the whole list regardless of name length.
 */
@Composable
fun ConversationRow(
    conversation: Conversation,
    isMine: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unread = conversation.unreadForAgents > 0
    // Formatting is the most expensive thing in this row. Keyed on the timestamp, it runs once
    // per change instead of on every recomposition and every scroll back into view.
    val time = remember(conversation.lastActivityAt) { formatTime(conversation.lastActivityAt) }

    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .background(MaterialTheme.colorScheme.background)
            .safeClickable(onClick = onOpen)
            .padding(horizontal = Spacing.gutter, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PersonAvatar(
            name = conversation.visitorName,
            email = conversation.visitorEmail,
            seed = conversation.id,
            size = 48.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = conversation.visitorName,
                        style = SupportType.largeBodyBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (conversation.isPending) {
                        Spacer(Modifier.width(8.dp))
                        StatusPill(text = "Waiting", emphasised = true)
                    } else if (conversation.isClosed) {
                        Spacer(Modifier.width(8.dp))
                        StatusPill(text = "Closed")
                    } else if (isMine) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = time,
                    style = SupportType.nav,
                    color = if (unread) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        SupportTheme.colors.contentTertiary
                    },
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.lastText.ifBlank { "No messages yet" },
                    style = if (unread) SupportType.bodyBold else SupportType.body,
                    color = if (unread) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (unread) {
                    Spacer(Modifier.width(8.dp))
                    CountBadge(count = conversation.unreadForAgents.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                }
            }
        }
    }
}

/*
 * Formatters are expensive to construct (locale data, calendar, pattern compile) and this runs
 * for every row and every bubble. SimpleDateFormat is not thread-safe, so one instance per
 * thread rather than one shared instance; in practice that is one, the main thread.
 */
private class Local<T : Any>(private val create: () -> T) : ThreadLocal<T>() {
    // ThreadLocal.withInitial is API 26; minSdk is 24.
    override fun initialValue(): T = create()
}

private val timeFormat = Local { SimpleDateFormat("HH:mm", Locale.getDefault()) }
private val dateFormat = Local { SimpleDateFormat("d MMM", Locale.getDefault()) }
private val calendars = Local { Calendar.getInstance() to Calendar.getInstance() }

fun formatTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return ""
    val (then, now) = calendars.get()!!
    then.timeInMillis = epochMillis
    now.timeInMillis = System.currentTimeMillis()
    val sameDay = then.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        then.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    val format = if (sameDay) timeFormat.get()!! else dateFormat.get()!!
    return format.format(Date(epochMillis))
}
