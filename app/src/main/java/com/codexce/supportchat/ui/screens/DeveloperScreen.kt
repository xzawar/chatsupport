@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.codexce.supportchat.ui.screens

import com.codexce.supportchat.ui.components.responsiveContent
import com.codexce.supportchat.ui.theme.supportFieldColors
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codexce.supportchat.data.AppPreferences
import com.codexce.supportchat.ui.components.AppIcons
import com.codexce.supportchat.ui.components.AppToast
import com.codexce.supportchat.ui.components.BackButton
import com.codexce.supportchat.ui.components.GroupGap
import com.codexce.supportchat.ui.components.SettingsGroup
import com.codexce.supportchat.ui.components.SettingsRow
import com.codexce.supportchat.ui.components.SupportCard
import com.codexce.supportchat.ui.components.debounced
import com.codexce.supportchat.ui.theme.supportButtonColors
import com.codexce.supportchat.ui.theme.supportSwitchColors

private val TintOrange = Color(0xFFF97316)
private val TintRed = Color(0xFFEF4444)

/**
 * The hidden developer mode's home. Only reachable once the mode is unlocked (seven taps on the
 * version footer in Settings), and only shown while it is on.
 *
 * Everything here is a local-testing convenience that would be wrong on a normal install, which
 * is why it lives behind the gate: an emulator override that survives a real install without a
 * rebuild, and the plain-words reminder that local website registration is unblocked while dev
 * mode is on.
 */
@Composable
fun DeveloperScreen(
    preferences: AppPreferences,
    onBack: () -> Unit,
) {
    val emulatorEnabled by preferences.emulatorEnabled.collectAsStateWithLifecycle()
    val emulatorHost by preferences.emulatorHost.collectAsStateWithLifecycle()
    var hostField by remember { mutableStateOf(emulatorHost) }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onBack) },
                title = { Text("Developer", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { insets ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState()).responsiveContent(),
        ) {
            GroupGap()
            Column(
                Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SupportCard {
                    Text(
                        text = "Developer mode is on",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.padding(top = 6.dp))
                    Text(
                        text = "These options are for local testing only. Leave them off for a " +
                            "normal install.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            GroupGap()
            SettingsGroup {
                SettingsRow(
                    icon = AppIcons.DatabaseSolid,
                    title = "Use Firebase emulators",
                    tint = TintOrange,
                    trailing = {
                        Switch(
                            colors = supportSwitchColors(),
                            checked = emulatorEnabled,
                            onCheckedChange = { enabled ->
                                preferences.setEmulatorEnabled(enabled)
                                AppToast.show(
                                    if (enabled) "Emulators on. Restart the app to apply."
                                    else "Emulators off. Restart the app to apply.",
                                )
                            },
                        )
                    },
                )
            }

            GroupGap()
            Column(
                Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SupportCard {
                    Text(
                        text = "Emulator host",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.padding(top = 8.dp))
                    OutlinedTextField(
                        value = hostField,
                        onValueChange = { hostField = it },
                        label = { Text("Host or LAN IP") },
                        placeholder = { Text("10.0.2.2") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = supportFieldColors(),
                    )
                    Spacer(Modifier.padding(top = 12.dp))
                    Button(
                        colors = supportButtonColors(),
                        onClick = debounced {
                            preferences.setEmulatorHost(hostField.trim())
                            AppToast.show("Host saved. Restart the app to apply.")
                        },
                    ) { Text("Save host") }
                    Spacer(Modifier.padding(top = 8.dp))
                    Text(
                        text = "Auth 9099 \u00b7 Firestore 8080 \u00b7 Database 9000. " +
                            "10.0.2.2 is the Android emulator's route to your computer; on a " +
                            "real phone use your machine's LAN IP. Restart the app after " +
                            "changing this \u2014 Firebase can only be redirected before its " +
                            "first use.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                SupportCard {
                    Text(
                        text = "Local & LAN websites",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.padding(top = 6.dp))
                    Text(
                        text = "With developer mode on, Link your website accepts localhost, " +
                            ".local, LAN IPs and http:// origins. Type the domain in and press " +
                            "Register \u2014 no QR scan needed. Open Settings \u2192 Link your " +
                            "website and enter e.g. enco.local.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            GroupGap()
            Button(
                onClick = debounced {
                    preferences.setDevMode(false)
                    AppToast.show("Developer mode turned off")
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TintRed,
                    contentColor = Color.White,
                ),
            ) {
                Icon(
                    painter = painterResource(AppIcons.LogoutSolid),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Turn off developer mode")
            }
            Spacer(Modifier.padding(top = 24.dp))
        }
    }
}
