package top.nanboom233.pixelessentials;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import top.nanboom233.pixelessentials.shortcuts.PinnedShortcutProxyActivity;
import top.nanboom233.pixelessentials.shortcuts.ShortcutPinResultReceiver;
import top.nanboom233.pixelessentials.shortcuts.ShortcutTokenStore;
import top.nanboom233.pixelessentials.wireless.SettingsLaunchSpec;
import top.nanboom233.pixelessentials.wireless.WirelessDebuggingGateway;

public final class ShortcutEntryActivity extends Activity {
    public static final String TAG = "PixelEssentials";
    public static final String ACTION_OPEN_WIRELESS_DEBUGGING =
            "top.nanboom233.pixelessentials.action.OPEN_WIRELESS_DEBUGGING";

    private volatile boolean dispatched;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (dispatched) {
            finishSilently();
            return;
        }
        dispatched = true;

        if (!Intent.ACTION_MAIN.equals(getIntent() != null ? getIntent().getAction() : null)) {
            Log.w(TAG, "Ignoring non-bootstrap launch for ShortcutEntryActivity");
            finishSilently();
            return;
        }

        ShortcutTokenStore tokenStore = new ShortcutTokenStore(this);
        handleBootstrap(tokenStore);
    }

    private void handleBootstrap(ShortcutTokenStore tokenStore) {
        new Thread(() -> {
            try {
                WirelessDebuggingGateway gateway = new WirelessDebuggingGateway(this);
                WirelessDebuggingGateway.PreparePinnedShortcutResult result =
                        gateway.preparePinnedShortcut();
                if (!result.isSuccess()) {
                    runOnUiThread(() -> {
                        Log.e(
                                TAG,
                                result.isAuthorized()
                                        ? "Wireless debugging discovery failed: " + result.getErrorMessage()
                                        : "Root authorization failed: " + result.getErrorMessage()
                        );
                        Toast.makeText(
                                this,
                                result.isAuthorized()
                                        ? getString(R.string.wireless_debugging_discovery_failed)
                                        : getString(R.string.root_authorization_failed),
                                Toast.LENGTH_LONG
                        ).show();
                        finishCompletely();
                    });
                    return;
                }

                SettingsLaunchSpec spec = result.getLaunchSpec();
                runOnUiThread(() -> {
                    Log.i(
                            TAG,
                            "Root authorization preflight passed. Discovered wireless debugging spec from entry="
                                    + spec.getEntryXmlName()
                                    + ", targetPage=" + spec.getTargetPageXmlName()
                                    + ", fragment=" + spec.getFragmentClassName()
                                    + ", key=" + spec.getFragmentArgsKey()
                    );
                    requestPinnedShortcut(tokenStore);
                });
            } catch (Exception exception) {
                Log.e(TAG, "Wireless debugging bootstrap crashed", exception);
                runOnUiThread(() -> {
                    Toast.makeText(
                            this,
                            getString(R.string.root_authorization_failed),
                            Toast.LENGTH_LONG
                    ).show();
                    finishCompletely();
                });
            }
        }, "root-preflight").start();
    }

    private void requestPinnedShortcut(ShortcutTokenStore tokenStore) {
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
        if (shortcutManager == null || !shortcutManager.isRequestPinShortcutSupported()) {
            Toast.makeText(this, getString(R.string.shortcut_not_supported), Toast.LENGTH_LONG).show();
            finishSilently();
            return;
        }

        String token = tokenStore.getOrCreateToken();
        Intent shortcutIntent = new Intent(this, PinnedShortcutProxyActivity.class)
                .setAction(ACTION_OPEN_WIRELESS_DEBUGGING)
                .putExtra(ShortcutTokenStore.EXTRA_SHORTCUT_TOKEN, token);

        ShortcutInfo shortcutInfo = new ShortcutInfo.Builder(this, ShortcutTokenStore.SHORTCUT_ID)
                .setShortLabel(getString(R.string.shortcut_short_label))
                .setLongLabel(getString(R.string.shortcut_long_label))
                .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut_wireless_debugging))
                .setActivity(new ComponentName(this, PinnedShortcutProxyActivity.class))
                .setIntent(shortcutIntent)
                .build();

        Intent callbackIntent = new Intent(this, ShortcutPinResultReceiver.class);
        PendingIntent callback = PendingIntent.getBroadcast(
                this,
                0,
                callbackIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        boolean requested = shortcutManager.requestPinShortcut(shortcutInfo, callback.getIntentSender());
        if (requested) {
            Log.i(TAG, "Pinned shortcut request submitted");
            Toast.makeText(this, getString(R.string.shortcut_request_sent), Toast.LENGTH_LONG).show();
        } else {
            Log.e(TAG, "Pinned shortcut request failed");
            Toast.makeText(this, getString(R.string.shortcut_request_failed), Toast.LENGTH_LONG).show();
        }
        finishSilently();
    }

    private void finishSilently() {
        finish();
        overridePendingTransition(0, 0);
    }

    private void finishCompletely() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        } else {
            finish();
        }
        overridePendingTransition(0, 0);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }, 300L);
    }
}
