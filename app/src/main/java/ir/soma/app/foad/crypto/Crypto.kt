package ir.soma.app.foad.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature

object Crypto {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "SOMA_ECDSA"

    fun ensureKeypair(): KeyPair {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!ks.containsAlias(ALIAS)) {
            val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            ).setDigests(KeyProperties.DIGEST_SHA256).build()
            kpg.initialize(spec)
            kpg.generateKeyPair()
        }
        val priv = ks.getKey(ALIAS, null) as PrivateKey
        val pub = ks.getCertificate(ALIAS).publicKey
        return KeyPair(pub, priv)
    }

    fun sign(data: ByteArray): String {
        val (_, priv) = ensureKeypair()
        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initSign(priv)
        sig.update(data)
        val bytes = sig.sign()
        // استفاده از android.util.Base64 برای سازگاری با minSdk 24
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun txId(buyerId: String, sellerId: String, amount: Long, ts: Long, counter: Long): String {
        val md = MessageDigest.getInstance("SHA-256")
        val input = "$buyerId|$sellerId|$amount|$ts|$counter".toByteArray()
        val d = md.digest(input)
        return d.take(8).joinToString("") { "%02x".format(it) }
    }

    fun publicKeyBase64(): String {
        val (pub, _) = ensureKeypair()
        return Base64.encodeToString(pub.encoded, Base64.NO_WRAP)
    }
}
