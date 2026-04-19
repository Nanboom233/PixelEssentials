package top.nanboom233.pixelessentials.keepscreenon;

import android.content.Context;
import android.os.SystemClock;

import top.nanboom233.pixelessentials.ShortcutEntryActivity;
import top.nanboom233.pixelessentials.logging.YukiDiagnosticsLog;

public final class KeepScreenOnSessionManager {
    private final Context appContext;
    private final KeepScreenOnStateStore stateStore;
    private final KeepScreenOnInteractionModel interactionModel;
    private final KeepScreenOnSettingsGateway settingsGateway;
    private final KeepScreenOnRestorePolicy restorePolicy;
    private final KeepScreenOnAlarmScheduler alarmScheduler;

    public KeepScreenOnSessionManager(Context context) {
        appContext = context.getApplicationContext();
        stateStore = new KeepScreenOnStateStore(appContext);
        interactionModel = new KeepScreenOnInteractionModel();
        settingsGateway = new KeepScreenOnSettingsGateway();
        restorePolicy = new KeepScreenOnRestorePolicy();
        alarmScheduler = new KeepScreenOnAlarmScheduler();
    }

    public boolean hasWriteSettingsPermission(Context context) {
        return settingsGateway.canWrite(context);
    }

    public KeepScreenOnState readState(long nowElapsedRealtime) {
        KeepScreenOnState storedState = stateStore.read();
        KeepScreenOnState state = interactionModel.normalize(storedState, nowElapsedRealtime);
        if (storedState.isActive() && !state.isActive()) {
            YukiDiagnosticsLog.info(
                    ShortcutEntryActivity.TAG,
                    "Keep screen on state normalized to inactive. storedDurationIndex="
                            + storedState.getDurationIndex()
                            + ", storedExpiresAt=" + storedState.getExpiresAtElapsedRealtime()
                            + ", nowElapsedRealtime=" + nowElapsedRealtime
            );
            restoreAndClear(appContext, storedState, "normalize_expired");
            return state;
        }
        if (!state.isActive()) {
            stateStore.clear();
        }
        return state;
    }

    public void applySession(Context context, KeepScreenOnState previousState, KeepScreenOnState nextState) {
        int originalTimeoutMs = previousState.isActive()
                ? previousState.getOriginalTimeoutMs()
                : settingsGateway.readScreenOffTimeoutMs(context);
        int appliedTimeoutMs = previousState.isActive() && previousState.getAppliedTimeoutMs() >= 0
                ? previousState.getAppliedTimeoutMs()
                : restorePolicy.resolveAppliedTimeoutMs(originalTimeoutMs);
        YukiDiagnosticsLog.info(
                ShortcutEntryActivity.TAG,
                "Applying keep screen on session. previousActive=" + previousState.isActive()
                        + ", previousDurationIndex=" + previousState.getDurationIndex()
                        + ", nextDurationIndex=" + nextState.getDurationIndex()
                        + ", originalTimeoutMs=" + originalTimeoutMs
                        + ", appliedTimeoutMs=" + appliedTimeoutMs
        );

        if (!settingsGateway.writeScreenOffTimeoutMs(context, appliedTimeoutMs)) {
            YukiDiagnosticsLog.error(ShortcutEntryActivity.TAG, "Failed to write screen_off_timeout");
            return;
        }

        KeepScreenOnState persistedState = nextState.withTimeouts(originalTimeoutMs, appliedTimeoutMs);
        stateStore.write(persistedState);
        alarmScheduler.schedule(context, persistedState, SystemClock.elapsedRealtime());
        KeepScreenOnTileRefresher.requestRefresh(context);
        YukiDiagnosticsLog.info(
                ShortcutEntryActivity.TAG,
                "Keep screen on session applied. durationIndex=" + persistedState.getDurationIndex()
                        + ", expiresAt=" + persistedState.getExpiresAtElapsedRealtime()
                        + ", originalTimeout=" + persistedState.getOriginalTimeoutMs()
                        + ", appliedTimeout=" + persistedState.getAppliedTimeoutMs()
        );
    }

    public void restoreAndClear(Context context, String reason) {
        restoreAndClear(context, stateStore.read(), reason);
    }

    public void restoreAndClear(Context context, KeepScreenOnState state, String reason) {
        boolean restoredTimeout = false;
        int currentTimeoutMs = -1;
        if (state.isActive() && settingsGateway.canWrite(context)) {
            currentTimeoutMs = settingsGateway.readScreenOffTimeoutMs(context);
            if (restorePolicy.shouldRestoreTimeout(state, currentTimeoutMs)) {
                settingsGateway.writeScreenOffTimeoutMs(context, state.getOriginalTimeoutMs());
                restoredTimeout = true;
            }
        }

        stateStore.clear();
        alarmScheduler.cancel(context);
        KeepScreenOnTileRefresher.requestRefresh(context);
        YukiDiagnosticsLog.info(
                ShortcutEntryActivity.TAG,
                "Keep screen on session cleared. reason=" + reason
                        + ", restoredTimeout=" + restoredTimeout
                        + ", currentTimeoutMs=" + currentTimeoutMs
                        + ", originalTimeoutMs=" + state.getOriginalTimeoutMs()
                        + ", appliedTimeoutMs=" + state.getAppliedTimeoutMs()
        );
    }
}
