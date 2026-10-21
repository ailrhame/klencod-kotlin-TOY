package com.toy.moy

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val colorBg = 0xFF0D1117.toInt()
    private val colorSurface = 0xFF161B22.toInt()
    private val colorSurfaceAlt = 0xFF21262D.toInt()
    private val colorPrimary = 0xFF1F6FEB.toInt()
    private val colorText = 0xFFE6EDF3.toInt()
    private val colorMuted = 0xFF8B949E.toInt()
    private val colorBorder = 0xFF30363D.toInt()
    private val colorSuccess = 0xFF3FB950.toInt()
    private val colorDanger = 0xFFF85149.toInt()
    private val colorGreen = 0xFF238636.toInt()
    private val colorRed = 0xFFDA3633.toInt()

    private lateinit var etMessage: EditText
    private lateinit var etSeconds: EditText
    private lateinit var tvAdminStatus: TextView
    private lateinit var tvNotifStatus: TextView

    private val prefs by lazy {
        getSharedPreferences("fakelock", Context.MODE_PRIVATE)
    }

    private fun dp(v: Int): Int = Math.round(v * resources.displayMetrics.density)

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.statusBarColor = colorBg
        window.navigationBarColor = colorBg

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colorBg)
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, -1))

        val title = TextView(this).apply {
            text = "🔒 FakeLock (Call)"
            setTextColor(colorText)
            setTextSize(24f)
            typeface = Typeface.DEFAULT_BOLD
        }
        val subtitle = TextView(this).apply {
            text = "شاشة اتصال وهمي عند محاولة فتح القفل"
            setTextColor(colorMuted)
            setTextSize(13f)
            setPadding(0, dp(4), 0, dp(20))
        }
        content.addView(title)
        content.addView(subtitle)

        val settingsCard = makeCard("⚙ الإعدادات")
        settingsCard.addView(makeLabel("اسم المتصل"))
        etMessage = EditText(this).apply {
            setTextColor(colorText)
            setHintTextColor(colorMuted)
            hint = "جهة اتصال مجهولة"
            setTextSize(14f)
            background = makeInputDrawable()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setText(prefs.getString("message", "جهة اتصال مجهولة"))
            setSelection(text.length)
        }
        settingsCard.addView(etMessage, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })

        settingsCard.addView(makeLabel("مدة الرنين (ثانية)").apply { setPadding(0, dp(12), 0, 0) })
        etSeconds = EditText(this).apply {
            setTextColor(colorText)
            setHintTextColor(colorMuted)
            hint = "15"
            inputType = InputType.TYPE_CLASS_NUMBER
            setTextSize(14f)
            background = makeInputDrawable()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setText(prefs.getInt("seconds", 15).toString())
        }
        settingsCard.addView(etSeconds, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })

        val saveBtn = makePrimaryButton("💾 حفظ الإعدادات", colorSurfaceAlt, colorBorder, colorText)
        saveBtn.setOnClickListener { saveSettings() }
        settingsCard.addView(saveBtn, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(14) })
        content.addView(settingsCard, cardLp())

        val permCard = makeCard("🔑 الصلاحيات المطلوبة")
        
        val adminRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        adminRow.addView(makeLabel("Device Admin").apply { setTextSize(14f); setTextColor(colorText) }, LinearLayout.LayoutParams(0, -2, 1f))
        tvAdminStatus = TextView(this).apply { setTextSize(12f); setPadding(dp(8), 0, dp(10), 0) }
        adminRow.addView(tvAdminStatus)
        adminRow.addView(makeSmallButton("تفعيل").apply { setOnClickListener { requestAdmin() } }, LinearLayout.LayoutParams(-2, dp(34)))
        permCard.addView(adminRow)

        val notifRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        notifRow.addView(makeLabel("إشعارات المكالمات").apply { setTextSize(14f); setTextColor(colorText) }, LinearLayout.LayoutParams(0, -2, 1f))
        tvNotifStatus = TextView(this).apply { setTextSize(12f); setPadding(dp(8), 0, dp(10), 0) }
        notifRow.addView(tvNotifStatus)
        notifRow.addView(makeSmallButton("تفعيل").apply { setOnClickListener { requestNotificationPermission() } }, LinearLayout.LayoutParams(-2, dp(34)))
        permCard.addView(notifRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        content.addView(permCard, cardLp())

        val ctrlCard = makeCard("▶ التحكم")
        val startBtn = makePrimaryButton("تشغيل المراقبة", colorGreen, colorSuccess, Color.WHITE)
        startBtn.setOnClickListener { startMonitoring() }
        ctrlCard.addView(startBtn, LinearLayout.LayoutParams(-1, dp(52)))

        val testBtn = makePrimaryButton("🧪 اختبار الاتصال الوهمي", colorPrimary, colorPrimary, Color.WHITE)
        testBtn.setOnClickListener { testFakeCall() }
        ctrlCard.addView(testBtn, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(10) })
        content.addView(ctrlCard, cardLp())

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    private fun makeCard(titleText: String): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = GradientDrawable().apply {
                setColor(colorSurface)
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), colorBorder)
            }
        }
        card.addView(TextView(this).apply {
            text = titleText
            setTextColor(colorText)
            setTextSize(15f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(10))
        })
        return card
    }

    private fun cardLp() = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
    private fun makeLabel(text: String) = TextView(this).apply { this.text = text; setTextColor(colorMuted); setTextSize(12f) }
    private fun makeInputDrawable() = GradientDrawable().apply { setColor(colorSurfaceAlt); cornerRadius = dp(8).toFloat(); setStroke(dp(1), colorBorder) }
    private fun makePrimaryButton(text: String, bg: Int, stroke: Int, txt: Int) = Button(this).apply {
        this.text = text; setTextSize(14f); isAllCaps = false; setTextColor(txt); typeface = Typeface.DEFAULT_BOLD
        background = GradientDrawable().apply { setColor(bg); cornerRadius = dp(12).toFloat(); setStroke(dp(1), stroke) }
    }
    private fun makeSmallButton(text: String) = Button(this).apply {
        this.text = text; setTextSize(12f); isAllCaps = false; setTextColor(Color.WHITE)
        background = GradientDrawable().apply { setColor(colorPrimary); cornerRadius = dp(8).toFloat() }
        setPadding(dp(14), 0, dp(14), 0)
    }

    private fun refreshPermissions() {
        val dpm = getSystemService(DevicePolicyManager::class.java)
        val comp = ComponentName(this, MyDeviceAdminReceiver::class.java)
        val adminOn = dpm != null && dpm.isAdminActive(comp)
        tvAdminStatus.text = if (adminOn) "✅" else "❌"
        tvAdminStatus.setTextColor(if (adminOn) colorSuccess else colorDanger)

        val notifOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        tvNotifStatus.text = if (notifOn) "✅" else "❌"
        tvNotifStatus.setTextColor(if (notifOn) colorSuccess else colorDanger)
    }

    private fun requestAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName(this, MyDeviceAdminReceiver::class.java))
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "مطلوب لمراقبة محاولات فتح القفل الفاشلة")
        try { startActivity(intent) } catch (e: Exception) { Toast.makeText(this, "خطأ: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        } else {
            Toast.makeText(this, "الصلاحية مفعلة تلقائياً", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSettings() {
        val msg = etMessage.text.toString().ifBlank { "جهة اتصال مجهولة" }
        val sec = etSeconds.text.toString().toIntOrNull()?.coerceIn(3, 3600) ?: 15
        prefs.edit().putString("message", msg).putInt("seconds", sec).apply()
        Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show()
    }

    private fun startMonitoring() {
        val dpm = getSystemService(DevicePolicyManager::class.java)
        val comp = ComponentName(this, MyDeviceAdminReceiver::class.java)
        if (dpm == null || !dpm.isAdminActive(comp)) {
            Toast.makeText(this, "يجب تفعيل Device Admin أولاً", Toast.LENGTH_LONG).show()
            return
        }
        val svc = Intent(this, LockMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc) else startService(svc)
        Toast.makeText(this, "المراقبة تعمل الآن", Toast.LENGTH_SHORT).show()
    }

    private fun testFakeCall() {
        FakeCallManager.triggerFakeCall(this)
    }
}

// ============================================================
// FakeCallManager: إدارة إشعار المكالمة الوهمية
// ============================================================
object FakeCallManager {
    private const val CHANNEL_ID = "fake_call_channel"

    fun triggerFakeCall(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "مكالمات واردة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setShowBadge(false)
                setSound(null, null)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(channel)
        }

        val fullScreenIntent = Intent(context, FakeCallActivity::class.java)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context).setPriority(Notification.PRIORITY_MAX)
        }

        val notification = builder
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("مكالمة واردة")
            .setContentText("جهة اتصال مجهولة")
            .setCategory(Notification.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        nm.notify(999, notification)
    }
}

// ============================================================
// FakeCallActivity: شاشة المكالمة الوهمية
// ============================================================
class FakeCallActivity : Activity() {
    private var countdownTimer: CountDownTimer? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val prefs = getSharedPreferences("fakelock", Context.MODE_PRIVATE)
        val callerName = prefs.getString("message", "جهة اتصال مجهولة") ?: "جهة اتصال مجهولة"
        val seconds = prefs.getInt("seconds", 15).toLong()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF111111.toInt())
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(32, 100, 32, 80)
        }

        val tvStatus = TextView(this).apply {
            text = "مكالمة واردة..."
            setTextColor(0xFFAAAAAA.toInt())
            textSize = 18f
            gravity = Gravity.CENTER
        }

        val tvCaller = TextView(this).apply {
            text = callerName
            setTextColor(Color.WHITE)
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }

        val tvTimer = TextView(this).apply {
            setTextColor(0xFFAAAAAA.toInt())
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }

        val spacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 1f) }

        val buttonsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        // زر الرفض
        val declineBtn = Button(this).apply {
            text = "📞\nرفض"
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFFDA3633.toInt())
            }
            setOnClickListener { dismissCall() }
        }
        val declineLp = LinearLayout.LayoutParams(100, 100).apply { setMargins(0, 0, 60, 0) }

        // زر الرد
        val answerBtn = Button(this).apply {
            text = "📞\nرد"
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF238636.toInt())
            }
            setOnClickListener { dismissCall() }
        }
        val answerLp = LinearLayout.LayoutParams(100, 100)

        buttonsRow.addView(declineBtn, declineLp)
        buttonsRow.addView(answerBtn, answerLp)

        root.addView(tvStatus)
        root.addView(tvCaller)
        root.addView(tvTimer)
        root.addView(spacer)
        root.addView(buttonsRow)

        setContentView(root)

        tvTimer.text = "الرنين... $seconds"

        countdownTimer = object : CountDownTimer(seconds * 1000, 1000) {
            override fun onTick(ms: Long) {
                tvTimer.text = "الرنين... ${ms / 1000}"
            }
            override fun onFinish() {
                dismissCall()
            }
        }.start()
    }

    private fun dismissCall() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(999)
        finish()
    }

    override fun onBackPressed() {
        // منع زر الرجوع
    }

    override fun onDestroy() {
        countdownTimer?.cancel()
        super.onDestroy()
    }
}

// ============================================================
// مستقبل المحاولات الفاشلة
// ============================================================
class MyDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        FakeCallManager.triggerFakeCall(context)
    }
}

// ============================================================
// خدمة خلفية
// ============================================================
class LockMonitorService : Service() {
    private val CHANNEL_ID = "fakelock_service"
    private val NOTIF_ID = 1001

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "FakeLock", NotificationManager.IMPORTANCE_LOW))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notif = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("🔒 FakeLock")
            .setContentText("المراقبة نشطة")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
        
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notif, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notif)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

// ============================================================
// التشغيل التلقائي
// ============================================================
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val svc = Intent(context, LockMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(svc) else context.startService(svc)
        }
    }
}