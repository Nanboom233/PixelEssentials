package top.nanboom233.pixelessentials;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import top.nanboom233.pixelessentials.keepscreenon.KeepScreenOnTileRefresher;
import top.nanboom233.pixelessentials.logging.YukiDiagnosticsLog;

public final class WriteSettingsGrantActivity extends Activity {
    private static final long[] REFRESH_BURST_DELAYS_MS = {300L, 1_000L, 2_000L};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        YukiDiagnosticsLog.info(
                ShortcutEntryActivity.TAG,
                "WriteSettingsGrantActivity launching MANAGE_WRITE_SETTINGS"
        );
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                .setData(Uri.parse("package:" + getPackageName()))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        requestRefreshBurst();
        finish();
    }

    private void requestRefreshBurst() {
        Context appContext = getApplicationContext();
        Handler handler = new Handler(Looper.getMainLooper());
        for (long delayMs : REFRESH_BURST_DELAYS_MS) {
            handler.postDelayed(() -> KeepScreenOnTileRefresher.requestRefresh(appContext), delayMs);
        }
    }
}
