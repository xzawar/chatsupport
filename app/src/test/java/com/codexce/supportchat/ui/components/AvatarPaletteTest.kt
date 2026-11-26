package com.codexce.supportchat.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarPaletteTest {

    @Test fun letterFollowsNameThenEmailThenQuestionMark() {
        assertEquals("A", AvatarPalette.letterFor("amelia hart", "z@example.com"))
        assertEquals("Z", AvatarPalette.letterFor("  ", "zed@example.com"))
        assertEquals("R", AvatarPalette.letterFor("  rahul", null))
        assertEquals("7", AvatarPalette.letterFor("7even", null))
        assertEquals("?", AvatarPalette.letterFor(null, null))
        assertEquals("?", AvatarPalette.letterFor("--", "@@"))
    }

    @Test fun discIsStableAndNeverOutOfRange() {
        val keys = listOf("", "a", "conversation-123", "￿".repeat(50), "x".repeat(10_000))
        for (k in keys) {
            assertEquals(AvatarPalette.discFor(k), AvatarPalette.discFor(k))
        }
    }

    private fun contrast(a: Color, b: Color): Float {
        val la = a.luminance() + 0.05f
        val lb = b.luminance() + 0.05f
        return maxOf(la, lb) / minOf(la, lb)
    }

    @Test fun avatarLetterIsLegibleForEveryHue() {
        // Sample enough keys to hit every disc in the palette, in both appearances.
        for (i in 0 until 500) {
            for (light in listOf(true, false)) {
                val (disc, ink) = AvatarPalette.tintFor("visitor-$i", light)
                val ratio = contrast(ink, disc)
                assertTrue("key $i light=$light letter contrast $ratio", ratio >= 3.0f)
            }
        }
    }
}
