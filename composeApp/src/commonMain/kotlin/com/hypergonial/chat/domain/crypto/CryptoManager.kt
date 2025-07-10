package com.hypergonial.chat.domain.crypto

import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.algorithms.SHA256
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CryptoManager {
    private const val KEY_SIZE = 2048

    suspend fun generateKeyPair(): RSA.OAEP.KeyPair = withContext(Dispatchers.Default) {
        CryptographyProvider.Default
            .get(RSA.OAEP)
            .keyPairGenerator(keySize = KEY_SIZE.bits, digest = SHA256)
            .generateKey()
    }

    suspend fun encrypt(message: String, publicKey: String): String = withContext(Dispatchers.Default) {
        getPublicKeyFromByteString(publicKey).encryptor().encrypt(message.encodeToByteArray()).encodeBase64()
    }

    suspend fun decrypt(message: String, privateKey: String): String = withContext(Dispatchers.Default) {
        getPrivateKeyFromByteString(privateKey).decryptor().decrypt(message.decodeBase64Bytes()).decodeToString()
    }

    private suspend fun getPublicKeyFromByteString(key: String): RSA.OAEP.PublicKey = withContext(Dispatchers.Default) {
        CryptographyProvider.Default
            .get(RSA.OAEP)
            .publicKeyDecoder(SHA256)
            .decodeFromByteArray(RSA.PublicKey.Format.DER.PKCS1, key.decodeBase64Bytes())
    }

    private suspend fun getPrivateKeyFromByteString(key: String): RSA.OAEP.PrivateKey = withContext(Dispatchers.Default) {
        CryptographyProvider.Default
            .get(RSA.OAEP)
            .privateKeyDecoder(SHA256)
            .decodeFromByteArray(RSA.PrivateKey.Format.DER.PKCS1, key.decodeBase64Bytes())
    }
}
