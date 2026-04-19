package top.nanboom233.pixelessentials.keepscreenon;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;

import top.nanboom233.pixelessentials.ShortcutEntryActivity;
import top.nanboom233.pixelessentials.logging.YukiDiagnosticsLog;

public final class KeepScreenOnXposedWatcherService extends Service {
    private static final String TAG = ShortcutEntryActivity.TAG;
    private static final long HEARTBEAT_STALE_THRESHOLD_MS = 60_000L;
    private static final long STATUS_REQUEST_GRACE_MS = 5_000L;
    private static final int STALE_CHECK_INTERVAL_TICKS = 30;
    private static final long INFINITE_STALE_CHECK_INTERVAL_MS = 30_000L;

    private KeepScreenOnXposedRouteSnapshotStore snapshotStore;
    private KeepScreenOnForegroundNotificationHelper notificationHelper;
    private KeepScreenOnModuleBridge moduleBridge;
    private CountDownTimer countDownTimer;
    private Handler infiniteCheckHandler;
    private Runnable infiniteCheckRunnable;
    private int tickCount;
    private boolean statusRequestPending;
    private long statusRequestSentAt;

    private final BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                YukiDiagnosticsLog.info(TAG, "Xposed watcher received SCREEN_OFF, clearing session");
                clearSessionAndStop("screen_off");
            }
        }
    };

    public static void start(Context context) {
        Intent intent = new Intent(context, KeepScreenOnXposedWatcherService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, KeepScreenOnXposedWatcherService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        snapshotStore = new KeepScreenOnXposedRouteSnapshotStore(this);
        notificationHelper = new KeepScreenOnForegroundNotificationHelper();
        moduleBridge = new KeepScreenOnModuleBridge();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF),
                    Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(screenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        }
        YukiDiagnosticsLog.info(TAG, "Xposed watcher service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        KeepScreenOnXposedRouteSnapshot snapshot = snapshotStore.read();
        KeepScreenOnState state = snapshot.getState();
        if (!state.isActive()) {
            YukiDiagnosticsLog.info(TAG, "Xposed watcher started but no active session, stopping");
            promoteToForeground(KeepScreenOnState.inactive(SystemClock.elapsedRealtime()));
            stopSelf();
            return START_NOT_STICKY;
        }
        startTracking(state);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        YukiDiagnosticsLog.info(TAG, "Xposed watcher service destroying");
        stopTracking();
        stopForeground(true);
        notificationHelper.cancel(this);
        unregisterReceiver(screenOffReceiver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startTracking(KeepScreenOnState state) {
        stopTracking();
        tickCount = 0;
        statusRequestPending = false;
        statusRequestSentAt = 0L;
        promoteToForeground(state);

        if (state.isInfinite()) {
            YukiDiagnosticsLog.info(TAG, "Xposed watcher tracking infinite session with periodic stale check");
            startInfiniteStaleCheck();
            return;
        }

        long remainingMs = Math.max(0L, state.getExpiresAtElapsedRealtime() - SystemClock.elapsedRealtime());
        if (remainingMs <= 0L) {
            YukiDiagnosticsLog.warn(TAG, "Xposed watcher found expired session on start");
            clearSessionAndStop("expired_on_start");
            return;
        }

        YukiDiagnosticsLog.info(TAG, "Xposed watcher started countdown. remainingMs=" + remainingMs);
        countDownTimer = new CountDownTimer(remainingMs, 1_000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long now = SystemClock.elapsedRealtime();
                KeepScreenOnXposedRouteSnapshot currentSnapshot = snapshotStore.read();
                KeepScreenOnState currentState = currentSnapshot.getState();
                notificationHelper.show(
                        KeepScreenOnXposedWatcherService.this,
                        currentState,
                        now
                );
                KeepScreenOnTileRefresher.requestRefresh(KeepScreenOnXposedWatcherService.this);

                tickCount++;
                if (tickCount % STALE_CHECK_INTERVAL_TICKS == 0) {
                    checkHeartbeatFreshness(currentSnapshot, now);
                }
            }

            @Override
            public void onFinish() {
                YukiDiagnosticsLog.info(TAG, "Xposed watcher countdown finished");
                clearSessionAndStop("watcher_timer_expired");
            }
        }.start();
    }

    private void stopTracking() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        stopInfiniteStaleCheck();
        stopForeground(true);
        notificationHelper.cancel(this);
    }

    private void startInfiniteStaleCheck() {
        stopInfiniteStaleCheck();
        infiniteCheckHandler = new Handler(Looper.getMainLooper());
        infiniteCheckRunnable = new Runnable() {
            @Override
            public void run() {
                long now = SystemClock.elapsedRealtime();
                KeepScreenOnXposedRouteSnapshot snapshot = snapshotStore.read();
                KeepScreenOnTileRefresher.requestRefresh(KeepScreenOnXposedWatcherService.this);
                checkHeartbeatFreshness(snapshot, now);
                if (infiniteCheckHandler != null) {
                    infiniteCheckHandler.postDelayed(this, INFINITE_STALE_CHECK_INTERVAL_MS);
                }
            }
        };
        infiniteCheckHandler.postDelayed(infiniteCheckRunnable, INFINITE_STALE_CHECK_INTERVAL_MS);
    }

    private void stopInfiniteStaleCheck() {
        if (infiniteCheckRunnable != null && infiniteCheckHandler != null) {
            infiniteCheckHandler.removeCallbacks(infiniteCheckRunnable);
        }
        infiniteCheckHandler = null;
        infiniteCheckRunnable = null;
    }

    private void promoteToForeground(KeepScreenOnState state) {
        long now = SystemClock.elapsedRealtime();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                    notificationHelper.getNotificationId(),
                    notificationHelper.createNotification(this, state, now),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            );
        } else {
            startForeground(
                    notificationHelper.getNotificationId(),
                    notificationHelper.createNotification(this, state, now)
            );
        }
    }

    private void checkHeartbeatFreshness(KeepScreenOnXposedRouteSnapshot snapshot, long now) {
        long lastCallback = snapshot.getLastStatusCallbackElapsedRealtime();
        long staleness = now - lastCallback;
        if (staleness <= HEARTBEAT_STALE_THRESHOLD_MS) {
            statusRequestPending = false;
            return;
        }
        if (!statusRequestPending) {
            YukiDiagnosticsLog.warn(TAG, "Xposed watcher heartbeat stale (" + staleness + "ms), sending status request");
            moduleBridge.requestStatus(this);
            statusRequestPending = true;
            statusRequestSentAt = now;
            return;
        }
        if (now - statusRequestSentAt >= STATUS_REQUEST_GRACE_MS) {
            YukiDiagnosticsLog.warn(TAG, "Xposed watcher no response within grace period, assuming session dead");
            clearSessionAndStop("heartbeat_timeout");
        }
    }

    private void clearSessionAndStop(String reason) {
        snapshotStore.writeInactive(SystemClock.elapsedRealtime());
        moduleBridge.sendStop(this);
        KeepScreenOnTileRefresher.requestRefresh(this);
        YukiDiagnosticsLog.info(TAG, "Xposed watcher cleared session. reason=" + reason);
        stopTracking();
        stopSelf();
    }
}
