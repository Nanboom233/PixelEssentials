package top.nanboom233.pixelessentials.keepscreenon;

import android.content.Context;
import android.provider.Settings;

public final class KeepScreenOnSettingsGateway {
    private static final int DEFAULT_SCREEN_OFF_TIMEOUT_MS = 30_000;

    public boolean canWrite(Context context) {
        return Settings.System.canWrite(context);
    }

    public int readScreenOffTimeoutMs(Context context) {
        return Settings.System.getInt(
                context.getContentResolver(),
                Settings.System.SCREEN_OFF_TIMEOUT,
                DEFAULT_SCREEN_OFF_TIMEOUT_MS
        );
    }

    public boolean writeScreenOffTimeoutMs(Context context, int timeoutMs) {
        return Settings.System.putInt(
                context.getContentResolver(),
                Settings.System.SCREEN_OFF_TIMEOUT,
                timeoutMs
        );
    }
}

