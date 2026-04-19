package top.nanboom233.pixelessentials.keepscreenon;

import android.content.Context;
import android.content.SharedPreferences;

public final class KeepScreenOnXposedRouteSnapshotStore {
    private static final String PREFS_NAME = "keep_screen_on_xposed_route_snapshot";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_DURATION_INDEX = "duration_index";
    private static final String KEY_EXPIRES_AT = "expires_at";
    private static final String KEY_LAST_CLICK_AT = "last_click_at";
    private static final String KEY_LAST_STATUS_CALLBACK_AT = "last_status_callback_at";

    private final SharedPreferences sharedPreferences;

    public KeepScreenOnXposedRouteSnapshotStore(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public KeepScreenOnXposedRouteSnapshot read() {
        KeepScreenOnState state = new KeepScreenOnState(
                sharedPreferences.getBoolean(KEY_ACTIVE, false),
                sharedPreferences.getInt(KEY_DURATION_INDEX, -1),
                sharedPreferences.getLong(KEY_EXPIRES_AT, 0L),
                sharedPreferences.getLong(KEY_LAST_CLICK_AT, 0L)
        );
        long lastStatusCallbackElapsedRealtime = sharedPreferences.getLong(KEY_LAST_STATUS_CALLBACK_AT, 0L);
        return new KeepScreenOnXposedRouteSnapshot(state, lastStatusCallbackElapsedRealtime);
    }

    public void writeFromAppState(KeepScreenOnState state, long confirmedAtElapsedRealtime) {
        write(state, confirmedAtElapsedRealtime);
    }

    public void updateFromStatusCallback(
            boolean active,
            int durationIndex,
            long expiresAtElapsedRealtime,
            long callbackAtElapsedRealtime
    ) {
        KeepScreenOnXposedRouteSnapshot currentSnapshot = read();
        long lastClickElapsedRealtime = currentSnapshot.getState().getLastClickElapsedRealtime();
        KeepScreenOnState nextState = active
                ? new KeepScreenOnState(active, durationIndex, expiresAtElapsedRealtime, lastClickElapsedRealtime)
                : KeepScreenOnState.inactive(callbackAtElapsedRealtime);
        write(nextState, callbackAtElapsedRealtime);
    }

    public void writeInactive(long confirmedAtElapsedRealtime) {
        write(KeepScreenOnState.inactive(confirmedAtElapsedRealtime), confirmedAtElapsedRealtime);
    }

    public void writeInactivePreservingFreshness(long nowElapsedRealtime) {
        long lastStatusCallbackElapsedRealtime = read().getLastStatusCallbackElapsedRealtime();
        write(KeepScreenOnState.inactive(nowElapsedRealtime), lastStatusCallbackElapsedRealtime);
    }

    private void write(KeepScreenOnState state, long lastStatusCallbackElapsedRealtime) {
        sharedPreferences.edit()
                .putBoolean(KEY_ACTIVE, state.isActive())
                .putInt(KEY_DURATION_INDEX, state.getDurationIndex())
                .putLong(KEY_EXPIRES_AT, state.getExpiresAtElapsedRealtime())
                .putLong(KEY_LAST_CLICK_AT, state.getLastClickElapsedRealtime())
                .putLong(KEY_LAST_STATUS_CALLBACK_AT, lastStatusCallbackElapsedRealtime)
                .apply();
    }
}
