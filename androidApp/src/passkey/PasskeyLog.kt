package dev.spaceapps.secvault.passkey

import android.util.Log

/**
 * Logcat trail for the passkey flow (filter by tag "VaultPasskey").
 * Logs steps, sites and error types only: never keys, challenges, user names or vault content.
 */
internal object PasskeyLog {
    private const val TAG = "VaultPasskey"

    fun step(message: String) {
        Log.i(TAG, message)
    }

    fun failed(
        step: String,
        error: Throwable,
    ) {
        Log.w(TAG, "$step failed: ${error::class.simpleName}: ${error.message}")
    }
}
