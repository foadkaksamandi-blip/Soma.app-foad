package ir.soma.app.foad.data

import androidx.room.*

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallet WHERE ownerId = :id")
    suspend fun get(id: String): Wallet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(w: Wallet)
}

@Dao
interface TxDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(tx: Tx)

    @Query("UPDATE transactions SET status=:status, sigSeller=:sigS, receipt=:receipt WHERE txId=:txId")
    suspend fun updateStatus(txId: String, status: String, sigS: String?, receipt: String?)

    @Query("SELECT * FROM transactions WHERE txId=:txId")
    suspend fun get(txId: String): Tx?
}
