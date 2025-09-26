package ir.soma.app.foad.bt

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Build
import android.os.IBinder
import ir.soma.app.foad.R
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.util.UUID

class BluetoothService : Service() {

    companion object {
        const val CHANNEL_ID = "SOMA_BT"
        val SOMA_UUID: UUID = UUID.fromString("8e0f1f4c-4a62-4d8c-9b67-0b7b8c3f8c00")

        const val ACTION_START_SERVER = "START_SERVER"
        const val ACTION_CONNECT_TO = "CONNECT_TO"
        const val EXTRA_DEVICE_ADDRESS = "DEVICE_ADDRESS"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: BluetoothServerSocket? = null
    private var socket: BluetoothSocket? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val n = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("سوما: اتصال آفلاین")
            .setContentText("کانال امن بلوتوث فعال است")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
        startForeground(1, n)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVER -> startServer()
            ACTION_CONNECT_TO -> {
                val mac = intent.getStringExtra(EXTRA_DEVICE_ADDRESS) ?: return START_STICKY
                connectTo(mac)
            }
        }
        return START_STICKY
    }

    private fun startServer() {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
        serverSocket = adapter.listenUsingRfcommWithServiceRecord("SOMA", SOMA_UUID)
        scope.launch {
            try {
                val sock = serverSocket?.accept()
                socket = sock
                sock?.let { handleConnection(it) }
            } catch (_: Exception) { }
        }
    }

    private fun connectTo(mac: String) {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
        scope.launch {
            try {
                val device: BluetoothDevice = adapter.getRemoteDevice(mac)
                adapter.cancelDiscovery()
                val sock = device.createRfcommSocketToServiceRecord(SOMA_UUID)
                sock.connect()
                socket = sock
                handleConnection(sock)
            } catch (_: Exception) { }
        }
    }

    private suspend fun handleConnection(sock: BluetoothSocket) {
        val reader = BufferedReader(InputStreamReader(sock.inputStream))
        val writer = PrintWriter(sock.outputStream, true)
        BtBus.setIo(writer, reader)
        // منطق پروتکل در گام ۸ (BtProto) متصل می‌شود.
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL_ID, "SOMA Bluetooth", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    override fun onDestroy() {
        try { socket?.close() } catch (_: Exception) {}
        try { serverSocket?.close() } catch (_: Exception) {}
        scope.cancel()
        super.onDestroy()
    }
}
