package com.codexce.supportchat.ui.components

import androidx.compose.foundation.layout.wrapContentWidth
import android.os.SystemClock
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.codexce.supportchat.data.AppPreferences
import com.codexce.supportchat.ui.theme.GroupedCardShape
import com.codexce.supportchat.ui.theme.PillShape
import com.codexce.supportchat.ui.theme.Sentiment
import com.codexce.supportchat.ui.theme.SettingsTileShape
import com.codexce.supportchat.ui.theme.Spacing
import com.codexce.supportchat.ui.theme.SupportTheme
import com.codexce.supportchat.ui.theme.SupportType
import com.codexce.supportchat.ui.theme.supportButtonColors
import com.codexce.supportchat.ui.theme.supportDangerButtonColors
import com.codexce.supportchat.ui.theme.supportNeutralButtonColors
import com.codexce.supportchat.ui.theme.supportSecondaryButtonColors

/*
 * The shared building blocks, styled to the calm-fintech DNA:
 *
 *   - surfaces separate by tint (16dp neutral groups), not by shadows or rules
 *   - leading icons sit in 40dp neutral circles, monochrome; colour only when it IS a sentiment
 *   - buttons are full-width 48dp pills; one accent-filled primary per screen
 *   - two type weights: 400 to read, 600 to find
 *
 * Every public signature here predates the restyle and is unchanged, so restyling these
 * restyles every screen without touching call sites.
 */

// =========================================================================================
// App bar pieces
// =========================================================================================

/**
 * The back affordance for every screen that has one: a 40dp round button on the neutral tint.
 *
 * [debounced] is applied here rather than at the call sites. Back is the control most likely to
 * be double-tapped, and an undebounced second tap pops two entries off the stack.
 */
@Composable
fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = debounced(onBack), modifier = modifier) {
        Box(
            Modifier
                .size(Spacing.iconButton)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(AppIcons.Back),
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * A top-level screen's title: left-aligned screenTitle, at most seven words, with an optional
 * one-line subtitle and trailing actions on the same baseline row.
 */
@Composable
fun ScreenTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = Spacing.gutter)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = SupportType.screenTitle,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            actions?.invoke(this)
        }
        if (subtitle != null) {
            Spacer(Modifier.height(Spacing.betweenText))
            Text(
                text = subtitle,
                style = SupportType.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Centres content at phone width on tablets and in landscape, instead of stretching rows and
 * buttons edge to edge. A no-op on phones.
 */
@Composable
fun ContentWidth(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = Spacing.maxContentWidth).fillMaxWidth(), content = content)
    }
}

/**
 * The same cap as [ContentWidth], as a modifier for the content of a scrolling column. Applied
 * after verticalScroll, so the scroll gesture still covers the whole window while the rows and
 * buttons stay phone-width and centred on a tablet or in landscape.
 */
fun Modifier.responsiveContent(): Modifier =
    this.wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = Spacing.maxContentWidth)

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    // Sentence case, groupTitle weight. Capitals shout; the DNA keeps caps for display only.
    Row(
        modifier.fillMaxWidth().padding(horizontal = Spacing.gutter, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = SupportType.groupTitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

// =========================================================================================
// Buttons. Full-width pills, 48dp tall, 16/24 SemiBold label, no elevation, no outline.
// =========================================================================================

@Composable
private fun PillButton(
    text: String,
    onClick: () -> Unit,
    colors: ButtonColors,
    modifier: Modifier,
    enabled: Boolean,
    loading: Boolean,
    @DrawableRes icon: Int?,
) {
    // While loading the button stays enabled-looking (same fill, ink spinner) and simply ignores
    // taps: greying it out mid-request reads as "something broke", not "working on it".
    val guarded = debounced { if (!loading) onClick() }
    Button(
        onClick = guarded,
        enabled = enabled || loading,
        modifier = modifier.fillMaxWidth().heightIn(min = Spacing.buttonHeight),
        shape = PillShape,
        colors = colors,
        elevation = null,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // The label keeps its slot while loading so the button never changes size.
        Box(contentAlignment = Alignment.Center) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (loading) Modifier.alpha(0f) else Modifier,
            ) {
                if (icon != null) {
                    Icon(painterResource(icon), contentDescription = null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(text, style = SupportType.largeBodyBold, maxLines = 1)
            }
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current,
                )
            }
        }
    }
}

/** The one accent-filled action on a screen. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    @DrawableRes icon: Int? = null,
) = PillButton(text, onClick, supportButtonColors(), modifier, enabled, loading, icon)

/** A real alternative to the primary: soft accent tint. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    @DrawableRes icon: Int? = null,
) = PillButton(text, onClick, supportSecondaryButtonColors(), modifier, enabled, loading, icon)

/** The quiet option: neutral tint. */
@Composable
fun NeutralButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    @DrawableRes icon: Int? = null,
) = PillButton(text, onClick, supportNeutralButtonColors(), modifier, enabled, loading, icon)

/** Destructive confirm: delete, sign out of everything. */
@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) = PillButton(text, onClick, supportDangerButtonColors(), modifier, enabled, loading, null)

/** A text action ("Learn more", "Skip"): ink, SemiBold, underlined so weight AND form mark it. */
@Composable
fun TertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .heightIn(min = Spacing.buttonHeight)
            .clip(PillShape)
            .safeClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = SupportType.largeBodyBold,
            color = if (enabled) MaterialTheme.colorScheme.primary else SupportTheme.colors.contentTertiary,
            textDecoration = TextDecoration.Underline,
        )
    }
}

// =========================================================================================
// Surfaces
// =========================================================================================

@Composable
fun SupportCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(GroupedCardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .then(if (onClick != null) Modifier.safeClickable(onClick = onClick) else Modifier)
            .padding(Spacing.component),
        content = content,
    )
}

@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    emphasised: Boolean = false,
) {
    // Emphasised = the soft accent tint, not the solid accent: a status is information, and the
    // solid accent is reserved for the one primary action on the screen.
    val c = SupportTheme.colors
    val background = if (emphasised) c.accentSoft else MaterialTheme.colorScheme.surfaceContainerHigh
    val foreground = if (emphasised) c.onAccentSoft else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = foreground,
        maxLines = 1,
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** A count badge: control ink on the accent, so a new message is the brightest thing in a row. */
@Composable
fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    val c = SupportTheme.colors
    Box(
        modifier
            .heightIn(min = 20.dp)
            .widthIn(min = 20.dp)
            .clip(CircleShape)
            .background(c.accent)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = SupportType.navBold,
            color = c.onAccent,
            maxLines = 1,
        )
    }
}

@Composable
fun InitialsAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    loading: Boolean = false,
    /**
     * The Google account photo, when there is one. If the URL fails to load the initials stay
     * behind it, which is also what happens for email-only accounts.
     */
    photoUrl: String? = null,
) {
    if (loading) {
        SkeletonBlock(modifier.size(size), CircleShape)
        return
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = SupportType.largeBodyBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(CircleShape),
            )
        }
    }
}

/** A 24dp monochrome glyph in a 40dp neutral circle: the leading element of every list row. */
@Composable
fun IconAvatar(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    background: Color = MaterialTheme.colorScheme.surfaceContainerLow,
) {
    Box(
        modifier
            .size(Spacing.iconButton)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = tint,
        )
    }
}

/**
 * A settings row: a solid white glyph on a coloured rounded tile, then the title.
 *
 * The per-row tile colours are back by request - they are what makes Settings scannable at a
 * glance, and each category keeps the same colour everywhere it appears. A row that passes no
 * [tint] gets the brand tile instead: the yellow with a charcoal glyph, since white on yellow
 * would be 1.5:1.
 */
@Composable
fun SettingsRow(
    @DrawableRes icon: Int,
    title: String,
    modifier: Modifier = Modifier,
    /*
     * Accepted and deliberately not drawn. Captions under row titles were removed from the whole
     * app; ignoring the parameter here removes every caption in one edit.
     */
    @Suppress("UNUSED_PARAMETER") subtitle: String? = null,
    tint: Color? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    val tile = tint ?: SupportTheme.colors.accent
    val glyph = if (tint == null) SupportTheme.colors.onAccent else Color.White
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .then(if (onClick != null) Modifier.safeClickable(onClick = onClick) else Modifier)
            .padding(horizontal = Spacing.gutter, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(SettingsTileShape)
                .background(tile),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = glyph,
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            style = SupportType.largeBody,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                painter = painterResource(AppIcons.ChevronRight),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = SupportTheme.colors.contentTertiary,
            )
        }
    }
}

// =========================================================================================
// Alerts and states
// =========================================================================================

/**
 * An inline alert: the sentiment lives in the ICON and a pale surface, the text stays content
 * colour. One sentence, one recovery action.
 */
@Composable
fun InlineAlert(
    message: String,
    modifier: Modifier = Modifier,
    sentiment: Sentiment = SupportTheme.colors.negative,
    @DrawableRes icon: Int = AppIcons.AlertCircle,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(GroupedCardShape)
            .background(sentiment.surface)
            .padding(horizontal = Spacing.component, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = sentiment.content,
            modifier = Modifier.padding(top = 1.dp).size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = message,
                style = SupportType.body,
                color = MaterialTheme.colorScheme.onSurface,
            )
            content()
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = actionLabel,
                style = SupportType.bodyBold,
                color = MaterialTheme.colorScheme.onSurface,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.safeClickable(onClick = onAction),
            )
        }
    }
}

/**
 * Surfaces a failure without putting a stack trace in front of a customer.
 *
 * [message] is always shown and is expected to be one plain sentence. [detail] is the unedited
 * diagnostic, and it appears only when developer mode is on - collapsed, behind a tap, and
 * copyable. Developer mode is read here so the rule is applied once, not at a dozen call sites.
 */
@Composable
fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    val devMode by AppPreferences.get(LocalContext.current).devMode.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    // Keyed on detail so a new failure always arrives collapsed.
    var expanded by remember(detail) { mutableStateOf(false) }
    val hasDetail = devMode && !detail.isNullOrBlank()

    InlineAlert(
        message = message,
        modifier = modifier.padding(horizontal = Spacing.gutter, vertical = 8.dp),
        actionLabel = "Dismiss",
        onAction = onDismiss,
    ) {
        if (hasDetail) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (expanded) "Hide details" else "Show details",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.safeClickable { expanded = !expanded },
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "Copy",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.safeClickable {
                        clipboard.setText(AnnotatedString(detail.orEmpty()))
                    },
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Text(
                    text = detail.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/**
 * The info-screen pattern: a glyph in a 64dp neutral circle, a short title, one supporting line,
 * one action. An invitation, not an absence.
 */
@Composable
fun EmptyState(
    @DrawableRes icon: Int,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.betweenSections, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(Spacing.component))
        Text(
            text = title,
            style = SupportType.bodyTitle,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.betweenText))
        Text(
            text = message,
            style = SupportType.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        if (action != null) {
            Spacer(Modifier.height(Spacing.contentToButton))
            Box(Modifier.widthIn(max = 320.dp)) { action() }
        }
    }
}

/** Divider for genuinely flat lists. Prefer separating by tint. */
@Composable
fun ThinDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(start = Spacing.gutter)
            .height(Dp.Hairline)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

/**
 * A group of rows drawn as one 16dp neutral-tint card on the screen. Meaning comes from which
 * rows share a card, not from headers and rules.
 */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.gutter)
            .clip(GroupedCardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        content = content,
    )
}

/**
 * Rows inside a [SettingsGroup] touch: they carry their own padding and are separated by the
 * group's tint, not by lines. Kept as a function so the call sites do not change; it draws
 * nothing.
 */
@Composable
fun GroupDivider(modifier: Modifier = Modifier) {
    Spacer(modifier)
}

// =========================================================================================
// Tap spam
// =========================================================================================

/**
 * How long the app ignores a second tap.
 *
 * 500ms is above the ~300ms a double-tap lands in and below the ~700ms at which a deliberate
 * second tap starts to feel dropped.
 */
private const val CLICK_WINDOW_MILLIS = 100L

/** One frame. Repeat calls within this are one tap passing through nested guards, not spam. */
private const val NESTED_CALL_MILLIS = 16L

/**
 * One guard for the whole app, not one per control.
 *
 * The damage from tap spam is almost never a control firing twice on its own; it is a second
 * tap landing on a DIFFERENT control while the first is still animating. Tapping a settings row
 * twice pushes that screen onto the back stack twice, so the first back press appears to do
 * nothing. Tapping two rows in quick succession pushes both. A per-control guard cannot see
 * either case, because the two taps are not the same control.
 *
 * elapsedRealtime, not currentTimeMillis: the wall clock can be moved backwards by the network
 * or the user, which would disable the guard until it caught up.
 *
 * Switches and text fields are deliberately not routed through this. A Switch reports through
 * onCheckedChange rather than a click, so it stays responsive, which is correct - flipping two
 * toggles quickly is ordinary use, not spam.
 */
object ClickGuard {
    private var lastClickAt = 0L

    fun allow(): Boolean {
        val now = SystemClock.elapsedRealtime()
        val sinceLast = now - lastClickAt

        // Also catches a clock that has not started: 0 - 0 is not less than the window.
        if (lastClickAt != 0L && sinceLast < CLICK_WINDOW_MILLIS) {
            /*
             * Re-entrancy, not spam.
             *
             * A guarded row that is ALSO handed an already-guarded lambda asks this question
             * twice for one tap. The first call stamps the clock, the second sees ~0ms elapsed
             * and, with a naive check, refuses - so the guard cancels its own click and the
             * control becomes permanently dead. That is exactly what happened to the nav bar,
             * the Settings rows and the profile chips.
             *
             * Anything arriving inside one frame of the last click is the same tap travelling
             * through nested guards, so it is allowed through. A human cannot produce two
             * distinct taps 16ms apart; real spam lands tens of milliseconds later and is
             * still refused. Guarding twice is now harmless instead of fatal.
             */
            return sinceLast <= NESTED_CALL_MILLIS
        }
        lastClickAt = now
        return true
    }
}

/**
 * Wraps a click so it can only fire once per window. Use on Buttons, IconButtons and anything
 * else taking an `onClick` lambda.
 *
 * rememberUpdatedState so a lambda that closes over changing state is not frozen at the value
 * it had on first composition.
 */
@Composable
fun debounced(onClick: () -> Unit): () -> Unit {
    val latest by rememberUpdatedState(onClick)
    return remember { { if (ClickGuard.allow()) latest() } }
}

/** [clickable] with the same guard. */
fun Modifier.safeClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier =
    // clickable, NOT safeClickable. Calling safeClickable here recurses forever and blows the
    // stack the first time any guarded row is composed.
    this.clickable(enabled = enabled) { if (ClickGuard.allow()) onClick() }

// =========================================================================================
// Grouping without headings
// =========================================================================================

/** The gap between two groups. */
@Composable
fun GroupGap(modifier: Modifier = Modifier) {
    Spacer(modifier.height(Spacing.contentToButton))
}

/** Right-aligned controls where a section heading used to hold them. */
@Composable
fun RowActions(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = Spacing.gutter, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/*
 * How a PlainRow splits its width. Labels are one or two words; values are addresses and email
 * addresses, so the value gets the larger share.
 */
private const val TitleWeight = 0.42f
private const val ValueWeight = 0.58f

/**
 * A receipt-style row: label left, value right. For lists of facts, not menus of destinations.
 */
@Composable
fun PlainRow(
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    /** Accepted and deliberately not drawn. See [SettingsRow]. */
    @Suppress("UNUSED_PARAMETER") subtitle: String? = null,
    danger: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.safeClickable(onClick = onClick) else Modifier)
            .padding(horizontal = Spacing.gutter, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(TitleWeight)) {
            Text(
                text = title,
                style = if (danger) SupportType.largeBodyBold else SupportType.largeBody,
                color = if (danger) {
                    SupportTheme.colors.negative.content
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
        if (value != null) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = value,
                style = SupportType.bodyBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(ValueWeight, fill = false),
            )
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Spacer(Modifier.width(6.dp))
            Icon(
                painter = painterResource(AppIcons.ChevronRight),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Static explanatory text, boxed, so it is never mistaken for a tappable row.
 */
@Composable
fun InfoBox(
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.gutter)
            .clip(GroupedCardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = Spacing.component, vertical = 14.dp),
    ) {
        Icon(
            painter = painterResource(AppIcons.InfoCircle),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 1.dp).size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            if (title != null) {
                Text(text = title, style = SupportType.bodyBold)
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = text,
                style = SupportType.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
