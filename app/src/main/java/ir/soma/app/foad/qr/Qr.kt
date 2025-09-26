package ir.soma.app.foad.qr

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

/**
 * ابزار تولید QR از رشتهٔ ورودی (مثلاً tx_id یا رسید).
 * خروجی: Bitmap برای نمایش در ImageView
 */
object Qr {
    /**
     * متن ورودی را به QR تبدیل می‌کند.
     * @param text محتوای QR (مثلاً "TX:c7a9f3e1")
     * @param size اندازه تصویر خروجی بر حسب پیکسل (پیشنهاد: 512)
     */
    fun make(text: String, size: Int = 512): Bitmap {
        val encoder = BarcodeEncoder()
        return encoder.encodeBitmap(text, BarcodeFormat.QR_CODE, size, size)
    }
}
