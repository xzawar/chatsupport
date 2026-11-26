package com.codexce.supportchat.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.codexce.supportchat.ui.theme.DarkColors
import com.codexce.supportchat.ui.theme.DarkSupportColors
import com.codexce.supportchat.ui.theme.LightColors
import com.codexce.supportchat.ui.theme.LightSupportColors
import com.codexce.supportchat.ui.theme.SupportColors
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the colour system: every pair the app prints text with must clear WCAG AA (4.5:1),
 * and the brand accent must never be treated as a text colour on a light surface.
 *
 * A palette change that breaks legibility fails here instead of on someone's phone.
 */
class ThemeContrastTest {

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance() + 0.05
        val lb = b.luminance() + 0.05
        return (maxOf(la, lb) / minOf(la, lb)).toDouble()
    }

    private fun assertAA(name: String, fg: Color, bg: Color, min: Double = 4.5) {
        val ratio = contrast(fg, bg)
        assertTrue("$name is ${"%.2f".format(ratio)}:1, needs $min:1", ratio >= min)
    }

    private fun checkScheme(label: String, s: ColorScheme, c: SupportColors) {
        assertAA("$label body text on screen", s.onBackground, s.background, 7.0)
        assertAA("$label supporting text on screen", s.onSurfaceVariant, s.background)
        assertAA("$label supporting text on grouped card", s.onSurfaceVariant, s.surfaceContainerLow)
        assertAA("$label tertiary text on screen", c.contentTertiary, s.background)
        assertAA("$label tertiary text on grouped card", c.contentTertiary, s.surfaceContainerLow)
        assertAA("$label links and selected tab", s.primary, s.background)
        assertAA("$label label on primary fill", s.onPrimary, s.primary)
        assertAA("$label label on accent (primary button)", c.onAccent, c.accent)
        assertAA("$label label on primary container", s.onPrimaryContainer, s.primaryContainer)
        assertAA("$label label on secondary button", c.onAccentSoft, c.accentSoft)
        assertAA("$label text in outgoing bubble", s.onSurface, c.accentSoft)
        assertAA("$label text in incoming bubble", s.onSurface, s.surfaceContainerLow)
        assertAA("$label text in dialogs", s.onSurface, s.surfaceContainer)
        assertAA("$label toast", s.inverseOnSurface, s.inverseSurface)
        assertAA("$label error text", s.onErrorContainer, s.errorContainer)
        assertAA("$label on error fill", s.onError, s.error)
        listOf(c.negative, c.positive, c.warning, c.proposition).forEachIndexed { i, sentiment ->
            assertAA("$label sentiment $i content on surface", sentiment.content, sentiment.surface)
            assertAA("$label sentiment $i on strong fill", sentiment.onStrong, sentiment.strong)
        }
    }

    @Test fun lightSchemeIsLegible() = checkScheme("light", LightColors, LightSupportColors)

    @Test fun darkSchemeIsLegible() = checkScheme("dark", DarkColors, DarkSupportColors)

    @Test fun accentIsNeverLightModeText() {
        // The yellow is a fill. If it ever becomes primary (text/link colour) in light mode,
        // links and selected tabs drop to ~1.5:1 on white.
        assertTrue(contrast(LightColors.primary, LightColors.background) > 7.0)
        assertTrue(contrast(LightSupportColors.accent, LightColors.background) < 3.0)
    }

    @Test fun surfacesAreOpaque() {
        // Translucent containers alpha-blend on every frame and break components that expect an
        // opaque container (menus, dialogs, text fields).
        for (s in listOf(LightColors, DarkColors)) {
            for (c in listOf(s.background, s.surface, s.surfaceContainerLow, s.surfaceContainer,
                s.surfaceContainerHigh, s.surfaceContainerHighest)) {
                assertTrue("surface $c must be opaque", c.alpha == 1f)
            }
        }
    }
}
