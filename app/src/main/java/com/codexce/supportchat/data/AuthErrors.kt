package com.codexce.supportchat.data

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.codexce.supportchat.data.api.ApiException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Turns a sign-in failure into something worth showing someone.
 *
 * Before this existed, every failure path ended at `failure.localizedMessage`, which is written for
 * whoever is holding a stack trace. "A network error (such as timeout, interrupted connection or
 * unreachable host) has occurred." is a true sentence that tells a user nothing they can act on,
 * and the SHA-1 fingerprint instructions that used to appear under the Google button belong in a
 * setup document, not in front of a customer.
 *
 * So a failure now has two halves:
 *
 *  - [message] is one plain sentence, ending in something the person can actually do. It is the
 *    only half shown by default.
 *  - [detail] is the raw class name, Firebase error code and original text. It is shown only when
 *    developer mode is on, and it is deliberately not summarised, because the whole value of a
 *    diagnostic is that it was not edited.
 *
 * Nothing is discarded. The information that used to be in front of everyone is still one toggle
 * away, which is the difference between hiding a detail and losing it.
 */
data class AuthFailure(
    val message: String,
    val detail: String?,
)

object AuthErrors {

    /**
     * @param throwable What actually went wrong.
     * @param googleFlow Changes the wording, not the meaning. A cancelled Google sheet and a
     *   wrong password are both "sign-in failed", but the useful next step is different, and a
     *   message that names the wrong one reads as the app being confused.
     */
    fun describe(throwable: Throwable, googleFlow: Boolean = false): AuthFailure {
        val detail = diagnostic(throwable)

        val message = when (throwable) {
            /*
             * The one the user complained about.
             *
             * Firebase reports offline, DNS failure, a captive portal and a dead backend as the
             * same exception, so this cannot honestly say which. It says what to check, which is
             * the same answer for all four.
             */
            is FirebaseNetworkException,
            is UnknownHostException,
            is SocketTimeoutException,
            ->
                "Cannot reach the server. Check your internet connection and try again."

            /*
             * Deliberately does not say which of the two was wrong.
             *
             * Firebase merges wrong-password and no-such-account into one exception on newer SDKs
             * precisely so that sign-in cannot be used to discover which email addresses have
             * accounts. Splitting them here would hand that back.
             */
            is FirebaseAuthInvalidCredentialsException ->
                "That email or password is not right. Check both and try again."

            is FirebaseAuthInvalidUserException ->
                if (throwable.errorCode == "ERROR_USER_DISABLED") {
                    "This account has been disabled. Contact info@keykraftt.com."
                } else {
                    "That email or password is not right. Check both and try again."
                }

            is FirebaseTooManyRequestsException ->
                "Too many attempts. Wait a few minutes before trying again."

            is FirebaseAuthUserCollisionException ->
                "An account already exists for that email. Sign in instead."

            is FirebaseAuthWeakPasswordException ->
                "That password is too short. Use at least six characters."

            /*
             * Cancelled is not an error, and it used to be the loudest message on the screen -
             * four sentences about certificate fingerprints, shown to someone who had simply
             * changed their mind. The fingerprint case is real, but it belongs in [detail].
             */
            is GetCredentialCancellationException ->
                "Sign-in was cancelled."

            is NoCredentialException ->
                "No Google account on this device. Add one in Settings, or sign in with email."

            is ApiException -> fromApiCode(throwable)

            else -> if (googleFlow) {
                "Could not sign in with Google. Try again, or use email."
            } else {
                "Could not sign you in. Try again."
            }
        }

        return AuthFailure(message = message, detail = detail)
    }

    /**
     * The backend's own vocabulary, translated once.
     *
     * These codes are a contract between the functions and this app, so they are matched on
     * exactly rather than pattern-matched on their text. A code that is not listed falls through
     * to a generic line instead of leaking `feature_not_in_plan` onto a screen.
     */
    private fun fromApiCode(error: ApiException): String = when (error.code) {
        "unauthenticated" ->
            "Your session has expired. Sign in again."

        "tenant_not_provisioned" ->
            "Your workspace is still being set up. Try again in a moment."

        "owner_only", "permission_denied" ->
            "This account does not have access to that workspace. Sign in with the owner account."

        "feature_not_in_plan" ->
            "Your plan does not include this. Upgrade from Settings to use it."

        "subscription_inactive" ->
            "Your subscription is not active. Renew it from Settings to continue."

        "website_limit_reached" ->
            "You have reached the website limit for your plan."

        else ->
            "The server refused that request. Try again."
    }

    /**
     * The unedited version, for developer mode.
     *
     * Includes the exception class because two different causes routinely produce the same text,
     * and the Firebase error code because it is the only value worth searching for. Causes are
     * walked to a depth of three: the useful one is usually wrapped, and beyond three the output
     * stops being readable on a phone screen.
     */
    private fun diagnostic(throwable: Throwable): String {
        val lines = mutableListOf<String>()

        lines += "${throwable.javaClass.simpleName}: ${throwable.message ?: "no message"}"

        when (throwable) {
            is ApiException -> lines += "code=${throwable.code} status=${throwable.status}"
            is FirebaseAuthInvalidCredentialsException -> lines += "errorCode=${throwable.errorCode}"
            is FirebaseAuthInvalidUserException -> lines += "errorCode=${throwable.errorCode}"
        }

        var cause = throwable.cause
        var depth = 0
        while (cause != null && depth < 3) {
            lines += "caused by ${cause.javaClass.simpleName}: ${cause.message ?: "no message"}"
            cause = cause.cause
            depth++
        }

        /*
         * The fingerprint hint, demoted.
         *
         * A Google sheet that dismisses itself immediately after an account is chosen is almost
         * always a missing SHA-1 rather than a user action. That is worth saying - to whoever
         * turned developer mode on.
         */
        if (throwable is GetCredentialCancellationException) {
            lines += "If the sheet closed by itself, the debug SHA-1 is probably missing from the " +
                "Firebase project. Add it under Project settings > Your apps, re-download " +
                "google-services.json, reinstall. Command is in NOTIFICATIONS.md."
        }

        return lines.joinToString("\n")
    }
}
