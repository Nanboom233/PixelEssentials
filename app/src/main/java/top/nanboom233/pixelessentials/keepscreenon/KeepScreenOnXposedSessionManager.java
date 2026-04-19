package top.nanboom233.pixelessentials.keepscreenon;

import android.content.Context;
import android.os.SystemClock;

import top.nanboom233.pixelessentials.ShortcutEntryActivity;
import top.nanboom233.pixelessentials.logging.YukiDiagnosticsLog;

public final class KeepScreenOnXposedSessionManager {
    private final Context appContext;
    private final KeepScreenOnXposedRouteSnapshotStore snapshotStore;
    private final KeepScreenOnInteractionModel interactionModel;
    private final KeepScreenOnModuleBridge moduleBridge;

    public KeepScreenOnXposedSessionManager(Context context) {
        appContext = context.getApplicationContext();
        snapshotStore = new KeepScreenOnXposedRouteSnapshotStore(appContext);
        interactionModel = new KeepScreenOnInteractionModel();
        moduleBridge = new KeepScreenOnModuleBridge();
    }

    public KeepScreenOnState readState(long nowElapsedRealtime) {
        KeepScreenOnState storedState = snapshotStore.read().getState();
        return interactionModel.normalize(storedState, nowElapsedRealtime);
    }

    public KeepScreenOnState reconcileExpiredState(long nowElapsedRealtime) {
        KeepScreenOnState storedState = snapshotStore.read().getState();
        KeepScreenOnState normalizedState = interactionModel.normalize(storedState, nowElapsedRealtime);
        if (storedState.isActive() && !normalizedState.isActive()) {
            YukiDiagnosticsLog.info(
                    ShortcutEntryActivity.TAG,
                    "Keep screen on Xposed state normalized to inactive. storedDurationIndex="
                            + storedState.getDurationIndex()
                            + ", storedExpiresAt=" + storedState.getExpiresAtElapsedRealtime()
                            + ", nowElapsedRealtime=" + nowElapsedRealtime
            );
            snapshotStore.writeInactivePreservingFreshness(nowElapsedRealtime);
            moduleBridge.sendStop(appContext);
            KeepScreenOnTileRefresher.requestRefresh(appContext);
            YukiDiagnosticsLog.info(
                    ShortcutEntryActivity.TAG,
                    "Keep screen on Xposed state cleared. reason=normalize_expired"
            );
        }
        return normalizedState;
    }

    public void handleToggle(Context context, long nowElapsedRealtime) {
        KeepScreenOnState currentState = reconcileExpiredState(nowElapsedRealtime);
        KeepScreenOnState nextState = interactionModel.handleClick(currentState, nowElapsedRealtime);
        applyState(context, nextState, "toggle");
    }

    public void handleSetInfinite(Context context, long nowElapsedRealtime) {
        reconcileExpiredState(nowElapsedRealtime);
        KeepScreenOnState nextState = interactionModel.handleLongPress(nowElapsedRealtime);
        applyState(context, nextState, "set_infinite");
    }

    public void clearFromModuleCallback(String reason) {
        snapshotStore.writeInactive(SystemClock.elapsedRealtime());
        KeepScreenOnXposedWatcherService.stop(appContext);
        KeepScreenOnTileRefresher.requestRefresh(appContext);
        YukiDiagnosticsLog.info(
                ShortcutEntryActivity.TAG,
                "Keep screen on Xposed state cleared from module callback. reason=" + reason
        );
    }

    private void applyState(Context context, KeepScreenOnState nextState, String reason) {
        if (nextState.isActive()) {
            snapshotStore.writeFromAppState(nextState, SystemClock.elapsedRealtime());
            moduleBridge.sendSync(context, nextState);
            KeepScreenOnXposedWatcherService.start(context);
            KeepScreenOnTileRefresher.requestRefresh(context);
            YukiDiagnosticsLog.info(
                    ShortcutEntryActivity.TAG,
                    "Keep screen on Xposed state applied. reason=" + reason
                            + ", durationIndex=" + nextState.getDurationIndex()
                            + ", expiresAt=" + nextState.getExpiresAtElapsedRealtime()
            );
            return;
        }
        clearAndStop(reason + "_off");
    }

    private void clearAndStop(String reason) {
        snapshotStore.writeInactive(SystemClock.elapsedRealtime());
        moduleBridge.sendStop(appContext);
        KeepScreenOnXposedWatcherService.stop(appContext);
        KeepScreenOnTileRefresher.requestRefresh(appContext);
        YukiDiagnosticsLog.info(ShortcutEntryActivity.TAG, "Keep screen on Xposed state cleared. reason=" + reason);
    }
}
