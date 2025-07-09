package com.hypergonial.chat.domain.crypto

import com.ionspin.kotlin.crypto.box.Box
import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.algorithms.SHA256
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.io.encoding.ExperimentalEncodingApi

object CryptoManager {

    @OptIn(DelicateCryptographyApi::class)
    fun testCryptoKotlin() {
        CoroutineScope(Dispatchers.Default).launch {
            val provider = CryptographyProvider.Default
            val rsaOaepProvider = provider.get(RSA.OAEP)

            val keyGenerator = rsaOaepProvider.keyPairGenerator(keySize = 2048.bits, digest = SHA256)
            val keyDecoder = rsaOaepProvider.publicKeyDecoder(SHA256)
            val key = keyGenerator.generateKey()
            val pubKey = key.publicKey
            val privateKey = key.privateKey
            println("private key: ${privateKey.encodeToByteString(RSA.PrivateKey.Format.DER.PKCS1).toByteArray().encodeBase64()}")
            val encryptTime1 = System.currentTimeMillis()
            val ciphertext = pubKey.encryptor().encrypt(plaintext = "jknasdfhnsadfo;jhfTFUIOKNQWEfh 9pif wygn89opfweak 7iotygnasdlm 8ohaeswuli fhm luas,./eilhnerf mkl.uasjhn ;dfkl.jhsZ.k fjeghbwalihgf yuiopghlweasrfg".encodeToByteArray()).encodeBase64()
            val encryptTime2 = System.currentTimeMillis() - encryptTime1
            println("Time in ms: $encryptTime2 ms")
            println("encoded str: $ciphertext")

            val decodeTime1 = System.currentTimeMillis()
            println("decoded str: ${privateKey.decryptor().decrypt(ciphertext = ciphertext.decodeBase64Bytes()).decodeToString()}")
            val decodeTime2 = System.currentTimeMillis() - decodeTime1
            println("Time in ms: $encryptTime2 ms")

            testPrivateDecode(rsaOaepProvider)
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class, ExperimentalEncodingApi::class)
    fun testPrivateDecode(provider: RSA.OAEP) {
        CoroutineScope(Dispatchers.Default).launch {
            val privKey = "MIIEowIBAAKCAQEA2tCCVlCHUNn9CM/a9s50fLBl8D4rbjWcZ7l0VeTIIeHRSCfozzRCEePg/wS+qPOpF+ASjoYSPlLVNUoS3NOYXWSVZUAeyJyis5cP9TgOQsJ4cooc9gXOEI4Q40rD8ElXcfEo7iJfyDgVFzmbYNyOqp+ZpJjlgfu7fwHoLO74HI1mOi+/rm53e8i917C9okn/KLgPg1UylmZB0sdtQ/eDLhtfPtfo8r27Lfw5z1htn8yUx9ssSv5GTFzEZGjh/Q34YX6rqKYRn4BVyJE6mohRBgWptkvqQDPck99A+X+kifjdywBD9/+JHJcOF0SvytGqd/JcGTqt+VGaL+8IX5Hg8wIDAQABAoIBAAXytBBPmRYQDupN6ChcXVtEWkkJD5or1+gT6P8qvGv7XPqGpzP2ujMNTEDyapJiKT8OZsK8D0PlcYlEzbV2BQbI5Ky/bPbMFzoliZgDYZPcAHUwnu/rIXpeukPLdP8Tp05gHVo30f251IzwknI0HKaztvP7+gUqCCNANIUAK/tuqatgF4KD3omLlRghQK0p/r22UHQqxbWpiPIyyUEXnX28GGKuSLog1GgQWSgm+vEs7ijU9ZmjzGdLxozI2fJh2TUE5kxKoba1zq6sNZgGIQlEjQK6L+hEZ7x8sGoD6bZUav9Bn5xYhVyYBau2jq41uZFsqeEflHQDYXMY2jQFWvECgYEA3fhXgBU/t6GnUPScDryUPs1RiV849bSOcSHLOhIUuEDqcBXm3fc3sFmANR3aW9z2rznzf32VNUyc9AcfJZ25P0G0EEAeqiHYH8BUe4j0dg5bytS/e47YwQmgDg7pZFCHPQqi+l1HLTfHnnCHzosTdGWvheQ5ysNwN/4fYCqlANECgYEA/FxR1ZraPy6JXyIdZ6JvIwxuC1pTAUGnu3ZSkej6NCLHTE8kJCR4PgCQP4m9bVjNzSjeDpNMdsvZIIo1TBwb8vtrk4ybhXmPAd9McXAHF+kjLz2hm9Ilb3nN1aBRvtw8e/B8fUTE5x8rOMvZ7CG5NrJPAewU/pJf6n8T8V4oloMCgYEAhE67GUWRLUvWB5NGUiJl2ulXaKKxuQNexGB4WqzcXNeU5iqn6japoxw2J3Mb7RXQlLHeSmf4wERiabK1Bh71hJcNVYJixNZXlNV3hgskbBoy7LvTRzrmSGoMVVTeGUVG10O30bQ38OQGwJIi4SJU/lR+Qzi+mfXUtgtreE7y+4ECgYAyKpgKAnKIRNQCGWIHC/9T1FC76QS/JzIUzfy0DMBQROmhfcoNdUKB8NK8DsGdTx36PrmF5Do7E2LiWRcnPh9AOkK0Xis8aWHIWX90vXRriGj6JyJNO4U9l9UXNnuJmXZcnP3iWR1bZJLPA5cancmX2fJiy2+Q+8AdeY2ZWcJvyQKBgEvJGBU0IwogFpvou0f0t+xnYk3rYSOwyHlODVYES4++SwPff+sz97lulgiGqKweFcXryzIPWzaa1J4CTcDJuNdpy/oLHb11CMCNcS5GBQ9IHeTpFWG1wmglL2S5YtkL27NY5+DCKvQS5t3KPwvjmeKWF0CHeuHmQ4puVvg/NgCQ"
            val privKeyBytes: ByteArray = privKey.decodeBase64Bytes()

            val importPrivKey = provider.privateKeyDecoder(SHA256)
            val privateKeyObj = importPrivKey.decodeFromByteArray(RSA.PrivateKey.Format.DER.PKCS1, privKeyBytes)
            val ciphertext = "q6lrhU1ua6ghUMDPUo4lYEZaj1eAtk8fKuMNv1gB34Bm4SDFRZGLA9JkXnJ/1Z/6e1xltYfxPyTYxeydLYXB+VjIW8ZgZCWanxIZj7JBPdwwwVTFqSlU8EpjtwqZv00eguvdL2Xh6YRETBeFSpvBNC91W6ZviXmhxvzAAFAyzQy5dIYlzu4ymZvHgF6Bzbmitsezoi3/Wf8OJmDvy6Mm3cminlyrtcs7UrFhfkYRi6JnKhCQ6XfA04xiV4AcDzaoK+45VXlC+R8VycHO9RoJKc5eVtW6g3LbkH+ZyAzWrSKI6f6CcGwYJpU9bCzOREhLqaWQbNk7W8L+xzy7wVQIjw=="

            println("decoded str SANYA: ${privateKeyObj.decryptor().decrypt(ciphertext = ciphertext.decodeBase64Bytes()).decodeToString()}")
        }
    }
}
