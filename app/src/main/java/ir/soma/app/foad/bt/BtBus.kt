package ir.soma.app.foad.bt

import java.io.BufferedReader
import java.io.PrintWriter
import java.util.concurrent.atomic.AtomicReference

object BtBus {
    private val wRef = AtomicReference<PrintWriter?>(null)
    private val rRef = AtomicReference<BufferedReader?>(null)

    fun setIo(w: PrintWriter, r: BufferedReader) {
        wRef.set(w); rRef.set(r)
    }
    fun writeLine(s: String) { wRef.get()?.println(s) }
    fun readLine(): String? = rRef.get()?.readLine()
}
