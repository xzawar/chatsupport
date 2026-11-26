package com.codexce.supportchat.ui.theme

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/*
 * Shared control colours, so the "one bright thing" rule is applied in one place.
 *
 * The accent is light, so nothing white is ever printed on it: the label is the deep control
 * ink (9.5:1). That one rule holds in both appearances, so there is no per-screen theme check.
 */

/** The switch: accent track, control-ink thumb (a white thumb on a light track is not a shape). */
@Composable
fun supportSwitchColors(): SwitchColors {
    val c = SupportTheme.colors
    return SwitchDefaults.colors(
        checkedThumbColor = c.onAccent,
        checkedTrackColor = c.accent,
        checkedBorderColor = c.accent,
        checkedIconColor = c.accent,
        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        uncheckedBorderColor = MaterialTheme.colorScheme.outline,
    )
}

/** The primary button. One per screen. */
@Composable
fun supportButtonColors(): ButtonColors {
    val c = SupportTheme.colors
    return ButtonDefaults.buttonColors(
        containerColor = c.accent,
        contentColor = c.onAccent,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        disabledContentColor = c.contentTertiary,
    )
}

/** The secondary button: soft accent tint, ink label. */
@Composable
fun supportSecondaryButtonColors(): ButtonColors {
    val c = SupportTheme.colors
    return ButtonDefaults.buttonColors(
        containerColor = c.accentSoft,
        contentColor = c.onAccentSoft,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        disabledContentColor = c.contentTertiary,
    )
}

/** The quiet alternative: neutral tint, ink label. */
@Composable
fun supportNeutralButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor = MaterialTheme.colorScheme.onSurface,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    disabledContentColor = SupportTheme.colors.contentTertiary,
)

/** Destructive confirm. */
@Composable
fun supportDangerButtonColors(): ButtonColors {
    val n = SupportTheme.colors.negative
    return ButtonDefaults.buttonColors(
        containerColor = n.strong,
        contentColor = n.onStrong,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        disabledContentColor = SupportTheme.colors.contentTertiary,
    )
}

/** App bars are the screen colour: no band across the top, no tint on scroll. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun supportTopBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.background,
    scrolledContainerColor = MaterialTheme.colorScheme.background,
    titleContentColor = MaterialTheme.colorScheme.onBackground,
    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
)

/**
 * Inputs sit on the neutral tint with no outline at rest; focus draws the ink border. Labels and
 * hints are secondary/tertiary content, errors the negative sentiment.
 */
@Composable
fun supportFieldColors(): TextFieldColors {
    val s = MaterialTheme.colorScheme
    val c = SupportTheme.colors
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = s.onSurface,
        unfocusedTextColor = s.onSurface,
        disabledTextColor = c.contentTertiary,
        cursorColor = s.primary,
        focusedBorderColor = s.primary,
        unfocusedBorderColor = Color.Transparent,
        disabledBorderColor = Color.Transparent,
        errorBorderColor = c.negative.content,
        focusedLabelColor = s.onSurfaceVariant,
        unfocusedLabelColor = s.onSurfaceVariant,
        disabledLabelColor = c.contentTertiary,
        focusedPlaceholderColor = c.contentTertiary,
        unfocusedPlaceholderColor = c.contentTertiary,
        focusedContainerColor = s.surfaceContainerLow,
        unfocusedContainerColor = s.surfaceContainerLow,
        disabledContainerColor = s.surfaceContainerLow,
        errorContainerColor = s.surfaceContainerLow,
        errorLabelColor = c.negative.content,
        errorSupportingTextColor = c.negative.content,
    )
}
