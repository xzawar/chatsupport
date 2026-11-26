package com.codexce.supportchat.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codexce.supportchat.R
import com.codexce.supportchat.ui.components.ErrorBanner
import com.codexce.supportchat.ui.components.PrimaryButton
import com.codexce.supportchat.ui.components.debounced
import com.codexce.supportchat.ui.theme.Motion
import com.codexce.supportchat.ui.theme.PillShape
import com.codexce.supportchat.ui.theme.Spacing
import com.codexce.supportchat.ui.theme.SupportTheme
import com.codexce.supportchat.ui.theme.SupportType
import com.codexce.supportchat.ui.theme.supportFieldColors
import com.codexce.supportchat.ui.theme.supportNeutralButtonColors
import com.codexce.supportchat.ui.theme.supportSecondaryButtonColors
import com.codexce.supportchat.viewmodel.AuthViewModel

/*
 * Login gate, as an info screen in the calm-fintech DNA:
 *
 *   illustration on a soft brand panel (32dp radius, inset from the edges)
 *   ONE display line in the heavy caps face - the only one in the app's whole first run
 *   one supporting sentence in the second person
 *   the footer actions, stacked, primary-style last
 *
 * Follows the theme, so it is correct in dark mode instead of being a fixed cream page.
 *
 * Button hierarchy, and why. Google's branding rules want its button white, dark or neutral, so
 * it is the neutral pill with the full-colour G. Email is the secondary tint. The accent - the
 * one bright thing - only appears once there is something to submit: the Sign in button inside
 * the email fields. Until then nothing on this screen is shouting.
 *
 * Motion: the panel and the actions fade in with a short stagger on the enter curve; opening the
 * email fields shrinks the illustration instead of growing a scrollbar. Both animate a single
 * value read in the draw/layout phase only.
 */

private const val IntroFadeMs = 450
private const val SheetDelayMs = 120
private const val LabelFadeMs = 180

@Composable
fun LoginScreen(authViewModel: AuthViewModel) {
    val auth by authViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var emailExpanded by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    // Saveable so a rotation or process restore does not replay the intro.
    var entered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val heroAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(IntroFadeMs, easing = Motion.EaseEnter),
        label = "heroAlpha",
    )
    val sheetAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(IntroFadeMs, delayMillis = SheetDelayMs, easing = Motion.EaseEnter),
        label = "sheetAlpha",
    )

    val baseHeroHeight = (LocalConfiguration.current.screenHeightDp * 0.42f).dp
        .coerceIn(220.dp, 380.dp)
    val heroHeight by animateDpAsState(
        targetValue = if (emailExpanded) baseHeroHeight * 0.55f else baseHeroHeight,
        animationSpec = tween(Motion.SURFACE_MILLIS, easing = Motion.EaseEnter),
        label = "heroHeight",
    )

    LaunchedEffect(emailExpanded, scrollState.maxValue) {
        if (emailExpanded && scrollState.maxValue > 0) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    val canSubmit = !auth.submitting && email.isNotBlank() && password.isNotBlank()
    val submit = { if (canSubmit) authViewModel.signIn(email, password) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = Spacing.maxContentWidth)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp),
        ) {
            // The illustration panel: a floating card inset 8dp, 32dp corners.
            Box(
                Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .height(heroHeight)
                    .graphicsLayer { alpha = heroAlpha }
                    .clip(RoundedCornerShape(32.dp))
                    .background(SupportTheme.colors.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.login_hero),
                    contentDescription = null,
                    modifier = Modifier
                        .size(heroHeight * 0.56f)
                        .clip(RoundedCornerShape(percent = 50)),
                )
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .alpha(sheetAlpha)
                    .padding(horizontal = 8.dp)
                    .padding(top = Spacing.betweenSections, bottom = Spacing.contentToButton),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "SUPPORT CHAT",
                    style = SupportType.display,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.component))
                Text(
                    text = "Answer your website visitors from anywhere.",
                    style = SupportType.largeBody,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                AnimatedVisibility(
                    visible = auth.error != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Spacer(Modifier.height(Spacing.component))
                        ErrorBanner(
                            message = auth.error.orEmpty(),
                            onDismiss = authViewModel::clearError,
                            detail = auth.errorDetail,
                            modifier = Modifier.padding(horizontal = 0.dp),
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.contentToButton))

                // Google: the neutral pill with the full-colour G (not tinted, per its brand rules).
                Button(
                    onClick = debounced {
                        authViewModel.signInWithGoogle(
                            context = context,
                            serverClientId = context.getString(R.string.google_web_client_id),
                        )
                    },
                    enabled = !auth.submitting,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.buttonHeight),
                    shape = PillShape,
                    elevation = null,
                    colors = supportNeutralButtonColors(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_google_g),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(12.dp))
                        Text("Continue with Google", style = SupportType.largeBodyBold)
                    }
                }

                Spacer(Modifier.height(Spacing.betweenCards))

                Button(
                    onClick = debounced { emailExpanded = !emailExpanded },
                    enabled = !auth.submitting,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.buttonHeight),
                    shape = PillShape,
                    elevation = null,
                    colors = supportSecondaryButtonColors(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Crossfade(
                        targetState = emailExpanded,
                        animationSpec = tween(LabelFadeMs, easing = Motion.EaseStandard),
                        label = "emailMethodLabel",
                    ) { expanded ->
                        Text(
                            text = if (expanded) "Use Google instead" else "Continue with email",
                            style = SupportType.largeBodyBold,
                        )
                    }
                }

                AnimatedVisibility(
                    visible = emailExpanded,
                    enter = fadeIn(tween(Motion.STANDARD_MILLIS, easing = Motion.EaseEnter)) +
                        expandVertically(tween(Motion.STANDARD_MILLIS, easing = Motion.EaseEnter)),
                    exit = fadeOut(tween(Motion.FAST_MILLIS, easing = Motion.EaseExit)) +
                        shrinkVertically(tween(Motion.FAST_MILLIS, easing = Motion.EaseExit)),
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Spacer(Modifier.height(Spacing.contentToButton))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Email") },
                            singleLine = true,
                            enabled = !auth.submitting,
                            textStyle = SupportType.largeBody,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                            ),
                            shape = RoundedCornerShape(16.dp),
                            colors = supportFieldColors(),
                        )
                        Spacer(Modifier.height(Spacing.betweenCards))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Password") },
                            singleLine = true,
                            enabled = !auth.submitting,
                            textStyle = SupportType.largeBody,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                focus.clearFocus()
                                submit()
                            }),
                            shape = RoundedCornerShape(16.dp),
                            colors = supportFieldColors(),
                        )
                        Spacer(Modifier.height(Spacing.contentToButton))
                        PrimaryButton(
                            text = "Sign in",
                            onClick = { submit() },
                            enabled = email.isNotBlank() && password.isNotBlank(),
                            loading = auth.submitting,
                        )
                    }
                }
            }
        }
    }
}
