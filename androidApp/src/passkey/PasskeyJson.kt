package dev.spaceapps.secvault.passkey

import android.content.Context
import androidx.annotation.RequiresApi
import androidx.credentials.provider.CallingAppInfo
import dev.spaceapps.secvault.R
import dev.spaceapps.secvault.feature.vault.domain.passkey.Base64Url
import dev.spaceapps.secvault.feature.vault.domain.passkey.CreatedPasskey
import dev.spaceapps.secvault.feature.vault.domain.passkey.PasskeyAssertion
import dev.spaceapps.secvault.feature.vault.domain.passkey.PasskeyCreationRequest
import dev.spaceapps.secvault.feature.vault.domain.passkey.WebAuthn
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/** The parts of a WebAuthn creation request (PublicKeyCredentialCreationOptionsJSON) we use. */
internal class CreationOptions(val request: PasskeyCreationRequest, val challenge: String)

/** The parts of a WebAuthn sign-in request (PublicKeyCredentialRequestOptionsJSON) we use. */
internal class SignInOptions(val rpId: String, val challenge: String, val allowCredentialIds: List<ByteArray>)

/** Who's asking: a website through a trusted browser, or an Android app (identified by its signing key). */
internal class Caller(val origin: String, val packageName: String, val isBrowser: Boolean)

/** The site and the request don't match, or the request can't be served. */
internal class PasskeyRequestException(message: String) : Exception(message)

/**
 * WebAuthn JSON in and out for Android's Credential Manager.
 * iOS hands over parsed values instead, so this stays Android-only.
 */
internal object PasskeyJson {
    private const val TYPE_PUBLIC_KEY = "public-key"
    private const val ATTACHMENT_PLATFORM = "platform"
    private const val APK_KEY_HASH_PREFIX = "android:apk-key-hash:"
    private const val TYPE_CREATE = "webauthn.create"
    private const val TYPE_GET = "webauthn.get"

    fun parseCreation(json: String): CreationOptions {
        val root = JSONObject(json)
        val rp = root.getJSONObject("rp")
        val user = root.getJSONObject("user")
        val algorithms = root.optJSONArray("pubKeyCredParams")?.objects()?.map { it.optLong("alg") }.orEmpty()
        if (algorithms.isNotEmpty() && WebAuthn.COSE_ALG_ES256 !in algorithms) {
            throw PasskeyRequestException("The site doesn't accept ES256 keys")
        }
        val rpId = rp.optString("id").ifBlank { throw PasskeyRequestException("Missing rp.id") }
        return CreationOptions(
            request = PasskeyCreationRequest(
                rpId = rpId,
                rpName = rp.optString("name"),
                userHandle = Base64Url.decode(user.getString("id")),
                userName = user.optString("name"),
                userDisplayName = user.optString("displayName"),
                excludeCredentialIds = root.optJSONArray("excludeCredentials").credentialIds(),
            ),
            challenge = root.getString("challenge"),
        )
    }

    fun parseSignIn(json: String): SignInOptions {
        val root = JSONObject(json)
        return SignInOptions(
            rpId = root.optString("rpId").ifBlank { throw PasskeyRequestException("Missing rpId") },
            challenge = root.getString("challenge"),
            allowCredentialIds = root.optJSONArray("allowCredentials").credentialIds(),
        )
    }

    /**
     * A browser on Google's privileged list passes the website's origin; anything else is an app,
     * whose origin is the hash of its signing certificate (the site's server checks it's allowed).
     */
    @RequiresApi(34)
    fun caller(context: Context, app: CallingAppInfo): Caller {
        if (app.isOriginPopulated()) {
            val allowlist = context.resources.openRawResource(R.raw.privileged_browsers).bufferedReader().use { it.readText() }
            val origin = runCatching { app.getOrigin(allowlist) }.getOrNull()
                ?: throw PasskeyRequestException("${app.packageName} isn't a trusted browser")
            return Caller(origin.trimEnd('/'), app.packageName, isBrowser = true)
        }
        val certificate = app.signingInfo.apkContentsSigners.first().toByteArray()
        val hash = MessageDigest.getInstance("SHA-256").digest(certificate)
        return Caller(APK_KEY_HASH_PREFIX + Base64Url.encode(hash), app.packageName, isBrowser = false)
    }

    /** A website may only use passkeys for its own domain or a parent domain (WebAuthn rpId rule). */
    fun checkOrigin(caller: Caller, rpId: String) {
        if (!caller.isBrowser) return
        val host = caller.origin.substringAfter("://").substringBefore(':').substringBefore('/').lowercase()
        val rp = rpId.lowercase()
        if (!caller.origin.startsWith("https://") || (host != rp && !host.endsWith(".$rp"))) {
            throw PasskeyRequestException("$host can't use passkeys for $rpId")
        }
    }

    /** clientDataJSON the site checks; the browser builds its own, so this is used for apps. */
    fun clientDataJson(type: String, challenge: String, caller: Caller): ByteArray =
        JSONObject().apply {
            put("type", type)
            put("challenge", challenge)
            put("origin", caller.origin)
            if (!caller.isBrowser) put("androidPackageName", caller.packageName)
        }.toString().replace("\\/", "/").encodeToByteArray()

    fun creationClientData(challenge: String, caller: Caller) = clientDataJson(TYPE_CREATE, challenge, caller)

    fun signInClientData(challenge: String, caller: Caller) = clientDataJson(TYPE_GET, challenge, caller)

    /** RegistrationResponseJSON. */
    fun registrationResponse(created: CreatedPasskey, clientDataJson: ByteArray): String {
        val id = Base64Url.encode(created.credentialId)
        return JSONObject().apply {
            put("id", id)
            put("rawId", id)
            put("type", TYPE_PUBLIC_KEY)
            put("authenticatorAttachment", ATTACHMENT_PLATFORM)
            put(
                "response",
                JSONObject().apply {
                    put("clientDataJSON", Base64Url.encode(clientDataJson))
                    put("attestationObject", Base64Url.encode(created.attestationObject))
                    put("authenticatorData", Base64Url.encode(created.authenticatorData))
                    put("publicKey", Base64Url.encode(created.publicKeySpki))
                    put("publicKeyAlgorithm", WebAuthn.COSE_ALG_ES256)
                    put("transports", JSONArray(listOf("internal", "hybrid")))
                },
            )
            put("clientExtensionResults", JSONObject().put("credProps", JSONObject().put("rk", true)))
        }.toString()
    }

    /** AuthenticationResponseJSON. */
    fun assertionResponse(assertion: PasskeyAssertion, clientDataJson: ByteArray): String {
        val id = Base64Url.encode(assertion.credentialId)
        return JSONObject().apply {
            put("id", id)
            put("rawId", id)
            put("type", TYPE_PUBLIC_KEY)
            put("authenticatorAttachment", ATTACHMENT_PLATFORM)
            put(
                "response",
                JSONObject().apply {
                    put("clientDataJSON", Base64Url.encode(clientDataJson))
                    put("authenticatorData", Base64Url.encode(assertion.authenticatorData))
                    put("signature", Base64Url.encode(assertion.signature))
                    put("userHandle", Base64Url.encode(assertion.userHandle))
                },
            )
            put("clientExtensionResults", JSONObject())
        }.toString()
    }

    fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)

    private fun JSONArray?.credentialIds(): List<ByteArray> =
        this?.objects()?.mapNotNull { it.optString("id").takeIf(String::isNotBlank)?.let(Base64Url::decode) }.orEmpty()

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
}
