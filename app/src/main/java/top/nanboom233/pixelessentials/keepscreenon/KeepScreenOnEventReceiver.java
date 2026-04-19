package top.nanboom233.pixelessentials.keepscreenon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import top.nanboom233.pixelessentials.ShortcutEntryActivity;
import top.nanboom233.pixelessentials.logging.YukiDiagnosticsLog;

public final class KeepScreenOnEventReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        handleStandardEvent(context, intent, intent.getAction());
    }

    private void handleStandardEvent(Context context, Intent intent, String action) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            cleanupXposedStaleState(context, action);
        }

        KeepScreenOnSessionManager sessionManager = new KeepScreenOnSessionManager(context);
        KeepScreenOnState state = sessionManager.readState(SystemClock.elapsedRealtime());
        if (!state.isActive()) {
            YukiDiagnosticsLog.info(
                    ShortcutEntryActivity.TAG,
                    "Keep screen on receiver found no active standard state; performing stale cleanup"
            );
            sessionManager.restoreAndClear(context, "stale_receiver_cleanup");
            return;
        }

        if (KeepScreenOnAlarmScheduler.ACTION_EXPIRE.equals(action)) {
            sessionManager.restoreAndClear(context, state, "timer_expired");
            return;
        }

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            sessionManager.restoreAndClear(context, state, "package_or_boot_recovery");
        }
    }

    private void cleanupXposedStaleState(Context context, String action) {
        KeepScreenOnXposedRouteSnapshotStore snapshotStore = new KeepScreenOnXposedRouteSnapshotStore(context);
        KeepScreenOnState xposedState = snapshotStore.read().getState();
        if (xposedState.isActive()) {
            YukiDiagnosticsLog.info(
                    ShortcutEntryActivity.TAG,
                    "Keep screen on clearing stale Xposed state on " + action
            );
            snapshotStore.writeInactive(SystemClock.elapsedRealtime());
            KeepScreenOnXposedWatcherService.stop(context);
            KeepScreenOnTileRefresher.requestRefresh(context);
        }
    }
}
