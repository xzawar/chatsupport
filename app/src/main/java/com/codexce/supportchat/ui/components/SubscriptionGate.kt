package com.codexce.supportchat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.codexce.supportchat.ui.theme.supportButtonColors

/**
 * Shown when the real reason a screen has no data is "this account has no active subscription".
 *
 * The screens used to surface the raw backend refusal for this case, which reads as a database
 * rules problem and sends the owner looking in entirely the wrong place. A lapsed subscription
 * is a billing state with an obvious next action, so it gets one.
 *
 * TODO: replicate this on the Email and Social tabs, and on the Emails screen reached from
 * Settings. Only the Chatbox tab is wired up in this pass, by request.
 *
 * Written as an offer rather than a refusal: the benefit and the one next step. The lock is
 * monochrome - a plan is not an error, so it does not get the negative sentiment's red.
 */
@Composable
fun SubscriptionGate(
    onSubscribe: () -> Unit,
    modifier: Modifier = Modifier,
    message: String = "Upgrade your plan to start using this.",
) {
    EmptyState(
        icon = AppIcons.Lock,
        title = "Unlock this with a plan",
        message = message,
        modifier = modifier,
        action = { PrimaryButton(text = "See plans", onClick = onSubscribe) },
    )
}
