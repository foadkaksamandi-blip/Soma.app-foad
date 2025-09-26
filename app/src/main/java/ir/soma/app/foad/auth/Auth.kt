package ir.soma.app.foad.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * ماژول تأیید هویت:
 * - بررسی پشتیبانی از اثرانگشت/بیومتریک
 * - نمایش دیالوگ تأیید
 */
object Auth {

    fun canUseBiometric(ctx: Context): Boolean {
        val bm = BiometricManager.from(ctx)
        return bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun prompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFail: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFail()
                }

                override fun onAuthenticationFailed() {
                    onFail()
                }
            })

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("تأیید پرداخت")
            .setSubtitle("اثر انگشت یا رمز دستگاه خود را وارد کنید")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(info)
    }
}
