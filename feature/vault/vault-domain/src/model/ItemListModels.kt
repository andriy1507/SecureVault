package dev.spaceapps.secvault.feature.vault.domain.model

/** List filters (chips on the vault screen). Codes: items with a 2FA secret, shown with live codes. */
enum class ItemFilter { All, Logins, Cards, Notes, Codes }

enum class ItemSort { Name, DateAdded, LastModified }

/** A one-time code and how long it stays valid. */
data class TotpCode(
    val code: String,
    val periodSeconds: Int,
    val secondsRemaining: Int,
) {
    override fun toString() = "TotpCode(period=$periodSeconds)"
}
