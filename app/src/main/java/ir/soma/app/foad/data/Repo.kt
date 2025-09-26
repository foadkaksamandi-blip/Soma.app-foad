package ir.soma.app.foad.data

class Repo(val db: SomaDb) {

    suspend fun getOrInitWallet(ownerId: String): Wallet {
        val w = db.wallet().get(ownerId)
        return if (w == null) {
            val nw = Wallet(ownerId, balance = 1_000_000, counter = 0) // موجودی اولیهٔ تست
            db.wallet().upsert(nw); nw
        } else w
    }

    suspend fun incCounter(ownerId: String): Long {
        val w = getOrInitWallet(ownerId)
        val nw = w.copy(counter = w.counter + 1)
        db.wallet().upsert(nw)
        return nw.counter
    }

    suspend fun debit(ownerId: String, amount: Long) {
        val w = getOrInitWallet(ownerId)
        require(w.balance >= amount) { "موجودی کافی نیست" }
        db.wallet().upsert(w.copy(balance = w.balance - amount))
    }

    suspend fun credit(ownerId: String, amount: Long) {
        val w = getOrInitWallet(ownerId)
        db.wallet().upsert(w.copy(balance = w.balance + amount))
    }
}
