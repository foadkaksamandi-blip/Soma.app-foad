package ir.soma.app.foad.data

import android.content.Context
import androidx.room.Room

object DbModule {
    fun create(ctx: Context): SomaDb =
        Room.databaseBuilder(ctx, SomaDb::class.java, "soma.db").build()
}
