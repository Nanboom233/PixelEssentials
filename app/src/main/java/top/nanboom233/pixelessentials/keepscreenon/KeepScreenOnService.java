package top.nanboom233.pixelessentials.keepscreenon;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.SystemClock;

import top.nanboom233.pixelessentials.ShortcutEntryActivity;
import top.nanboom233.pixelessentials.logging.YukiDiagnosticsLog;

public final class KeepScreenOnService extends Service {
    public static final String ACTION_TOGGLE =
            "top.nanboom233.pixelessentials.action.KEEP_SCREEN_ON_TOGGLE";
    public static final String ACTION_SET_INFINITE =
            "top.nanboom233.pixelessentials.action.KEEP_SCREEN_ON_SET_INFINITE";
    public static final String ACTION_STOP =
            "top.nanboom233.pixelessentials.action.KEEP_SCREEN_ON_STOP";

    private final BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                KeepScreenOnState currentState = sessionManager.readState(SystemClock.elapsedRealtime());
                YukiDiagnosticsLog.info(
                        ShortcutEntryActivity.TAG,
                        "Keep screen on service received ACTION_SCREEN_OFF. active=" + currentState.isActive()
                                + ", durationIndex=" + currentState.getDurationIndex()
                                + ", expiresAt=" + currentState.getExpiresAtElapsedRealtime()
                );
                if (currentState.isActive()) {
                    sessionManager.restoreAndClear(context, currentState, "screen_turned_off");
                }
                stopTracking();
                stopSelf();
            }
        }
    };

    private KeepScreenOnSessionManager sessionManager;
    private KeepScreenOnInteractionModel interactionModel;
    private KeepScreenOnForegroundNotificationHelper foregroundNotificationHelper;
    private CountDownTimer countDownTimer;

    public static void startAction(Context context, String action) {
        YukiDiagnosticsLog.info(ShortcutEntryActivity.TAG, "Keep screen on service start requested. action=" + action);
        Intent intent = new Intent(context, KeepScreenOnService.class).setAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
            return;
        }
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sessionManager = new KeepScreenOnSessionManager(this);
        interactionModel = new KeepScreenOnInteractionModel();
        foregroundNotificationHelper = new KeepScreenOnForegroundNotificationHelper();
        registerReceiver(screenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        YukiDiagnosticsLog.info(
                ShortcutEntryActivity.TAG,
                "Keep screen on service created and ACTION_SCREEN_OFF receiver registered"
        );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long nowElapsedRealtime = SystemClock.elapsedRealtime();
        KeepScreenOnState currentState = sessionManager.readState(nowElapsedRealtime);
        YukiDiagnosticsLog.info(
                ShortcutEntryActivity.TAG,
                "Keep screen on service handling start command. action="
                        + (intent == null ? "<null>" : intent.getAction())
                        + ", startId=" + startId
                        + ", active=" + currentState.isActive()
                        + ", durationIndex=" + currentState.getDurationIndex()
                        + ", expiresAt=" + currentState.getExpiresAtElapsedRealtime()
        );

        if (intent == null || intent.getAction() == null) {
            if (currentState.isActive()) {
                YukiDiagnosticsLog.info(ShortcutEntryActivity.TAG, "Keep screen on service restarting active session tracking");
                startTracking(currentState);
                return START_STICKY;
            }
            YukiDiagnosticsLog.info(ShortcutEntryActivity.TAG, "Keep screen on service has no action and no active session; stopping");
            stopTracking();
            stopSelfResult(startId);
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (!sessionManager.hasWriteSettingsPermission(this) && !ACTION_STOP.equals(action)) {
            YukiDiagnosticsLog.warn(
                    ShortcutEntryActivity.TAG,
                    "Keep screen on action ignored without WRITE_SETTINGS grant. action=" + action
            );
            stopSelfResult(startId);
            return START_NOT_STICKY;
        }

        if (ACTION_STOP.equals(action)) {
            YukiDiagnosticsLog.info(ShortcutEntryActivity.TAG, "Keep screen on service received explicit stop");
            sessionManager.restoreAndClear(this, currentState, "explicit_stop");
            stopTracking();
            stopSelfResult(startId);
            return START_NOT_STICKY;
        }

        KeepScreenOnState nextState = ACTION_SET_INFINITE.equals(action)
                ? interactionModel.handleLongPress(nowElapsedRealtime)
                : interactionModel.handleClick(currentState, nowElapsedRealtime);
        YukiDiagnosticsLog.info(
                ShortcutEntryActivity.TAG,
                "Keep screen on service computed next state. action=" + action
                        + ", nextActive=" + nextState.isActive()
                        + ", nextDurationIndex=" + nextState.getDurationIndex()
                        + ", nextExpiresAt=" + nextState.getExpiresAtElapsedRealtime()
        );

        if (nextState.isActive()) {
            sessionManager.applySession(this, currentState, nextState);
            startTracking(sessionManager.readState(SystemClock.elapsedRealtime()));
            return START_STICKY;
        } else {
            YukiDiagnosticsLog.info(ShortcutEntryActivity.TAG, "Keep screen on service toggled session off");
            sessionManager.restoreAndClear(this, currentState, "toggle_off");
            stopTracking();
        }

        stopSelfResult(startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        YukiDiagnosticsLog.info(ShortcutEntryActivity.TAG, "Keep screen on service destroying");
        stopTracking();
        stopForeground(true);
        foregroundNotificationHelper.cancel(this);
        unregisterReceiver(screenOffReceiver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startTracking(KeepScreenOnState state) {
        stopTracking();
        if (state.isActive()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                        foregroundNotificationHelper.getNotificationId(),
                        foregroundNotificationHelper.createNotification(
                                this,
                                state,
                                SystemClock.elapsedRealtime()
                        ),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                );
            } else {
                startForeground(
                        foregroundNotificationHelper.getNotificationId(),
                        foregroundNotificationHelper.createNotification(
                                this,
                                state,
                                SystemClock.elapsedRealtime()
                        )
                );
            }
            foregroundNotificationHelper.show(this, state, SystemClock.elapsedRealtime());
        }
        if (!state.isActive() || state.isInfinite()) {
            YukiDiagnosticsLog.info(
                    ShortcutEntryActivity.TAG,
                    "Keep screen on service tracking state without countdown. active=" + state.isActive()
                            + ", infinite=" + state.isInfinite()
            );
            return;
        }

        long remainingMs = Math.max(0L, state.getExpiresAtElapsedRealtime() - SystemClock.elapsedRealtime());
        if (remainingMs <= 0L) {
            YukiDiagnosticsLog.warn(
                    ShortcutEntryActivity.TAG,
                    "Keep screen on service found expired countdown while starting tracking"
            );
            sessionManager.restoreAndClear(this, state, "service_timer_expired_immediate");
            stopSelf();
            return;
        }

        YukiDiagnosticsLog.info(
                ShortcutEntryActivity.TAG,
                "Keep screen on service started countdown. remainingMs=" + remainingMs
        );
        countDownTimer = new CountDownTimer(remainingMs, 1_000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                foregroundNotificationHelper.show(
                        KeepScreenOnService.this,
                        state,
                        SystemClock.elapsedRealtime()
                );
                KeepScreenOnTileRefresher.requestRefresh(KeepScreenOnService.this);
            }

            @Override
            public void onFinish() {
                KeepScreenOnState currentState = sessionManager.readState(SystemClock.elapsedRealtime());
                YukiDiagnosticsLog.info(
                        ShortcutEntryActivity.TAG,
                        "Keep screen on service countdown finished. active=" + currentState.isActive()
                                + ", durationIndex=" + currentState.getDurationIndex()
                                + ", expiresAt=" + currentState.getExpiresAtElapsedRealtime()
                );
                sessionManager.restoreAndClear(KeepScreenOnService.this, currentState, "service_timer_expired");
                stopTracking();
                stopSelf();
            }
        }.start();
    }

    private void stopTracking() {
        if (countDownTimer != null) {
            YukiDiagnosticsLog.info(ShortcutEntryActivity.TAG, "Keep screen on service canceled countdown tracking");
            countDownTimer.cancel();
            countDownTimer = null;
        }
        stopForeground(true);
        foregroundNotificationHelper.cancel(this);
    }
}
