package ir.soma.app.foad.bt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import java.util.concurrent.CopyOnWriteArrayList

/**
 * اسکن دستگاه‌های بلوتوث کلاسیک و نگهداری لیست کشف‌شده‌ها.
 * ثبت Receiver باید از اکتیویتی انجام شود (در گام ۱۱).
 */
class BtScanner(private val ctx: Context) {

    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val devices = CopyOnWriteArrayList<BluetoothDevice>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BluetoothDevice.ACTION_FOUND == intent.action) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    if (devices.none { d -> d.address == it.address }) devices.add(it)
                }
            }
        }
    }

    fun start(): Boolean {
        if (adapter == null || !adapter.isEnabled) return false
        // پاکسازی لیست قبلی
        devices.clear()
        // ثبت Receiver
        val f = IntentFilter(BluetoothDevice.ACTION_FOUND)
        ctx.registerReceiver(receiver, f)
        // شروع جستجو
        return adapter.startDiscovery()
    }

    fun stop() {
        try { ctx.unregisterReceiver(receiver) } catch (_: Exception) {}
        adapter?.cancelDiscovery()
    }

    fun list(): List<BluetoothDevice> = devices.toList()
}
