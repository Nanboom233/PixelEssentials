package top.nanboom233.pixelessentials.hook

import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.CountDownTimer
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import top.nanboom233.pixelessentials.keepscreenon.KeepScreenOnModuleBridge
import top.nanboom233.pixelessentials.logging.YukiDiagnosticsLog

object SystemUiKeepScreenOnController {

    private const val TAG = "PixelEssentialsXposed"
    private const val WAKE_LOCK_TAG = "PixelEssentials:SystemUiKeepScreenOn"
    private const val APP_PACKAGE = "top.nanboom233.pixelessentials"
    private val EVENT_RECEIVER_COMPONENT = ComponentName(
        APP_PACKAGE,
        "$APP_PACKAGE.keepscreenon.KeepScreenOnXposedCallbackReceiver"
    )

    private var installed = false
    private var appContext: Context? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var countDownTimer: CountDownTimer? = null
    private var infiniteHeartbeatHandler: android.os.Handler? = null
    private var infiniteHeartbeatRunnable: Runnable? = null
    private var stopCallback: PendingIntent? = null
    private var currentActive = false
    private var currentDurationIndex = -1
    private var currentExpiresAtElapsedRealtime = 0L
    private var commandReceiver: BroadcastReceiver? = null
    private var screenOffReceiver: BroadcastReceiver? = null
    private var heartbeatTickCount = 0
    private const val HEARTBEAT_INTERVAL_TICKS = 30
    private const val INFINITE_HEARTBEAT_INTERVAL_MS = 30_000L

    fun install(application: Application) {
        if (installed) return
        installed = true
        appContext = application

        val powerManager = application.getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            setReferenceCounted(false)
        }

        val localCommandReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                YukiDiagnosticsLog.info(TAG, "SystemUI keep-screen-on command receiver action=${intent.action}")
                when (intent.action) {
                    KeepScreenOnModuleBridge.ACTION_STOP -> stopSession("bridge_stop", notifyModule = false)
                    KeepScreenOnModuleBridge.ACTION_SYNC -> handleSync(intent)
                    KeepScreenOnModuleBridge.ACTION_STATUS_REQUEST -> handleStatusRequest(intent)
                }
            }
        }
        commandReceiver = localCommandReceiver
        val commandIntentFilter = IntentFilter().apply {
            addAction(KeepScreenOnModuleBridge.ACTION_SYNC)
            addAction(KeepScreenOnModuleBridge.ACTION_STOP)
            addAction(KeepScreenOnModuleBridge.ACTION_STATUS_REQUEST)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(
                localCommandReceiver,
                commandIntentFilter,
                KeepScreenOnModuleBridge.BRIDGE_PERMISSION,
                null,
                Context.RECEIVER_EXPORTED
            )
        } else {
            application.registerReceiver(
                localCommandReceiver,
                commandIntentFilter,
                KeepScreenOnModuleBridge.BRIDGE_PERMISSION,
                null
            )
        }

        val localScreenOffReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (Intent.ACTION_SCREEN_OFF == intent.action) {
                    YukiDiagnosticsLog.info(TAG, "SystemUI keep-screen-on received ACTION_SCREEN_OFF")
                    stopSession("screen_off")
                }
            }
        }
        screenOffReceiver = localScreenOffReceiver
        val screenOffIntentFilter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(localScreenOffReceiver, screenOffIntentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            application.registerReceiver(localScreenOffReceiver, screenOffIntentFilter)
        }
        YukiDiagnosticsLog.info(TAG, "SystemUI keep-screen-on bridge installed")
    }

    private fun handleSync(intent: Intent) {
        val active = intent.getBooleanExtra(KeepScreenOnModuleBridge.EXTRA_ACTIVE, false)
        stopCallback = readStopCallback(intent)
        YukiDiagnosticsLog.info(
            TAG,
            "SystemUI keep-screen-on handling sync. active=$active, durationIndex="
                    + intent.getIntExtra(KeepScreenOnModuleBridge.EXTRA_DURATION_INDEX, -1)
                    + ", expiresAt="
                    + intent.getLongExtra(KeepScreenOnModuleBridge.EXTRA_EXPIRES_AT, 0L)
                    + ", hasStopCallback=${stopCallback != null}"
        )
        if (!active) {
            stopSession("inactive_sync", notifyModule = false)
            return
        }

        currentActive = true
        currentDurationIndex = intent.getIntExtra(KeepScreenOnModuleBridge.EXTRA_DURATION_INDEX, -1)
        val expiresAtElapsedRealtime = intent.getLongExtra(
            KeepScreenOnModuleBridge.EXTRA_EXPIRES_AT,
            0L
        )
        currentExpiresAtElapsedRealtime = expiresAtElapsedRealtime

        acquireWakeLockIfNeeded()
        stopCountDown()
        heartbeatTickCount = 0

        if (expiresAtElapsedRealtime < 0L) {
            YukiDiagnosticsLog.info(TAG, "SystemUI keep-screen-on mirrored as infinite, starting periodic heartbeat")
            startInfiniteHeartbeat()
            return
        }

        val remainingMs = maxOf(0L, expiresAtElapsedRealtime - SystemClock.elapsedRealtime())
        countDownTimer = object : CountDownTimer(remainingMs, 1_000L) {
            override fun onTick(millisUntilFinished: Long) {
                heartbeatTickCount++
                if (heartbeatTickCount % HEARTBEAT_INTERVAL_TICKS == 0) {
                    sendHeartbeat()
                }
            }

            override fun onFinish() {
                stopSession("timer_expired")
            }
        }.start()

        YukiDiagnosticsLog.info(TAG, "SystemUI keep-screen-on mirrored as timed session, remainingMs=$remainingMs")
    }

    private fun handleStatusRequest(intent: Intent) {
        val callback = readStatusCallback(intent)
        if (callback == null) {
            YukiDiagnosticsLog.warn(TAG, "SystemUI keep-screen-on status request missing callback")
            return
        }
        try {
            YukiDiagnosticsLog.info(TAG, "SystemUI keep-screen-on responding to status request")
            callback.send(
                appContext,
                0,
                Intent()
                    .putExtra(KeepScreenOnModuleBridge.EXTRA_ACTIVE, currentActive)
                    .putExtra(KeepScreenOnModuleBridge.EXTRA_DURATION_INDEX, currentDurationIndex)
                    .putExtra(KeepScreenOnModuleBridge.EXTRA_EXPIRES_AT, currentExpiresAtElapsedRealtime)
            )
        } catch (exception: PendingIntent.CanceledException) {
            YukiDiagnosticsLog.warn(TAG, "SystemUI keep-screen-on status callback canceled", exception)
        }
    }

    private fun acquireWakeLockIfNeeded() {
        val localWakeLock = wakeLock ?: return
        if (!localWakeLock.isHeld) {
            YukiDiagnosticsLog.info(TAG, "SystemUI keep-screen-on acquiring wake lock")
            localWakeLock.acquire()
        } else {
            YukiDiagnosticsLog.info(TAG, "SystemUI keep-screen-on wake lock already held")
        }
    }

    private fun stopCountDown() {
        countDownTimer?.cancel()
        countDownTimer = null
        stopInfiniteHeartbeat()
    }

    private fun startInfiniteHeartbeat() {
        stopInfiniteHeartbeat()
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                sendHeartbeat()
                handler.postDelayed(this, INFINITE_HEARTBEAT_INTERVAL_MS)
            }
        }
        infiniteHeartbeatHandler = handler
        infiniteHeartbeatRunnable = runnable
        handler.postDelayed(runnable, INFINITE_HEARTBEAT_INTERVAL_MS)
    }

    private fun stopInfiniteHeartbeat() {
        infiniteHeartbeatRunnable?.let { infiniteHeartbeatHandler?.removeCallbacks(it) }
        infiniteHeartbeatHandler = null
        infiniteHeartbeatRunnable = null
    }

    private fun stopSession(reason: String, notifyModule: Boolean = true) {
        stopCountDown()
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        currentActive = false
        currentDurationIndex = -1
        currentExpiresAtElapsedRealtime = 0L
        val localStopCallback = stopCallback
        stopCallback = null
        if (notifyModule) {
            notifyModuleStopped(localStopCallback, reason)
        }
        YukiDiagnosticsLog.info(TAG, "SystemUI keep-screen-on stopped, reason=$reason")
    }

    private fun notifyModuleStopped(callback: PendingIntent?, reason: String) {
        if (callback == null) {
            YukiDiagnosticsLog.warn(TAG, "SystemUI keep-screen-on has no stop callback to notify. reason=$reason")
            return
        }
        try {
            YukiDiagnosticsLog.info(TAG, "SystemUI keep-screen-on notifying module stop callback. reason=$reason")
            callback.send(
                appContext,
                0,
                Intent().putExtra(KeepScreenOnModuleBridge.EXTRA_STOP_REASON, reason)
            )
        } catch (exception: PendingIntent.CanceledException) {
            YukiDiagnosticsLog.warn(TAG, "SystemUI keep-screen-on stop callback canceled", exception)
        }
    }

    private fun sendHeartbeat() {
        val context = appContext ?: return
        try {
            val intent = Intent(KeepScreenOnModuleBridge.ACTION_HEARTBEAT_CALLBACK)
                .setComponent(EVENT_RECEIVER_COMPONENT)
                .putExtra(KeepScreenOnModuleBridge.EXTRA_ACTIVE, currentActive)
                .putExtra(KeepScreenOnModuleBridge.EXTRA_DURATION_INDEX, currentDurationIndex)
                .putExtra(KeepScreenOnModuleBridge.EXTRA_EXPIRES_AT, currentExpiresAtElapsedRealtime)
            context.sendBroadcast(intent, KeepScreenOnModuleBridge.BRIDGE_PERMISSION)
            YukiDiagnosticsLog.info(TAG, "SystemUI keep-screen-on heartbeat sent")
        } catch (exception: Exception) {
            YukiDiagnosticsLog.warn(TAG, "SystemUI keep-screen-on heartbeat failed", exception)
        }
    }

    private fun readStopCallback(intent: Intent): PendingIntent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(
                KeepScreenOnModuleBridge.EXTRA_STOP_CALLBACK,
                PendingIntent::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(KeepScreenOnModuleBridge.EXTRA_STOP_CALLBACK)
        }
    }

    private fun readStatusCallback(intent: Intent): PendingIntent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(
                KeepScreenOnModuleBridge.EXTRA_STATUS_CALLBACK,
                PendingIntent::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(KeepScreenOnModuleBridge.EXTRA_STATUS_CALLBACK)
        }
    }
}
