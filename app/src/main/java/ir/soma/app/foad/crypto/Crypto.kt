package ir.soma.app.foad.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.util.Base64

/**
 * ماژول رمزنگاری:
 * - ساخت جفت‌کلید ECDSA در Android Keystore (امن و غیرقابل خروج خصوصی)
 * - امضای پیام‌ها
 * - تولید شناسه تراکنش (tx_id) از فیلدهای اصلی
 */
object Crypto {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "SOMA_ECDSA"

    /** اطمینان از وجود جفت‌کلید؛ در صورت نبود، ایجاد می‌کند. */
    fun ensureKeypair(): KeyPair {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!ks.containsAlias(ALIAS)) {
            val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            ).setDigests(KeyProperties.DIGEST_SHA256)
             .build()
            kpg.initialize(spec)
            kpg.generateKeyPair()
        }
        val priv = ks.getKey(ALIAS, null) as PrivateKey
        val pub = ks.getCertificate(ALIAS).publicKey
        return KeyPair(pub, priv)
    }

    /** امضای داده با ECDSA(SHA-256) و خروجی Base64 */
    fun sign(data: ByteArray): String {
        val (_, priv) = ensureKeypair()
        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initSign(priv)
        sig.update(data)
        return Base64.getEncoder().encodeToString(sig.sign())
    }

    /** شناسه تراکنش کوتاه برای نمایش در UI (۸ بایت اول SHA-256 به hex) */
    fun txId(buyerId: String, sellerId: String, amount: Long, ts: Long, counter: Long): String {
        val md = MessageDigest.getInstance("SHA-256")
        val input = "$buyerId|$sellerId|$amount|$ts|$counter".toByteArray()
        val d = md.digest(input)
        return d.take(8).joinToString("") { "%02x".format(it) }
    }

    /** کلید عمومی را برای ذخیره/نمایش (در آینده) برمی‌گرداند – Base64 DER */
    fun publicKeyBase64(): String {
        val (pub, _) = ensureKeypair()
        return Base64.getEncoder().encodeToString(pub.encoded)
    }
}
