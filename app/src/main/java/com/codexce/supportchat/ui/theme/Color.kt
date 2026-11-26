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
 * The brand comes from the app icon: a sunny yellow tile (#FDD32B) with charcoal line art
 * (#333232) and white. Everything else is derived from those three. Swapping the brand means
 * editing the four values below and nothing else:
 *
 *   BrandAccent     the single bright fill: primary buttons, the "on" switch, badges, send
 *   BrandControl    text and glyphs printed ON the accent (charcoal, never white)
 *   BrandInk        interactive text in light mode: links, selected tabs, text buttons
 *   BrandInkSoft    the accent tint: secondary-button fill, outgoing chat bubble
 *
 * Contrast (WCAG, computed; UnitTest ThemeContrastTest enforces the text pairs):
 *   BrandControl on BrandAccent      9.9:1     ContentPrimary on white      18.1:1
 *   BrandInk on white               12.8:1     ContentSecondary on white     8.7:1
 *   BrandInk on BrandInkSoft        11.6:1     ContentTertiary on white      5.2:1
 *   Accent on dark screen           13.0:1     Dark primary text on screen  17.0:1
 *
 * The one hard rule the yellow imposes: it is 1.45:1 against white, so it is only ever a FILL
 * carrying charcoal content. Never yellow text, never a thin yellow line on a light surface.
 *
 * Surfaces are separated by tint, not by shadow or rule. The neutral fills are stored as
 * opaque, pre-composited values rather than translucent ones: they paint in one pass with no
 * alpha blend, and Material components that expect an opaque container (menus, dialogs, text
 * fields) behave correctly.
 */

// ---------------------------------------------------------------------------
// Brand slots
// ---------------------------------------------------------------------------

val BrandAccent = Color(0xFFFDD32B)
val BrandControl = Color(0xFF2B2A28)
val BrandInk = Color(0xFF333232)
val BrandInkSoft = Color(0xFFFFF4CC)

/** The accent-tint in the dark appearance: a deep olive-gold surface carrying yellow text. */
private val BrandInkSoftDark = Color(0xFF3A3216)

// ---------------------------------------------------------------------------
// Neutrals. Faintly warm so they sit with the yellow rather than against it.
// ---------------------------------------------------------------------------

internal val ContentPrimaryLight = Color(0xFF161614)
internal val ContentSecondaryLight = Color(0xFF4D4B46)
internal val ContentTertiaryLight = Color(0xFF6F6C66)

internal val ScreenLight = Color(0xFFFFFFFF)
internal val NeutralLight = Color(0xFFF5F4F0)        // grouped cards, inputs, avatars
private val NeutralStrongLight = Color(0xFFECEAE4)  // pressed rows, search pill on a neutral card
private val NeutralStrongestLight = Color(0xFFE2DFD8)
private val BorderLight = Color(0xFFE6E4DE)         // ink at 12% on white

internal val ContentPrimaryDark = Color(0xFFF5F4F0)
internal val ContentSecondaryDark = Color(0xFFCFCCC4)
internal val ContentTertiaryDark = Color(0xFFA19E97)

/** Never pure black: a warm black, so the yellow reads as sunlight rather than neon. */
internal val ScreenDark = Color(0xFF121211)
private val ElevatedDark = Color(0xFF1D1D1B)        // dialogs, menus, sheets
internal val NeutralDark = Color(0xFF282825)         // white at ~10% on the screen
private val NeutralStrongDark = Color(0xFF31312D)
private val NeutralStrongestDark = Color(0xFF3B3B37)
private val BorderDark = Color(0xFF34342F)

/** Window background colours, mirrored in res/values(-night)/colors.xml and MainActivity. */
const val WindowBackgroundLight = 0xFFFFFFFF
const val WindowBackgroundDark = 0xFF121211

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
    overlay = Color(0x14333232),
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
