package ir.soma.app.foad.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import ir.soma.app.foad.BuildConfig           // ← اضافه شد
import ir.soma.app.foad.R                    // ← اضافه شد
import ir.soma.app.foad.SomaApp
import ir.soma.app.foad.auth.Auth
import ir.soma.app.foad.bt.BluetoothService
import ir.soma.app.foad.bt.BtScanner
import ir.soma.app.foad.data.Repo
import ir.soma.app.foad.proto.BtProto
import ir.soma.app.foad.qr.Qr
import kotlinx.coroutines.*
import java.text.NumberFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var repo: Repo

    private lateinit var tvRole: TextView
    private lateinit var tvBalance: TextView
    private lateinit var btnStartServer: Button
    private lateinit var btnScan: Button
    private lateinit var lvDevices: ListView
    private lateinit var tvSelected: TextView
    private lateinit var etAmount: EditText
    private lateinit var btnPay: Button
    private lateinit var tvResult: TextView
    private lateinit var imgQr: ImageView

    private var isBuyer: Boolean = false
    private var selectedMac: String? = null
    private var scanner: BtScanner? = null

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = (application as SomaApp).repo
        setContentView(R.layout.activity_main)

        tvRole = findViewById(R.id.tvRole)
        tvBalance = findViewById(R.id.tvBalance)
        btnStartServer = findViewById(R.id.btnStartServer)
        btnScan = findViewById(R.id.btnScan)
        lvDevices = findViewById(R.id.lvDevices)
        tvSelected = findViewById(R.id.tvSelected)
        etAmount = findViewById(R.id.etAmount)
        btnPay = findViewById(R.id.btnPay)
        tvResult = findViewById(R.id.tvResult)
        imgQr = findViewById(R.id.imgQr)

        // نقش از روی flavor
        isBuyer = BuildConfig.FLAVOR.contains("buyer")
        tvRole.text = if (isBuyer) "نقش: خریدار" else "نقش: فروشنده"

        // موجودی
        scope.launch(Dispatchers.IO) {
            val id = if (isBuyer) "buyer-001" else "seller-001"
            val w = repo.getOrInitWallet(id)
            withContext(Dispatchers.Main) { tvBalance.text = "موجودی: ${formatRials(w.balance)}" }
        }

        requestPerms()

        // فروشنده: شروع سرور و حلقهٔ دریافت
        btnStartServer.setOnClickListener {
            if (isBuyer) { toast("این دکمه مخصوص فروشنده است"); return@setOnClickListener }
            val svc = Intent(this, BluetoothService::class.java).apply {
                action = BluetoothService.ACTION_START_SERVER
            }
            startForegroundService(svc)
            toast("سرور بلوتوث فعال شد؛ منتظر پرداخت خریدار باشید")

            scope.launch(Dispatchers.IO) {
                val proto = BtProto(repo, "seller-001", isSeller = true)
                while (true) {
                    val txId = proto.handleAsSeller() ?: continue
                    withContext(Dispatchers.Main) {
                        tvResult.text = "✅ تراکنش موفق — کد: $txId"
                        imgQr.setImageBitmap(Qr.make("TX:$txId", 512))
                        scope.launch(Dispatchers.IO) {
                            val w = repo.getOrInitWallet("seller-001")
                            withContext(Dispatchers.Main) {
                                tvBalance.text = "موجودی: ${formatRials(w.balance)}"
                            }
                        }
                    }
                }
            }
        }

        // خریدار: اسکن و انتخاب
        btnScan.setOnClickListener {
            if (!isBuyer) { toast("این دکمه مخصوص خریدار است"); return@setOnClickListener }
            if (BluetoothAdapter.getDefaultAdapter()?.isEnabled != true) {
                toast("بلوتوث را روشن کنید"); return@setOnClickListener
            }
            scanner?.stop()
            scanner = BtScanner(this)
            val ok = scanner!!.start()
            if (!ok) { toast("اسکن شروع نشد"); return@setOnClickListener }

            scope.launch {
                delay(2500)
                val list = scanner!!.list()
                val items = list.map { "${it.name ?: "بدون‌نام"} | ${it.address}" }
                lvDevices.adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, items)
                lvDevices.setOnItemClickListener { _, _, pos, _ ->
                    selectedMac = list[pos].address
                    tvSelected.text = "دستگاه انتخاب‌شده: ${list[pos].name ?: "-"} (${selectedMac})"
                    toast("انتخاب شد: ${selectedMac}")
                    scanner?.stop()
                }
            }
        }

        // خریدار: پرداخت با تأیید هویت
        btnPay.setOnClickListener {
            if (!isBuyer) { toast("این دکمه مخصوص خریدار است"); return@setOnClickListener }
            val mac = selectedMac
            val amount = etAmount.text.toString().toLongOrNull() ?: 0L
            if (mac.isNullOrBlank()) { toast("ابتدا دستگاه فروشنده را از لیست انتخاب کنید"); return@setOnClickListener }
            if (amount <= 0) { toast("مبلغ معتبر وارد کنید"); return@setOnClickListener }

            if (Auth.canUseBiometric(this)) {
                Auth.prompt(this,
                    onSuccess = { startPayment(mac, amount) },
                    onFail = { toast("تأیید هویت انجام نشد") }
                )
            } else {
                startPayment(mac, amount)
            }
        }

        // نمایش/عدم نمایش اجزا بر اساس نقش
        btnStartServer.isVisible = !isBuyer
        btnScan.isVisible = isBuyer
        lvDevices.isVisible = isBuyer
        tvSelected.isVisible = isBuyer
        etAmount.isVisible = isBuyer
        btnPay.isVisible = isBuyer
    }

    private fun startPayment(mac: String, amount: Long) {
        val svc = Intent(this, BluetoothService::class.java).apply {
            action = BluetoothService.ACTION_CONNECT_TO
            putExtra(BluetoothService.EXTRA_DEVICE_ADDRESS, mac)
        }
        startForegroundService(svc)
        tvResult.text = "در حال پرداخت…"

        scope.launch(Dispatchers.IO) {
            val proto = BtProto(repo, "buyer-001", isSeller = false)
            val txId = proto.startAsBuyer("seller-001", amount)
            withContext(Dispatchers.Main) {
                if (txId == null) {
                    tvResult.text = "❌ تراکنش ناموفق"
                } else {
                    tvResult.text = "✅ تراکنش موفق — کد: $txId"
                    imgQr.setImageBitmap(Qr.make("TX:$txId", 512))
                    scope.launch(Dispatchers.IO) {
                        val w = repo.getOrInitWallet("buyer-001")
                        withContext(Dispatchers.Main) {
                            tvBalance.text = "موجودی: ${formatRials(w.balance)}"
                        }
                    }
                }
            }
        }
    }

    private fun requestPerms() {
        val perms = buildList {
            if (Build.VERSION.SDK_INT >= 31) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
            } else {
                add(Manifest.permission.BLUETOOTH)
                add(Manifest.permission.BLUETOOTH_ADMIN)
            }
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }.toTypedArray()
        permLauncher.launch(perms)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun formatRials(v: Long): String {
        val nf = NumberFormat.getInstance(Locale("fa"))
        return nf.format(v) + " ریال"
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
