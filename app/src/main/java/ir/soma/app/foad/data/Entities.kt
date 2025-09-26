package ir.soma.app.foad.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallet")
data class Wallet(
    @PrimaryKey val ownerId: String,
    val balance: Long,
    val counter: Long // شمارنده برای جلوگیری از دوباره‌خرجی
)

@Entity(tableName = "transactions")
data class Tx(
    @PrimaryKey val txId: String,
    val buyerId: String,
    val sellerId: String,
    val amount: Long,
    val status: String, // از TxStatus
    val ts: Long,
    val sigBuyer: String?,
    val sigSeller: String?,
    val receipt: String?
)
