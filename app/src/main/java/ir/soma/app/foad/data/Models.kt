package ir.soma.app.foad.data

import kotlinx.serialization.Serializable

@Serializable
data class PayInit(
    val type: String = "PAY_INIT",
    val buyerId: String,
    val sellerId: String,
    val amount: Long,
    val ts: Long,
    val counter: Long,
    val sigB: String
)

@Serializable
data class PayPrepare(
    val type: String = "PAY_PREPARE",
    val txId: String,
    val ok: Boolean,
    val ts: Long,
    val sigS: String
)

@Serializable
data class PayCommit(
    val type: String = "PAY_COMMIT",
    val txId: String,
    val sigB: String
)

@Serializable
data class Receipt(
    val type: String = "RECEIPT",
    val txId: String,
    val sigS: String
)

enum class TxStatus { INIT, PREPARED, COMMITTED, ACKED, FAILED }
