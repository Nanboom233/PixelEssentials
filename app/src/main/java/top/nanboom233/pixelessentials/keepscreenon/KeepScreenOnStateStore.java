package top.nanboom233.pixelessentials.keepscreenon;

import android.content.Context;
import android.content.SharedPreferences;

public final class KeepScreenOnStateStore {
    private static final String PREFS_NAME = "keep_screen_on_state";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_DURATION_INDEX = "duration_index";
    private static final String KEY_EXPIRES_AT = "expires_at";
    private static final String KEY_LAST_CLICK_AT = "last_click_at";
    private static final String KEY_ORIGINAL_TIMEOUT = "original_timeout_ms";
    private static final String KEY_APPLIED_TIMEOUT = "applied_timeout_ms";

    private final SharedPreferences sharedPreferences;

    public KeepScreenOnStateStore(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public KeepScreenOnState read() {
        return new KeepScreenOnState(
                sharedPreferences.getBoolean(KEY_ACTIVE, false),
                sharedPreferences.getInt(KEY_DURATION_INDEX, -1),
                sharedPreferences.getLong(KEY_EXPIRES_AT, 0L),
                sharedPreferences.getLong(KEY_LAST_CLICK_AT, 0L),
                sharedPreferences.getInt(KEY_ORIGINAL_TIMEOUT, -1),
                sharedPreferences.getInt(KEY_APPLIED_TIMEOUT, -1)
        );
    }

    public void write(KeepScreenOnState state) {
        sharedPreferences.edit()
                .putBoolean(KEY_ACTIVE, state.isActive())
                .putInt(KEY_DURATION_INDEX, state.getDurationIndex())
                .putLong(KEY_EXPIRES_AT, state.getExpiresAtElapsedRealtime())
                .putLong(KEY_LAST_CLICK_AT, state.getLastClickElapsedRealtime())
                .putInt(KEY_ORIGINAL_TIMEOUT, state.getOriginalTimeoutMs())
                .putInt(KEY_APPLIED_TIMEOUT, state.getAppliedTimeoutMs())
                .apply();
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }
}
