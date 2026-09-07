package com.codexce.supportchat.data

import android.content.Context
import com.codexce.supportchat.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Points the app at the local Firebase emulator suite instead of the real project.
 *
 * Two ways in, and both are deliberately narrow:
 *
 *  - USE_EMULATORS, a BuildConfig flag that is false for release, so a plain debug build can be
 *    wired to a laptop by flipping one constant.
 *  - The hidden developer mode. Once unlocked (tap the version footer seven times) its emulator
 *    toggle sets a persisted preference that this reads at startup. This is the path that makes a
 *    real, installed build testable against emulators without recompiling, which is exactly what
 *    local testing on a phone needs. It can only be reached from behind the dev-mode gate, and
 *    turning dev mode off clears it.
 *
 * The host is 10.0.2.2 by default, which is how the Android emulator reaches the host machine's
 * localhost. On a physical device that address means nothing — set your machine's LAN address in
 * the developer screen's host field.
 */
object Emulators {

    /** True when either the build flag or the developer-mode override asks for emulators. */
    fun isEnabled(context: Context): Boolean =
        BuildConfig.USE_EMULATORS || AppPreferences.get(context).emulatorEnabledAtStartup

    /**
     * The host to redirect to. A developer-mode override, when set, wins over the build-time
     * default so a physical device can point at a LAN address without a rebuild.
     */
    fun hostFor(context: Context): String {
        val prefs = AppPreferences.get(context)
        return if (prefs.emulatorEnabledAtStartup && prefs.emulatorHostAtStartup.isNotBlank()) {
            prefs.emulatorHostAtStartup
        } else {
            BuildConfig.EMULATOR_HOST
        }
    }

    private const val AUTH_PORT = 9099
    private const val DATABASE_PORT = 9000
    private const val FIRESTORE_PORT = 8080

    /**
     * Call once, before anything touches Auth, Firestore or the database. All three SDKs throw if
     * they are redirected after first use, which is why this runs from the startup initializer and
     * nowhere else — and why changing the emulator setting needs an app restart to take effect.
     */
    fun connect(context: Context) {
        if (!isEnabled(context)) return
        val host = hostFor(context)

        FirebaseAuth.getInstance().useEmulator(host, AUTH_PORT)
        FirebaseDatabase.getInstance(DATABASE_URL).useEmulator(host, DATABASE_PORT)
        FirebaseFirestore.getInstance().useEmulator(host, FIRESTORE_PORT)
    }
}
