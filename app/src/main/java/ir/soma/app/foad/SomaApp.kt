package ir.soma.app.foad

import android.app.Application
import ir.soma.app.foad.data.DbModule
import ir.soma.app.foad.data.Repo

class SomaApp : Application() {
    lateinit var repo: Repo
    override fun onCreate() {
        super.onCreate()
        val db = DbModule.create(this)
        repo = Repo(db)
    }
}
