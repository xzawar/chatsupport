@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.codexce.supportchat.ui.screens

import com.codexce.supportchat.ui.components.responsiveContent
import com.codexce.supportchat.ui.theme.supportFieldColors
import androidx.compose.foundation.shape.RoundedCornerShape
import com.codexce.supportchat.ui.components.SecondaryButton
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.codexce.supportchat.data.api.ApiException
import com.codexce.supportchat.data.api.SitePairing
import com.codexce.supportchat.data.api.SupportApi
import com.codexce.supportchat.data.api.Website
import com.codexce.supportchat.data.api.WebsiteSecret
import com.codexce.supportchat.ui.components.AppIcons
import com.codexce.supportchat.ui.components.BackButton
import com.codexce.supportchat.ui.components.debounced
import com.codexce.supportchat.ui.components.EmptyState
import com.codexce.supportchat.ui.components.ErrorBanner
import com.codexce.supportchat.ui.components.CardListSkeleton
import com.codexce.supportchat.ui.components.GroupGap
import com.codexce.supportchat.ui.components.StatusPill
import com.codexce.supportchat.ui.components.SupportCard
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch
import com.codexce.supportchat.ui.theme.supportButtonColors

/**
 * Website registration.
 *
 * Pairing codes are gone. A website is now a row the backend owns, and the only secret is the
 * API key it mints when the site is registered. That key is returned exactly once, in the
 * response to the create call, and is never stored in a readable form afterwards - the backend
 * keeps a hash. So the copy button below is genuinely the only chance to take the key, and the
 * screen says so rather than implying it can be looked up again later.
 */
@Composable
fun LinkWebsiteScreen(
    signedInEmail: String?,
    onBack: () -> Unit,
    devMode: Boolean = false,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var websites by remember { mutableStateOf<List<Website>?>(null) }
    var domain by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // The freshly created site, held in memory only, for as long as this screen lives. Only its
    // short link code is ever put on screen; the raw key inside it is never rendered.
    var revealed by remember { mutableStateOf<WebsiteSecret?>(null) }
    // The website that asked to be linked, if this registration started from a scan. Held only
    // until the code has been handed back to it.
    var pairing by remember { mutableStateOf<SitePairing?>(null) }
    var pairingNote by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        runCatching { SupportApi.websites() }
            .onSuccess { websites = it }
            .onFailure { failure ->
                websites = emptyList()
                error = (failure as? ApiException)?.message
                    ?: failure.localizedMessage
                    ?: "Could not load your websites"
            }
    }

    /*
     * One active domain at a time. The backend already refuses a second one - createWebsite
     * throws website_limit_reached inside the transaction - but letting someone fill in a form
     * and press Register only to be told no is a poor way to communicate a hard rule. The form
     * is closed while a live site exists and opens again the moment Remove deactivates it.
     */
    val hasActiveSite = websites?.any { it.active } == true

    /*
     * Scan-to-link: the other half of the plugin's "Show QR code" button.
     *
     * The code carries a session id and nothing else. This reads that session out of Firestore,
     * fills in the domain the site asked about, and the ordinary Register button below does the
     * rest. No HTTP client is involved and the phone never contacts the website, which is what
     * makes a localhost or staging install pairable at all.
     */
    fun openScannedSession(raw: String) {
        val value = raw.trim()

        // The computer flow's codes are sc1:<session>:<secret>. One scanned here would otherwise
        // fail with something unhelpful about a session that does not exist.
        if (value.startsWith("sc1:")) {
            error = "That is a computer pairing code. Use \"Link a computer\" in Settings for it."
            return
        }

        if (!value.startsWith("sc2:")) {
            error = "That is not a website pairing code. In the plugin, press \"Show QR code\"."
            return
        }

        // One website per account, checked here so the refusal arrives before the camera work
        // rather than after a filled-in form.
        if (hasActiveSite) {
            error = "Remove your existing site first, then scan this code again."
            return
        }

        val session = value.removePrefix("sc2:")
        working = true
        error = null
        pairingNote = null
        scope.launch {
            runCatching { SupportApi.sitePairing(session) }
                .onSuccess { found ->
                    pairing = found
                    domain = found.domain
                    pairingNote = "Ready to link " +
                        (found.siteName?.takeIf { it.isNotBlank() } ?: found.domain) +
                        ". Press Register and the code is sent back for you."
                }
                .onFailure { failure ->
                    pairing = null
                    error = (failure as? ApiException)?.message
                        ?: failure.localizedMessage
                        ?: "That code could not be read"
                }
            working = false
        }
    }

    fun scanSession() {
        error = null
        GmsBarcodeScanning.getClient(context).startScan()
            .addOnSuccessListener { barcode ->
                val raw = barcode.rawValue
                if (raw == null) error = "No QR code was detected." else openScannedSession(raw)
            }
            .addOnCanceledListener { }
            .addOnFailureListener { error = it.message ?: "Scanner could not open." }
    }

    LaunchedEffect(Unit) { reload() }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onBack) },
                title = { Text("Link your website", style = MaterialTheme.typography.titleLarge) },
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
            error?.let { message ->
                ErrorBanner(message = message, onDismiss = { error = null })
            }

            GroupGap()
            Column(
                Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SupportCard {
                    Text(
                        text = if (hasActiveSite) {
                            "You already have an active domain. Remove it below before " +
                                "registering a different one."
                        } else {
                            "Register the one domain the widget will run on. An account can " +
                                "link a single website, and requests from any other origin are " +
                                "refused, so the key alone is not enough to use it somewhere else."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (devMode) {
                        Spacer(Modifier.padding(top = 8.dp))
                        Text(
                            text = "Developer mode: local origins are accepted here \u2014 " +
                                "localhost, .local, LAN IPs like 192.168.1.5, and http:// " +
                                "addresses. Type the domain (e.g. enco.local) and press " +
                                "Register; no QR scan needed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.padding(top = 12.dp))
                    OutlinedTextField(
                        value = domain,
                        onValueChange = { domain = it },
                        label = { Text("Domain") },
                        placeholder = { Text("example.com") },
                        singleLine = true,
                        enabled = !working && !hasActiveSite,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = supportFieldColors(),
                    )
                    Spacer(Modifier.padding(top = 12.dp))
                    Button(
                        colors = supportButtonColors(),
                        onClick = debounced {
                            if (working) return@debounced
                            working = true
                            error = null
                            scope.launch {
                                runCatching { SupportApi.createWebsite(domain.trim()) }
                                    .onSuccess { created ->
                                        revealed = created
                                        domain = ""
                                        /*
                                         * If this came from a scan, hand the code back through the
                                         * same session. The website is already polling for it and
                                         * links itself; nothing is typed at either end.
                                         */
                                        val session = pairing
                                        val code = created.linkCode
                                        if (session != null && code != null) {
                                            runCatching {
                                                SupportApi.completeSitePairing(
                                                    session.sessionId,
                                                    code,
                                                    signedInEmail,
                                                )
                                            }
                                                .onSuccess {
                                                    pairing = null
                                                    pairingNote = "Sent to " + session.domain +
                                                        ". That site finishes linking itself in " +
                                                        "a few seconds."
                                                }
                                                .onFailure { failure ->
                                                    error = (failure as? ApiException)?.message
                                                        ?: "The site was registered, but the code " +
                                                            "could not be sent back. Type it in " +
                                                            "on the website instead."
                                                }
                                        }
                                        reload()
                                    }
                                    .onFailure { failure ->
                                        error = (failure as? ApiException)?.message
                                            ?: failure.localizedMessage
                                            ?: "Could not register that domain"
                                    }
                                working = false
                            }
                        },
                        enabled = !working && domain.isNotBlank() && !hasActiveSite,
                    ) {
                        if (working) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("Register")
                        }
                    }
                    Spacer(Modifier.padding(top = 8.dp))
                    SecondaryButton(
                        text = "Scan a site QR code",
                        onClick = { scanSession() },
                        enabled = !working && !hasActiveSite,
                    )
                    pairingNote?.let { note ->
                        Spacer(Modifier.padding(top = 8.dp))
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                /*
                 * The verification code, and nothing else.
                 *
                 * This card used to print the raw sk_live_ key, the site id and a warning about
                 * key handling. None of that is the owner's problem: the plugin never asks for
                 * the key, it asks for this short code and trades it for the key itself over the
                 * API. Showing the key here only created something worth leaking, and the site
                 * id is an internal identifier with no use outside a support ticket.
                 *
                 * The key still exists, and the summary row below still proves it exists - as a
                 * fingerprint, never in plaintext.
                 */
                revealed?.let { created ->
                    val code = created.linkCode
                    SupportCard {
                        Text(
                            text = "Your verification code",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.padding(top = 8.dp))
                        Text(
                            // A word rather than a dash. This slot is empty only while the code
                            // is still being generated, and "Generating" says that; a dash just
                            // looks like the code came back blank.
                            text = code ?: "Generating",
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.padding(top = 8.dp))
                        Text(
                            text = "Enter this code in the widget on " +
                                (created.domain ?: "your site") +
                                " to finish linking it. It is valid for a limited time; if it " +
                                "expires, rotate the key below to get a new one.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.padding(top = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                colors = supportButtonColors(),
                                onClick = debounced { code?.let { copyToClipboard(context, it) } },
                                enabled = code != null,
                            ) { Text("Copy code") }
                            TextButton(onClick = debounced { revealed = null }) { Text("Done") }
                        }
                    }
                }
            }

            GroupGap()
            val current = websites
            when {
                current == null -> CardListSkeleton(cards = 2, cardHeight = 88.dp)

                current.isEmpty() -> EmptyState(
                    icon = AppIcons.Cloud,
                    title = "No sites yet",
                    message = "Register a domain above to get the key the widget needs.",
                )

                else -> Column(
                    Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    for (site in current) {
                        WebsiteCard(
                            site = site,
                            busy = working,
                            onRotate = {
                                working = true
                                error = null
                                scope.launch {
                                    runCatching { SupportApi.rotateWebsiteKey(site.id) }
                                        .onSuccess { rotated ->
                                            revealed = rotated
                                            reload()
                                        }
                                        .onFailure { failure ->
                                            error = (failure as? ApiException)?.message
                                                ?: "Could not rotate that key"
                                        }
                                    working = false
                                }
                            },
                            onDelete = {
                                working = true
                                error = null
                                scope.launch {
                                    runCatching { SupportApi.deleteWebsite(site.id) }
                                        .onFailure { failure ->
                                            error = (failure as? ApiException)?.message
                                                ?: "Could not remove that site"
                                        }
                                    reload()
                                    working = false
                                }
                            },
                        )
                    }
                }
            }

            Text(
                text = "Signed in as " + (signedInEmail ?: "an unknown account") +
                    ". The widget calls the handshake endpoint with this key, and the reply " +
                    "carries a short-lived token for that one conversation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            )
        }
    }
}

@Composable
private fun WebsiteCard(
    site: Website,
    busy: Boolean,
    onRotate: () -> Unit,
    onDelete: () -> Unit,
) {
    SupportCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = site.domain,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            StatusPill(text = if (site.active) "Active" else "Disabled", emphasised = site.active)
        }
        Spacer(Modifier.padding(top = 4.dp))
        Text(
            // Masked by default. The fingerprint is enough to tell two keys apart and to confirm
            // one was issued, without ever putting the key itself on screen.
            text = site.keyFingerprint?.let { "Key \u2022\u2022\u2022\u2022 \u2022\u2022\u2022\u2022 " + it }
                ?: "No key issued yet",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.padding(top = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = debounced(onRotate), enabled = !busy) { Text("Rotate key") }
            TextButton(onClick = debounced(onDelete), enabled = !busy) { Text("Remove") }
        }
    }
}

private fun copyToClipboard(context: Context, value: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    manager?.setPrimaryClip(ClipData.newPlainText("API key", value))
}
