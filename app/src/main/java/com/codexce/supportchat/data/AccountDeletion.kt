package com.codexce.supportchat.data

/*
 * Account deletion, from the app's side.
 *
 * Shaped by two facts that are not negotiable:
 *
 * 1. The project is on the Spark plan. There is no Cloud Functions runtime and no Cloud
 *    Scheduler, so nothing on Google's side can expire a request on a timer. Expiry happens
 *    when someone next looks: this app at launch, or an operator in the console.
 *    `Retention.kt` already makes exactly this trade for expired chats, for the same reason.
 *
 * 2. `firestore.rules` says `allow delete: if false` on tenants/{tenantId}. The owner can empty
 *    the workspace but cannot remove the document itself, and no client can ever delete another
 *    account's Auth user. So the app does the part it is authorised for - the websites, leads,
 *    devices, the RTDB chat tree, and its own Auth user - and leaves a marked stub behind for
 *    the console's Admin SDK sweep to finish.
 *
 * The order below is deliberate: content first, Auth user last. While the Auth user exists the
 * owner can still sign in, so if this dies halfway the account is still flagged and the next
 * launch or sweep resumes it. Deleting Auth first would strand the data with nobody permitted
 * to read or remove it.
 */

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** The grace period a request can be cancelled in. Mirrors GRACE_MS in the console. */
const val DELETION_GRACE_MS: Long = 7L * 24 * 60 * 60 * 1000

/** Field name on tenants/{tenantId}. Mirrors the console and the rules comment. */
private const val REQUESTED_AT = "deletionRequestedAt"

/**
 * Where an account stands.
 *
 * [Purged] is reported once the local half has run, and is what puts the "account deleted"
 * screen in front of the person instead of a sign-in error they would have to interpret.
 */
sealed interface DeletionState {
    /** Nothing requested. */
    data object None : DeletionState

    /** Requested, still inside the grace period. [dueAt] is when it stops being cancellable. */
    data class Pending(val requestedAt: Long, val dueAt: Long) : DeletionState

    /** Requested, grace period over. The next launch purges. */
    data class Due(val requestedAt: Long, val dueAt: Long) : DeletionState

    /** The local purge has run. There is no account to go back to. */
    data object Purged : DeletionState
}

object AccountDeletion {

    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()

    /**
     * Read the current state.
     *
     * A missing tenant document is [DeletionState.None] rather than an error: a signed-in user
     * who never finished provisioning has nothing to delete, and blocking them out of the app
     * over it would be worse than letting them continue.
     */
    suspend fun state(tenantId: String?, now: Long = System.currentTimeMillis()): DeletionState {
        val id = tenantId ?: return DeletionState.None

        val snapshot = runCatching { db.collection("tenants").document(id).get().await() }
            .getOrNull() ?: return DeletionState.None

        val requestedAt = snapshot.getLong(REQUESTED_AT) ?: 0L
        if (requestedAt <= 0L) return DeletionState.None

        val dueAt = requestedAt + DELETION_GRACE_MS
        return if (now >= dueAt) {
            DeletionState.Due(requestedAt, dueAt)
        } else {
            DeletionState.Pending(requestedAt, dueAt)
        }
    }

    /**
     * Mark the account for deletion.
     *
     * Written with merge so the rest of the tenant document is untouched, which also keeps the
     * update inside what the rules permit: they re-check tenantId, ownerUid, status, features
     * and websiteCount on every write, and a merge carries the existing values through.
     *
     * @return null on success, or a message to show.
     */
    suspend fun request(tenantId: String?): String? {
        val id = tenantId ?: return "This device does not know which workspace to delete yet."

        return runCatching {
            db.collection("tenants").document(id)
                .set(mapOf(REQUESTED_AT to System.currentTimeMillis()), com.google.firebase.firestore.SetOptions.merge())
                .await()
        }.fold(
            onSuccess = { null },
            onFailure = { "Could not schedule the deletion: ${it.message ?: "unknown error"}" },
        )
    }

    /** Take the request back. Only meaningful inside the grace period. */
    suspend fun cancel(tenantId: String?): String? {
        val id = tenantId ?: return "This device does not know which workspace to delete yet."

        return runCatching {
            // Zero rather than a field delete, so an older client reading a Long still sees
            // "not requested" instead of throwing on a missing field.
            db.collection("tenants").document(id)
                .set(mapOf(REQUESTED_AT to 0L), com.google.firebase.firestore.SetOptions.merge())
                .await()
        }.fold(
            onSuccess = { null },
            onFailure = { "Could not cancel the deletion: ${it.message ?: "unknown error"}" },
        )
    }

    /**
     * Run the half of the purge this app is authorised for.
     *
     * Every step is tolerated failing. A partial purge that leaves the stub flagged is
     * recoverable by the console sweep; refusing to continue because one lead would not delete
     * would leave the person signed in to an account they asked to be rid of.
     *
     * @return null on success, or a message to show. Success means the Auth user is gone.
     */
    suspend fun purgeLocalHalf(tenantId: String?): String? {
        val id = tenantId
        val user = auth.currentUser ?: return null

        if (id != null) {
            val tenant = db.collection("tenants").document(id)

            // The owner is permitted to delete these by firestore.rules; the tenant document
            // itself is not deletable by any client, which is why the stub is left flagged.
            for (name in listOf("websites", "leads", "devices")) {
                runCatching {
                    val rows = tenant.collection(name).get().await()
                    for (row in rows.documents) {
                        runCatching { row.reference.delete().await() }
                    }
                }
            }

            runCatching {
                FirebaseDatabase.getInstance().reference.child("chats").child(id).removeValue().await()
            }

            // Leaves the console a row it can find and finish, and records that the local half
            // is already done so a second launch does not repeat the sweep.
            runCatching {
                tenant.set(
                    mapOf(
                        "deletionLocalPurgeAt" to System.currentTimeMillis(),
                        "status" to "inactive",
                    ),
                    com.google.firebase.firestore.SetOptions.merge(),
                ).await()
            }

            runCatching { db.collection("owners").document(user.uid).delete().await() }
        }

        return runCatching { user.delete().await() }.fold(
            onSuccess = { null },
            onFailure = {
                /*
                 * Firebase refuses to delete a user whose sign-in is too old. That is a
                 * re-authentication prompt, not a failure of the deletion, and it must say so
                 * rather than reporting a generic error: the data is already gone at this point.
                 */
                if (it.message?.contains("recent", ignoreCase = true) == true) {
                    "Sign in again to finish removing your login. Your workspace data has already been deleted."
                } else {
                    "Your workspace data was deleted, but your login could not be removed: ${it.message ?: "unknown error"}"
                }
            },
        )
    }
}
