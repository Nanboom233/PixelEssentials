package top.nanboom233.pixelessentials.keepscreenon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import top.nanboom233.pixelessentials.ShortcutEntryActivity;
import top.nanboom233.pixelessentials.logging.YukiDiagnosticsLog;

public final class KeepScreenOnXposedCallbackReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();

        if (KeepScreenOnModuleBridge.ACTION_STATUS_CALLBACK.equals(action)
                || KeepScreenOnModuleBridge.ACTION_HEARTBEAT_CALLBACK.equals(action)) {
            handleXposedStatusCallback(context, intent);
            return;
        }

        if (KeepScreenOnModuleBridge.ACTION_MODULE_STOP_CALLBACK.equals(action)) {
            handleXposedModuleStopCallback(context, intent);
        }
    }

    private void handleXposedStatusCallback(Context context, Intent intent) {
        long nowElapsedRealtime = SystemClock.elapsedRealtime();
        boolean active = intent.getBooleanExtra(KeepScreenOnModuleBridge.EXTRA_ACTIVE, false);
        int durationIndex = intent.getIntExtra(KeepScreenOnModuleBridge.EXTRA_DURATION_INDEX, -1);
        long expiresAtElapsedRealtime = intent.getLongExtra(KeepScreenOnModuleBridge.EXTRA_EXPIRES_AT, 0L);
        new KeepScreenOnXposedRouteSnapshotStore(context).updateFromStatusCallback(
                active,
                durationIndex,
                expiresAtElapsedRealtime,
                nowElapsedRealtime
        );
        KeepScreenOnTileRefresher.requestRefresh(context);
        YukiDiagnosticsLog.info(
                ShortcutEntryActivity.TAG,
                "Keep screen on Xposed callback recorded live snapshot. active=" + active
                        + ", durationIndex=" + durationIndex
                        + ", expiresAt=" + expiresAtElapsedRealtime
        );
    }

    private void handleXposedModuleStopCallback(Context context, Intent intent) {
        KeepScreenOnXposedSessionManager sessionManager = new KeepScreenOnXposedSessionManager(context);
        KeepScreenOnState callbackState = sessionManager.readState(SystemClock.elapsedRealtime());
        if (callbackState.isActive()) {
            String stopReason = intent.getStringExtra(KeepScreenOnModuleBridge.EXTRA_STOP_REASON);
            YukiDiagnosticsLog.info(
                    ShortcutEntryActivity.TAG,
                    "Keep screen on Xposed callback handling module stop. stopReason=" + stopReason
            );
            sessionManager.clearFromModuleCallback(
                    "module_callback_" + (stopReason == null ? "unknown" : stopReason)
            );
            return;
        }

        YukiDiagnosticsLog.info(
                ShortcutEntryActivity.TAG,
                "Keep screen on Xposed callback ignored module stop for inactive state"
        );
    }
}
