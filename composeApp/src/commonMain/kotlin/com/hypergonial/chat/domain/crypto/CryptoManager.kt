package com.hypergonial.chat.domain.crypto

import com.ionspin.kotlin.crypto.box.Box
import com.ionspin.kotlin.crypto.util.decodeFromUByteArray
import com.ionspin.kotlin.crypto.util.encodeToUByteArray
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.algorithms.SHA256
import io.ktor.util.*
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object CryptoManager {

    @OptIn(DelicateCryptographyApi::class)
    fun testCryptoKotlin() {
        CoroutineScope(Dispatchers.Default).launch {
            val provider = CryptographyProvider.Default
            val rsaPkcs1 = provider.get(RSA.PKCS1)

            val keyGenerator = rsaPkcs1.keyPairGenerator(digest = SHA256)
            val key = keyGenerator.generateKey()
            val pubKey = key.publicKey
            val privateKey = key.privateKey
            println("private key: ${privateKey.encodeToByteString(RSA.PrivateKey.Format.PEM.PKCS1).toByteArray().encodeBase64()}")
            val ciphertext = pubKey.encryptor().encrypt(plaintext = "RSA_PKCS1".encodeToByteArray()).encodeBase64()
            println("encoded str - $ciphertext")

            println("decoded str - ${privateKey.decryptor().decrypt(ciphertext = ciphertext.decodeBase64Bytes()).decodeToString()}")
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class, ExperimentalEncodingApi::class)
    fun testLibsodium() {
        CoroutineScope(Dispatchers.Default).launch {
            val (publicKey, secretKey) = Box.keypair()
            val message = "LIBSODIUM_KAL".encodeToUByteArray()
            val ciphertext = Box.seal(message, publicKey).asByteArray().encodeBase64()
            val decrypted = Box.sealOpen(ciphertext.decodeBase64Bytes().asUByteArray(), publicKey, secretKey)
            println(publicKey)
            println(secretKey)
            println(ciphertext)
            println(decrypted.decodeFromUByteArray())

            val privKey = "MIIEpQIBAAKCAQEAuNSQEFAeywy7Id5wnheEXB8uF/TWb5pvpQos+LMAZW2/thKgjHFv/8xyKKscfiD3Cvwyq98NMmH9z0mSZrwJk8nN/0HsvOS3Ye04Ae7D3Ihl41rlg8UGTjx2hvAl/RHkJG8LmLRHVqZim8Eh2aUQsOfcqwprq7ajdaWxXLgcOPnEdgO3UasOjpdochMrScu92w0D3GRCiOfQN3WhAJ6eZur9/sKWnZJj2nPnzH/j95tk2GH+GS9bJQ/RqlMjDQlc+wdNAYz4pyTcBL3+KLr1pw5ElsQNnULjQbUU3+6ghKWUsTHCKYp4e8pIxGvSGS2tt15ranqQI05cqUFbw8zROwIDAQABAoIBAEWrIc50VNcWqmbe0LZWiPasbhRrnnSc0t5z0nXACzMwRiYcKVYm4B+ccZ9wuCG2zUffvj3YqsHF/ASpZ7y/viBt3e8Ma27cC1+nKghYdo1nAHjNZ1ve5TySP98nIDqfBs7Q6J8bvRQlTWAGUXsXZA46p4v1NpaYFk8fMJaRc1np7sCOsDXgJ9wNwNo0oQDOlXHA9Rz1tXVvjzer1V/MZ8g7mar2wvXPsEN17ctSptCGVMhgzoI+q40bSePCIn2ffl0p7JGo64OJBWApdzAbePBqc/rgPnvta/iYpxlvqEPLc4Ol5r+WOYiiawLxCkBayLGjhueQjve4vse0BcfaFmkCgYEAw5FmlSq5kWZTNp7PkfQEUgaAZwU3tJnHyDmAhlTcWeTINgHBHLwb3biGHTSvwCVAJG6UcqfeBiRvEtlSzqCfqxknoUk0OLGiHJMlSefsZGtII42WbkizP0JD+fXdhmfWi/0GWDfheIWi7mUZhEnpiuo3Q1B0BYUFPWGRQFPqu08CgYEA8fG/6heaJBWLU/jd51AgAKQeRyi+c86i9l9RgFDwVgPwtSm9JyK7x6WHLvz+bjd3xIkf3tWG8/GfsDmDVnJh1vm3XG0Fwp4c8QeCzaBhCgGh/8b3SOu/4chTBi/qrb2jB22XrFXcg33b9GVE9vEh5/SKdfLL3tDRs+yD6fGxYFUCgYEAlksel9I+zBPkLVy8zkDGNTdT5FgpKDzqQOCX+iQrN1ZP2tlcTDXwAGP3hWhhDHxUH+tvX/HL+dJ0Hfsv3SWprzbkstlsLYmtuOIITRTUIZQk21XLXrO2NQ466VTeypTwV6K7Bn7jYtjojubZRkX7GcvHbo7cqwVAMpzRoKsdAJcCgYEAxJ0awpePkcdYRFM3sWRxvVOhr14y91VzJRfs2YPs61mkYPScXJNjWijwJIVAFj1JPRPaLHIFi3RNux3h13x4egTQt3F5fuCS0GS0LXopocIV7g+4oS/D7S2oGp0R70Lum3i6CzhzTQAYoREy7CKk1STq6FL4zgeQgtCvmovpA0kCgYEAwM0r6IbQdxGPtCv0wp+kvZEj9cHYkQ6yk178NSzpw8TLPlWAhuaBaBMOUscXE+KOElbEdAVR31B+IvR8DaUsjEp4GGh2ouyFgSi194BMhPmu9piWyfBcDZNNeSdgwB969eiOx0CmleOyVeJoVqboNMmPTRPV3+vqIX80e03jtWE="
            val privKeyBytes: ByteArray = privKey.decodeBase64Bytes()
            val pubKey = "MIIBCgKCAQEAuNSQEFAeywy7Id5wnheEXB8uF/TWb5pvpQos+LMAZW2/thKgjHFv/8xyKKscfiD3Cvwyq98NMmH9z0mSZrwJk8nN/0HsvOS3Ye04Ae7D3Ihl41rlg8UGTjx2hvAl/RHkJG8LmLRHVqZim8Eh2aUQsOfcqwprq7ajdaWxXLgcOPnEdgO3UasOjpdochMrScu92w0D3GRCiOfQN3WhAJ6eZur9/sKWnZJj2nPnzH/j95tk2GH+GS9bJQ/RqlMjDQlc+wdNAYz4pyTcBL3+KLr1pw5ElsQNnULjQbUU3+6ghKWUsTHCKYp4e8pIxGvSGS2tt15ranqQI05cqUFbw8zROwIDAQAB"
            val pubKeyBytes: ByteArray = pubKey.decodeBase64Bytes()

            val importPrivKey: UByteArray = privKeyBytes.toUByteArray()
            val importPubKey: UByteArray = pubKeyBytes.toUByteArray()

            val decodeSan = Box.sealOpen(
                ciphertext = "NI9yzD86f4LfUyicRGHtxpIcTWfnNstGkwMklxae+IY2VQwpx4qlgrsxzVyOyYWXqfpQKgci0p/yeFxYFbD/lZVsjiDzEgbbh977eaiYipKRapkB/Ex+RJ+m7fAIy0X1hyd7re1A1sGr2IMZ92gZ8/MlWWkAPdJaEMRc4qfrYzYAPxKfB1B2Q9auVz5DUSSawqhs71dMnouwtBb8rkN7MvOWGovsdOQbjgWiNWY4Bc5WmRQafUZRdNWN5ufk2S+PZf0GTJo/+liVqPbwYS+7HywnslO1FZifT860SnMvwn7dfjqdZRE8IoTgEOQNfi3A0bQgCFaLE/q4yHTX1jQwuA==".decodeBase64Bytes().asUByteArray(),
                recipientsPublicKey = importPubKey,
                recipientsSecretKey = importPrivKey
            )

            println("DECODE SANYA - $decodeSan")
        }
    }
}
