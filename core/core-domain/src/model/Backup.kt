package dev.spaceapps.secvault.core.domain.model

/** What can be shown about a backup file before the recovery phrase is entered. */
data class BackupInfo(
    val accountId: String,
    val createdAt: Long,
    val itemCount: Int,
)

/** Backup file naming and phrase parsing shared by export, import and restore. */
object BackupFormat {
    const val EXTENSION = ".svbackup"
    const val PHRASE_WORDS = 24

    fun fileName(accountId: String) = "secvault-backup-$accountId$EXTENSION"

    /** "12. word" lines, as in the recovery key file saved during onboarding. */
    private val NUMBERED_WORD = Regex("""^\s*\d+[.)]\s*([A-Za-z]+)\s*$""", RegexOption.MULTILINE)
    private val NON_LETTERS = Regex("[^a-z]+")

    /**
     * Words of a typed or pasted phrase, or of the whole recovery key file. Case, numbering and
     * punctuation are ignored.
     */
    fun parsePhrase(text: String): List<String> {
        val numbered = NUMBERED_WORD.findAll(text).map { it.groupValues[1].lowercase() }.toList()
        if (numbered.isNotEmpty()) return numbered
        return text.lowercase().split(NON_LETTERS).filter { it.isNotEmpty() }
    }
}
