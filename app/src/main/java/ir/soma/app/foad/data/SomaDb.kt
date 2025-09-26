package ir.soma.app.foad.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Wallet::class, Tx::class], version = 1, exportSchema = false)
abstract class SomaDb : RoomDatabase() {
    abstract fun wallet(): WalletDao
    abstract fun tx(): TxDao
}
