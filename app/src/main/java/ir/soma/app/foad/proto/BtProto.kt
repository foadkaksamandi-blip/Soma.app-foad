package ir.soma.app.foad.proto

import ir.soma.app.foad.bt.BtBus
import ir.soma.app.foad.crypto.Crypto
import ir.soma.app.foad.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

/**
 * پروتکل تراکنش:
 * 1. خریدار: PayInit
 * 2. فروشنده: PayPrepare
 * 3. خریدار: PayCommit
 * 4. فروشنده: Receipt
 */
class BtProto(private val repo: Repo, private val ownerId: String, private val isSeller: Boolean) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun startAsBuyer(sellerId: String, amount: Long): String? = withContext(Dispatchers.IO) {
        try {
            val ts = System.currentTimeMillis()
            val counter = repo.incCounter(ownerId)
            val txId = Crypto.txId(ownerId, sellerId, amount, ts, counter)

            // مرحله 1: PayInit
            val init = PayInit(
                buyerId = ownerId,
                sellerId = sellerId,
                amount = amount,
                ts = ts,
                counter = counter,
                sigB = Crypto.sign("$ownerId|$sellerId|$amount|$ts|$counter".toByteArray())
            )
            BtBus.writeLine(json.encodeToString(init))

            // مرحله 2: دریافت PayPrepare
            val line2 = BtBus.readLine() ?: return@withContext null
            val prepare = json.decodeFromString(PayPrepare.serializer(), line2)
            if (!prepare.ok) return@withContext null

            // موجودی کم شود
            repo.debit(ownerId, amount)

            // مرحله 3: PayCommit
            val commit = PayCommit(txId = txId, sigB = Crypto.sign(txId.toByteArray()))
            BtBus.writeLine(json.encodeToString(commit))

            // مرحله 4: دریافت Receipt
            val line4 = BtBus.readLine() ?: return@withContext null
            val receipt = json.decodeFromString(Receipt.serializer(), line4)

            // تراکنش ذخیره شود
            repo.db.tx().insert(
                Tx(
                    txId, ownerId, sellerId, amount,
                    TxStatus.ACKED.name, ts,
                    init.sigB, receipt.sigS, receipt.sigS
                )
            )

            return@withContext txId
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun handleAsSeller(): String? = withContext(Dispatchers.IO) {
        try {
            // مرحله 1: دریافت PayInit
            val line1 = BtBus.readLine() ?: return@withContext null
            val init = json.decodeFromString(PayInit.serializer(), line1)

            val txId = Crypto.txId(init.buyerId, ownerId, init.amount, init.ts, init.counter)

            // مرحله 2: پاسخ PayPrepare
            val prepare = PayPrepare(
                txId = txId,
                ok = true,
                ts = System.currentTimeMillis(),
                sigS = Crypto.sign("$txId|PREPARE".toByteArray())
            )
            BtBus.writeLine(json.encodeToString(prepare))

            // مرحله 3: دریافت PayCommit
            val line3 = BtBus.readLine() ?: return@withContext null
            val commit = json.decodeFromString(PayCommit.serializer(), line3)

            // موجودی اضافه شود
            repo.credit(ownerId, init.amount)

            // مرحله 4: ارسال Receipt
            val receipt = Receipt(
                txId = txId,
                sigS = Crypto.sign("$txId|RECEIPT".toByteArray())
            )
            BtBus.writeLine(json.encodeToString(receipt))

            // ذخیره تراکنش
            repo.db.tx().insert(
                Tx(
                    txId, init.buyerId, ownerId, init.amount,
                    TxStatus.ACKED.name, init.ts,
                    init.sigB, receipt.sigS, receipt.sigS
                )
            )

            return@withContext txId
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}
