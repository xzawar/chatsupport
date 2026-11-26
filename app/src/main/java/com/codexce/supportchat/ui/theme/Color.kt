package com.codexce.supportchat.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * Calm-fintech colour system ("Neptune" structure), applied to this app's own brand.
 *
 * The structure follows the design DNA in the Wise extraction: one bright accent per screen,
 * a deep same-hue ink for anything printed on top of it, near-black content, tinted neutrals,
 * and five sentiments. Colour is a ROLE, never a decoration: screens pick a token from
 * MaterialTheme.colorScheme or SupportTheme.colors, never a hex.
 *
 * The brand is NOT Wise's. Their green pair is a proprietary asset; the four brand slots below
 * use this app's launcher sky blue instead. Swapping the brand means editing these four values
 * and nothing else:
 *
 *   BrandAccent     the single bright fill: primary buttons, the "on" switch, progress
 *   BrandControl    text and glyphs printed ON the accent (never white - see contrast below)
 *   BrandInk        interactive text in light mode: links, selected tabs, text buttons
 *   BrandInkSoft    the accent tint: secondary-button fill, outgoing chat bubble
 *
 * Contrast (WCAG, computed):
 *   BrandControl on BrandAccent      9.5:1     ContentPrimary on white      19.0:1
 *   BrandInk on white               12.0:1     ContentSecondary on white     9.0:1
 *   BrandInk on BrandInkSoft        10.6:1     ContentTertiary on white      5.1:1
 *   Accent on dark screen           11.2:1     Dark primary text on screen  16.9:1
 *
 * Surfaces are separated by tint, not by shadow or rule. The neutral fills are stored as
 * opaque, pre-composited values rather than translucent ones: they paint in one pass with no
 * alpha blend, and Material components that expect an opaque container (menus, dialogs, text
 * fields) behave correctly.
 */

// ---------------------------------------------------------------------------
// Brand slots
// ---------------------------------------------------------------------------

val BrandAccent = Color(0xFF7FD4FF)
val BrandControl = Color(0xFF05263B)
val BrandInk = Color(0xFF0B3A57)
val BrandInkSoft = Color(0xFFE3F4FF)

/** The accent-tint in the dark appearance: a deep brand surface, carrying accent-coloured text. */
private val BrandInkSoftDark = Color(0xFF123247)

// ---------------------------------------------------------------------------
// Neutrals. Faintly cool so they sit with the blue brand rather than against it.
// ---------------------------------------------------------------------------

private val ContentPrimaryLight = Color(0xFF0E1113)
private val ContentSecondaryLight = Color(0xFF454A4E)
private val ContentTertiaryLight = Color(0xFF6A6F74)

private val ScreenLight = Color(0xFFFFFFFF)
private val NeutralLight = Color(0xFFEEF2F5)        // grouped cards, inputs, avatars
private val NeutralStrongLight = Color(0xFFE5EAEE)  // pressed rows, search pill on a neutral card
private val NeutralStrongestLight = Color(0xFFDBE1E6)
private val BorderLight = Color(0xFFE1E5E8)         // ink at 12% on white

private val ContentPrimaryDark = Color(0xFFF2F5F7)
private val ContentSecondaryDark = Color(0xFFC8CED3)
private val ContentTertiaryDark = Color(0xFF9AA2A8)

/** Never pure black: a blue-black, so the brand still reads in the dark appearance. */
private val ScreenDark = Color(0xFF111416)
private val ElevatedDark = Color(0xFF1D2124)        // dialogs, menus, sheets
private val NeutralDark = Color(0xFF262A2D)         // white at ~10% on the screen
private val NeutralStrongDark = Color(0xFF2F3438)
private val NeutralStrongestDark = Color(0xFF3A3F43)
private val BorderDark = Color(0xFF33383C)

/** Window background colours, mirrored in res/values(-night)/colors.xml and MainActivity. */
const val WindowBackgroundLight = 0xFFFFFFFF
const val WindowBackgroundDark = 0xFF111416

// ---------------------------------------------------------------------------
// Sentiments. Pale surface + dark content, so the text always clears 4.5:1 on its own box.
// Warning text is never yellow: yellow is a fill, the text is brown.
// ---------------------------------------------------------------------------

@Immutable
data class Sentiment(
    /** Text and glyphs that carry the sentiment. */
    val content: Color,
    /** Pale box behind a sentiment message. */
    val surface: Color,
    /** Solid fill for a loud, blocking banner or a destructive confirm. */
    val strong: Color,
    /** Content on [strong]. */
    val onStrong: Color,
)

/**
 * The roles Material's ColorScheme has no slot for.
 *
 * Read through [SupportTheme.colors]. Everything Material does have a slot for still lives in
 * MaterialTheme.colorScheme, so stock components keep working without wrappers.
 */
@Immutable
data class SupportColors(
    val accent: Color,
    val onAccent: Color,
    /** Secondary-button fill and the outgoing bubble. */
    val accentSoft: Color,
    val onAccentSoft: Color,
    /** Timestamps, placeholders, hints. Below onSurfaceVariant, still above 4.5:1. */
    val contentTertiary: Color,
    /** Pressed and hovered overlay on any surface. */
    val overlay: Color,
    val negative: Sentiment,
    val positive: Sentiment,
    val warning: Sentiment,
    val proposition: Sentiment,
    val isLight: Boolean,
)

val LightSupportColors = SupportColors(
    accent = BrandAccent,
    onAccent = BrandControl,
    accentSoft = BrandInkSoft,
    onAccentSoft = BrandInk,
    contentTertiary = ContentTertiaryLight,
    overlay = Color(0x140B3A57),
    negative = Sentiment(Color(0xFFCB272F), Color(0xFFFBEAEA), Color(0xFFCB272F), Color.White),
    positive = Sentiment(Color(0xFF054D28), Color(0xFFE2F6D5), Color(0xFF054D28), Color(0xFFE2F6D5)),
    warning = Sentiment(Color(0xFF4A3B1C), Color(0xFFFFF7D7), Color(0xFFFFD11A), Color(0xFF4A3B1C)),
    proposition = Sentiment(Color(0xFF054D4D), Color(0xFFE0F7F7), Color(0xFF054D4D), Color.White),
    isLight = true,
)

val DarkSupportColors = SupportColors(
    accent = BrandAccent,
    onAccent = BrandControl,
    accentSoft = BrandInkSoftDark,
    onAccentSoft = BrandAccent,
    contentTertiary = ContentTertiaryDark,
    overlay = Color(0x33FFFFFF),
    negative = Sentiment(Color(0xFFFFA8AD), Color(0xFF410B0D), Color(0xFFFFA8AD), Color(0xFF410B0D)),
    positive = Sentiment(Color(0xFFCEF1B8), Color(0xFF054D28), Color(0xFFBAE5A0), Color(0xFF252C20)),
    warning = Sentiment(Color(0xFFFADC65), Color(0xFF3A3523), Color(0xFFFADC65), Color(0xFF3A3523)),
    proposition = Sentiment(Color(0xFFE0F7F7), Color(0xFF054D4D), Color(0xFFE0F7F7), Color(0xFF0B312F)),
    isLight = false,
)

val LocalSupportColors = staticCompositionLocalOf { LightSupportColors }

// ---------------------------------------------------------------------------
// Material schemes
// ---------------------------------------------------------------------------

/*
 * Token mapping:
 *
 *   primary                 interactive text and glyphs: links, selected tab, text buttons,
 *                           badges. Deep ink in light, the accent in dark.
 *   primaryContainer        THE accent fill (primary button). onPrimaryContainer is the control
 *                           ink that prints on it.
 *   secondaryContainer      the soft accent tint (secondary button, outgoing bubble)
 *   background / surface    the screen. App bars are the screen colour too.
 *   surfaceContainerLow     neutral tint: grouped cards, list groups, info boxes
 *   surfaceContainer        elevated: dialogs, menus. White in light, one step up in dark.
 *   surfaceContainerHigh    search pills, incoming bubbles, pressed rows
 *   surfaceTint             transparent, so tonal elevation never tints anything blue.
 */

val LightColors = lightColorScheme(
    primary = BrandInk,
    onPrimary = Color.White,
    primaryContainer = BrandAccent,
    onPrimaryContainer = BrandControl,
    inversePrimary = BrandAccent,
    secondary = BrandInk,
    onSecondary = Color.White,
    secondaryContainer = BrandInkSoft,
    onSecondaryContainer = BrandInk,
    tertiary = Color(0xFF054D4D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F7F7),
    onTertiaryContainer = Color(0xFF054D4D),
    background = ScreenLight,
    onBackground = ContentPrimaryLight,
    surface = ScreenLight,
    onSurface = ContentPrimaryLight,
    surfaceVariant = NeutralLight,
    onSurfaceVariant = ContentSecondaryLight,
    surfaceTint = Color.Transparent,
    surfaceBright = ScreenLight,
    surfaceDim = NeutralStrongLight,
    surfaceContainerLowest = ScreenLight,
    surfaceContainerLow = NeutralLight,
    surfaceContainer = ScreenLight,
    surfaceContainerHigh = NeutralStrongLight,
    surfaceContainerHighest = NeutralStrongestLight,
    outline = Color(0xFFC3CACF),
    outlineVariant = BorderLight,
    inverseSurface = ElevatedDark,
    inverseOnSurface = ContentPrimaryDark,
    error = Color(0xFFCB272F),
    onError = Color.White,
    errorContainer = Color(0xFFFBEAEA),
    onErrorContainer = Color(0xFF9B141B),
    scrim = Color(0xFF000000),
)

val DarkColors = darkColorScheme(
    primary = BrandAccent,
    onPrimary = BrandControl,
    primaryContainer = BrandAccent,
    onPrimaryContainer = BrandControl,
    inversePrimary = BrandInk,
    secondary = BrandAccent,
    onSecondary = BrandControl,
    secondaryContainer = BrandInkSoftDark,
    onSecondaryContainer = BrandAccent,
    tertiary = Color(0xFFE0F7F7),
    onTertiary = Color(0xFF054D4D),
    tertiaryContainer = Color(0xFF054D4D),
    onTertiaryContainer = Color(0xFFE0F7F7),
    background = ScreenDark,
    onBackground = ContentPrimaryDark,
    surface = ScreenDark,
    onSurface = ContentPrimaryDark,
    surfaceVariant = NeutralDark,
    onSurfaceVariant = ContentSecondaryDark,
    surfaceTint = Color.Transparent,
    surfaceBright = NeutralStrongDark,
    surfaceDim = ScreenDark,
    surfaceContainerLowest = Color(0xFF0C0F11),
    surfaceContainerLow = NeutralDark,
    surfaceContainer = ElevatedDark,
    surfaceContainerHigh = NeutralStrongDark,
    surfaceContainerHighest = NeutralStrongestDark,
    outline = Color(0xFF4A5055),
    outlineVariant = BorderDark,
    inverseSurface = ContentPrimaryDark,
    inverseOnSurface = ScreenDark,
    error = Color(0xFFFFA8AD),
    onError = Color(0xFF410B0D),
    errorContainer = Color(0xFF410B0D),
    onErrorContainer = Color(0xFFFFA8AD),
    scrim = Color(0xFF000000),
)
