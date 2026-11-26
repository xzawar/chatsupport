package com.codexce.supportchat.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.codexce.supportchat.ui.navigation.TopLevelTab
import com.codexce.supportchat.ui.theme.Motion
import com.codexce.supportchat.ui.theme.Spacing
import com.codexce.supportchat.ui.theme.SupportTheme
import com.codexce.supportchat.ui.theme.SupportType

/**
 * Height of the bar's content, not counting the system navigation inset under it. Screens and
 * the toast read this to clear the bar; the inset is added by whoever needs it.
 */
val TabBarHeight: Dp = 64.dp

/**
 * The bottom navigation: docked, flat, the screen's own colour, with a hairline on top.
 *
 * It used to be a floating pill with a 12dp shadow. The calm-UI rule is that controls have no
 * elevation and surfaces separate by tint or a 12% line, and a shadow under a bar that never
 * moves is also a layer the GPU re-renders every frame something scrolls behind it.
 *
 * Selected tab: filled glyph, ink colour, SemiBold label and a soft-tint pill behind the icon.
 * The pill scales in on the draw phase only (graphicsLayer), so changing tabs recomposes the
 * two tabs that changed and nothing else.
 *
 * The name is kept so call sites and AppToast do not change.
 */
@Composable
fun FloatingTabBar(
    tabs: List<TopLevelTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(Dp.Hairline)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(TabBarHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Row(Modifier.widthIn(max = Spacing.maxContentWidth).fillMaxHeight()) {
                tabs.forEachIndexed { index, tab ->
                    TabButton(
                        tab = tab,
                        selected = index == selectedIndex,
                        onClick = debounced { onSelect(index) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    tab: TopLevelTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val indicator by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = Motion.tabFloat,
        label = "indicator",
    )
    val tint by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = androidx.compose.animation.core.tween(Motion.TAB_MILLIS, easing = Motion.EaseStandard),
        label = "tint",
    )
    val indicatorColor = SupportTheme.colors.accentSoft

    Column(
        modifier = modifier
            .fillMaxHeight()
            .selectable(
                selected = selected,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 36.dp),
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .width(56.dp)
                    .height(30.dp)
                    .graphicsLayer {
                        scaleX = 0.6f + 0.4f * indicator
                        alpha = indicator
                    }
                    .clip(CircleShape)
                    .background(indicatorColor),
            )
            Icon(
                painter = painterResource(if (selected) tab.filledIcon else tab.outlineIcon),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = tab.label,
            style = if (selected) SupportType.navBold else SupportType.nav,
            color = tint,
            maxLines = 1,
        )
    }
}
