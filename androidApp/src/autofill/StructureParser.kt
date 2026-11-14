package dev.spaceapps.secvault.autofill

import android.app.assist.AssistStructure
import android.text.InputType
import android.view.View
import android.view.autofill.AutofillId

/** The login-relevant parts of a screen another app showed to the autofill framework. */
class ParsedForm(
    val packageName: String,
    val webDomain: String?,
    val usernameIds: List<AutofillId>,
    val passwordIds: List<AutofillId>,
    /** Sign-up / change-password form: offer a generated password. */
    val isNewPassword: Boolean,
    val usernameValue: String?,
    val passwordValue: String?,
) {
    val isEmpty: Boolean get() = usernameIds.isEmpty() && passwordIds.isEmpty()
    val allIds: List<AutofillId> get() = usernameIds + passwordIds
}

/**
 * Finds username and password fields, by (in order of trust) autofill hints, HTML attributes,
 * input type, then id / hint text. If a password field has no username field, the text field
 * right before it is taken as the username.
 */
object StructureParser {
    fun parse(structure: AssistStructure): ParsedForm {
        val state = State()
        for (i in 0 until structure.windowNodeCount) visit(structure.getWindowNodeAt(i).rootViewNode, state)

        if (state.usernames.isEmpty() && state.passwords.isNotEmpty()) {
            val firstPassword = state.passwords.first().id
            val index = state.textFields.indexOfFirst { it.id == firstPassword }
            state.textFields
                .getOrNull(index - 1)
                ?.takeIf { it.id !in state.passwords.map(Field::id) }
                ?.let { state.usernames += it }
        }
        return ParsedForm(
            packageName = structure.activityComponent.packageName,
            webDomain = state.webDomain,
            usernameIds = state.usernames.map { it.id }.distinct(),
            passwordIds = state.passwords.map { it.id }.distinct(),
            isNewPassword = state.newPassword || state.passwords.size >= 2,
            usernameValue = state.usernames.firstNotNullOfOrNull { it.value?.takeIf(String::isNotBlank) },
            passwordValue = state.passwords.firstNotNullOfOrNull { it.value?.takeIf(String::isNotEmpty) },
        )
    }

    private class Field(
        val id: AutofillId,
        val value: String?,
    )

    private class State {
        var webDomain: String? = null
        var newPassword = false
        val usernames = mutableListOf<Field>()
        val passwords = mutableListOf<Field>()
        val textFields = mutableListOf<Field>()
    }

    private fun visit(
        node: AssistStructure.ViewNode,
        state: State,
    ) {
        if (state.webDomain == null) node.webDomain?.takeIf { it.isNotBlank() }?.let { state.webDomain = it }

        val id = node.autofillId
        if (id != null && node.autofillType == View.AUTOFILL_TYPE_TEXT && node.visibility == View.VISIBLE) {
            val field = Field(
                id,
                node.autofillValue
                    ?.takeIf { it.isText }
                    ?.textValue
                    ?.toString(),
            )
            val hints = node.autofillHints?.map { it.lowercase() }.orEmpty()
            val html = node.htmlInfo
                ?.attributes
                ?.associate { it.first.lowercase() to it.second.lowercase() }
                .orEmpty()
            val autocomplete = html["autocomplete"].orEmpty()
            val text = listOfNotNull(node.idEntry, node.hint, html["name"], html["id"]).joinToString(" ").lowercase()

            if ("newpassword" in hints || "new-password" in hints || autocomplete.contains("new-password")) {
                state.newPassword = true
            }
            when {
                isPassword(node, hints, html) -> state.passwords += field
                isUsername(node, hints, autocomplete, text) -> state.usernames += field
            }
            if (html["type"] != "hidden") state.textFields += field
        }
        for (i in 0 until node.childCount) visit(node.getChildAt(i), state)
    }

    private fun isPassword(
        node: AssistStructure.ViewNode,
        hints: List<String>,
        html: Map<String, String>,
    ): Boolean {
        if (hints.any { it.contains("password") }) return true
        if (html["type"] == "password") return true
        val cls = node.inputType and InputType.TYPE_MASK_CLASS
        val variation = node.inputType and InputType.TYPE_MASK_VARIATION
        return (cls == InputType.TYPE_CLASS_TEXT && variation in TEXT_PASSWORD_VARIATIONS) ||
            (cls == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
    }

    private fun isUsername(
        node: AssistStructure.ViewNode,
        hints: List<String>,
        autocomplete: String,
        text: String,
    ): Boolean {
        if (hints.any { it in USERNAME_HINTS }) return true
        if (autocomplete.contains("username") || autocomplete.contains("email")) return true
        val variation = node.inputType and InputType.TYPE_MASK_VARIATION
        if ((node.inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT &&
            (
                variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                    variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
            )
        ) {
            return true
        }
        return USERNAME_WORDS.any { text.contains(it) }
    }

    private val TEXT_PASSWORD_VARIATIONS = setOf(
        InputType.TYPE_TEXT_VARIATION_PASSWORD,
        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
        InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
    )
    private val USERNAME_HINTS = setOf("username", "emailaddress", "email", "newusername")
    private val USERNAME_WORDS = listOf("user", "login", "email", "e-mail", "account")
}
