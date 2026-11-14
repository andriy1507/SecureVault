import Foundation
import CryptoKit
import LocalAuthentication
import KotlinModules

/// Seals the key bundle to a P-256 key that lives in the Secure Enclave.
///
/// seal:   ephemeral P-256 key + ECDH with the enclave public key -> HKDF-SHA256 -> AES-GCM.
///         Uses only the public key, so no prompt is shown.
/// unseal: ECDH with the enclave private key, which requires device passcode or biometrics.
///
/// Sealed format: [65-byte ephemeral public key (x963) | AES-GCM combined (nonce | ciphertext | tag)]
final class SecureEnclaveKeyGuard: NativeKeyGuard {

    private let sharedInfo = Data("secvault-seal-v1".utf8)
    private let queue = DispatchQueue(label: "secvault.keyguard")

    private var keyFileURL: URL {
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir.appendingPathComponent("se_key.bin")
    }

    func isDeviceSecure() -> Bool {
        LAContext().canEvaluatePolicy(.deviceOwnerAuthentication, error: nil)
    }

    func seal(plainBase64: String, completion: @escaping (String?, String?) -> Void) {
        queue.async {
            do {
                guard self.isDeviceSecure() else { return completion(nil, "not_secure") }
                guard let plain = Data(base64Encoded: plainBase64) else { return completion(nil, "bad_input") }
                let publicKey = try self.loadOrCreatePublicKey()
                let ephemeral = P256.KeyAgreement.PrivateKey()
                let secret = try ephemeral.sharedSecretFromKeyAgreement(with: publicKey)
                let box = try AES.GCM.seal(plain, using: self.symmetricKey(secret))
                var out = ephemeral.publicKey.x963Representation
                out.append(box.combined!)
                completion(out.base64EncodedString(), nil)
            } catch {
                completion(nil, self.code(for: error))
            }
        }
    }

    func unseal(sealedBase64: String, reason: String, completion: @escaping (String?, String?) -> Void) {
        queue.async {
            do {
                guard let sealed = Data(base64Encoded: sealedBase64), sealed.count > 65 else {
                    return completion(nil, "bad_input")
                }
                let context = LAContext()
                context.localizedReason = reason
                let ephemeralPublic = try P256.KeyAgreement.PublicKey(x963Representation: sealed.prefix(65))
                let secret = try self.loadPrivateKey(context: context)
                    .sharedSecretFromKeyAgreement(with: ephemeralPublic) // shows passcode / Face ID prompt
                let box = try AES.GCM.SealedBox(combined: sealed.dropFirst(65))
                let plain = try AES.GCM.open(box, using: self.symmetricKey(secret))
                completion(plain.base64EncodedString(), nil)
            } catch {
                completion(nil, self.code(for: error))
            }
        }
    }

    func reset() {
        try? FileManager.default.removeItem(at: keyFileURL)
    }

    // MARK: - Keys

    private func symmetricKey(_ secret: SharedSecret) -> SymmetricKey {
        secret.hkdfDerivedSymmetricKey(using: SHA256.self, salt: Data(), sharedInfo: sharedInfo, outputByteCount: 32)
    }

    private func loadOrCreatePublicKey() throws -> P256.KeyAgreement.PublicKey {
        if FileManager.default.fileExists(atPath: keyFileURL.path) {
            return try loadPrivateKey(context: nil).publicKey
        }
        #if targetEnvironment(simulator)
        // No Secure Enclave on the simulator: software key, DEBUG convenience only.
        let key = P256.KeyAgreement.PrivateKey()
        try key.rawRepresentation.write(to: keyFileURL, options: .completeFileProtection)
        return key.publicKey
        #else
        var error: Unmanaged<CFError>?
        guard let access = SecAccessControlCreateWithFlags(
            nil,
            kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly, // key destroyed if passcode is removed
            [.privateKeyUsage, .userPresence],               // passcode or biometrics on every use
            &error
        ) else { throw error!.takeRetainedValue() as Error }
        let key = try SecureEnclave.P256.KeyAgreement.PrivateKey(accessControl: access)
        // dataRepresentation is an enclave-encrypted handle, useless outside this device's enclave.
        try key.dataRepresentation.write(to: keyFileURL, options: .completeFileProtection)
        return key.publicKey
        #endif
    }

    private func loadPrivateKey(context: LAContext?) throws -> KeyAgreeing {
        guard let data = try? Data(contentsOf: keyFileURL) else { throw GuardError.invalidated }
        #if targetEnvironment(simulator)
        return try P256.KeyAgreement.PrivateKey(rawRepresentation: data)
        #else
        do {
            return try SecureEnclave.P256.KeyAgreement.PrivateKey(dataRepresentation: data, authenticationContext: context)
        } catch {
            throw GuardError.invalidated
        }
        #endif
    }

    private func code(for error: Error) -> String {
        if let e = error as? GuardError, e == .invalidated { return "invalidated" }
        let ns = error as NSError
        if ns.domain == LAErrorDomain,
           [LAError.userCancel.rawValue, LAError.appCancel.rawValue, LAError.systemCancel.rawValue].contains(ns.code) {
            return "cancelled"
        }
        if ns.code == Int(errSecUserCanceled) { return "cancelled" }
        return "\(ns.domain):\(ns.code)"
    }

    private enum GuardError: Error { case invalidated }
}

/// Common shape of the enclave key and the simulator software key.
private protocol KeyAgreeing {
    var publicKey: P256.KeyAgreement.PublicKey { get }
    func sharedSecretFromKeyAgreement(with publicKeyShare: P256.KeyAgreement.PublicKey) throws -> SharedSecret
}

extension P256.KeyAgreement.PrivateKey: KeyAgreeing {}
extension SecureEnclave.P256.KeyAgreement.PrivateKey: KeyAgreeing {}
