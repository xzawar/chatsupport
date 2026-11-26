package com.codexce.supportchat.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.codexce.supportchat.R

// ---------------------------------------------------------------------------
// Shape. Interactive = pill, containers = 16, big immersive containers = 24-32. Never sharp.
// ---------------------------------------------------------------------------

val SupportShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Cards, list groups, inputs, banners: the most common container shape. */
val GroupedCardShape = RoundedCornerShape(16.dp)

/** Leading icon containers are circles on the neutral tint, not coloured squircles. */
val SettingsTileShape = CircleShape

/** Every button, chip, badge and icon button. */
val PillShape = CircleShape

// ---------------------------------------------------------------------------
// Type. Inter in two working weights (400 reads, 600 points), 500 only for small group titles.
// Anton stands in for a heavy caps display face, used at most once per screen.
// ---------------------------------------------------------------------------

/*
 * Bundled rather than downloadable: no network round trip on first launch, no layout shift
 * when a downloaded face swaps in, and the files are latin + latin-ext subsets (~140KB each).
 * Glyphs outside those ranges fall back to the system font per character.
 *
 * Only 400/500/600 are shipped. A FontWeight.Bold request resolves to the 600 file instead of
 * a fake-emboldened 400, which is exactly the two-weight rule enforced by the font itself.
 */
val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

val DisplayFamily = FontFamily(Font(R.font.anton_regular, FontWeight.Normal))

private fun inter(weight: FontWeight, size: Float, line: Float, tracking: Float) = TextStyle(
    fontFamily = InterFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

/** The extended scale. Material's slots below map onto these. */
@Immutable
object SupportType {
    val screenTitle = inter(FontWeight.SemiBold, 30f, 34.5f, -0.021f)
    val sectionTitle = inter(FontWeight.SemiBold, 26f, 31.7f, -0.020f)
    val subsectionTitle = inter(FontWeight.SemiBold, 22f, 27.5f, -0.018f)
    val bodyTitle = inter(FontWeight.SemiBold, 18f, 24.3f, -0.014f)
    val groupTitle = inter(FontWeight.Medium, 14f, 19.6f, -0.006f)
    val largeBody = inter(FontWeight.Normal, 16f, 24f, -0.011f)
    val largeBodyBold = inter(FontWeight.SemiBold, 16f, 24f, -0.011f)
    val body = inter(FontWeight.Normal, 14f, 21.7f, -0.006f)
    val bodyBold = inter(FontWeight.SemiBold, 14f, 21.7f, -0.006f)
    val nav = inter(FontWeight.Normal, 12f, 18f, 0f)
    val navBold = inter(FontWeight.SemiBold, 12f, 18f, 0f)

    /** Heavy, caps-only, set tighter than its size. One per screen, never in lists. */
    val display = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 44.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.em,
    )

    /** Hero numbers: counts and amounts that are the point of the screen. */
    val displayNumber = inter(FontWeight.SemiBold, 40f, 44f, -0.022f)
}

val SupportTypography = Typography(
    displayLarge = SupportType.display.copy(fontSize = 64.sp, lineHeight = 56.sp),
    displayMedium = SupportType.display.copy(fontSize = 52.sp, lineHeight = 46.sp),
    displaySmall = SupportType.display,
    headlineLarge = SupportType.screenTitle,
    headlineMedium = SupportType.sectionTitle,
    headlineSmall = SupportType.subsectionTitle,
    titleLarge = SupportType.subsectionTitle,
    titleMedium = SupportType.largeBodyBold,
    titleSmall = SupportType.bodyBold,
    bodyLarge = SupportType.largeBody,
    bodyMedium = SupportType.body,
    bodySmall = inter(FontWeight.Normal, 12f, 18f, 0f),
    // Buttons and text buttons: SemiBold, never Medium - weight is what marks something tappable.
    labelLarge = SupportType.bodyBold,
    labelMedium = SupportType.navBold,
    // Never below 12sp.
    labelSmall = inter(FontWeight.Medium, 12f, 16f, 0f),
)

// ---------------------------------------------------------------------------
// Spacing. Named by relationship, not by size.
// ---------------------------------------------------------------------------

@Immutable
object Spacing {
    val betweenText = 8.dp
    val textToComponent = 16.dp
    val component = 16.dp
    val contentToButton = 24.dp
    val betweenSections = 32.dp
    val gutter = 16.dp
    val betweenCards = 12.dp
    val betweenChips = 8.dp

    /** Primary and secondary buttons: 12dp vertical padding + a 24dp line. */
    val buttonHeight = 48.dp

    /** Round icon buttons in app bars, avatars in list rows. */
    val iconButton = 40.dp

    /** On wide screens the content column stays phone-width and centred. */
    val maxContentWidth = 600.dp
}

/** Accessors for the roles Material has no slot for. */
object SupportTheme {
    val colors: SupportColors
        @Composable @ReadOnlyComposable
        get() = LocalSupportColors.current
}

@Composable
fun SupportChatTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalSupportColors provides if (darkTheme) DarkSupportColors else LightSupportColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = SupportTypography,
            shapes = SupportShapes,
            content = content,
        )
    }
}
