package com.nanboom.pixelessentials;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.nanboom.pixelessentials.root.ExecResult;
import com.nanboom.pixelessentials.root.ProcessRootExecutor;
import com.nanboom.pixelessentials.root.RootAuthorizationUseCase;
import com.nanboom.pixelessentials.shortcuts.LaunchMode;
import com.nanboom.pixelessentials.shortcuts.LaunchModeResolver;
import com.nanboom.pixelessentials.shortcuts.ShortcutPinResultReceiver;
import com.nanboom.pixelessentials.shortcuts.ShortcutTokenStore;
import com.nanboom.pixelessentials.wireless.AuthorizedOpenWirelessDebuggingUseCase;
import com.nanboom.pixelessentials.wireless.OpenWirelessDebuggingUseCase;
import com.nanboom.pixelessentials.wireless.WirelessDebuggingCommandProvider;

public final class ShortcutEntryActivity extends Activity {
    public static final String TAG = "PixelEssentials";
    public static final String ACTION_OPEN_WIRELESS_DEBUGGING =
            "com.nanboom.pixelessentials.action.OPEN_WIRELESS_DEBUGGING";

    private volatile boolean dispatched;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (dispatched) {
            finishSilently();
            return;
        }
        dispatched = true;

        ShortcutTokenStore tokenStore = new ShortcutTokenStore(this);
        LaunchMode launchMode = new LaunchModeResolver().resolve(
                getIntent() != null ? getIntent().getAction() : null,
                getIntent() != null ? getIntent().getStringExtra(ShortcutTokenStore.EXTRA_SHORTCUT_TOKEN) : null,
                tokenStore.peekToken(),
                tokenStore.isLauncherAliasEnabled()
        );

        if (launchMode == LaunchMode.DISPATCH) {
            handleDispatch();
            return;
        }

        if (launchMode == LaunchMode.BOOTSTRAP) {
            handleBootstrap(tokenStore);
            return;
        }

        Log.w(TAG, "Ignoring untrusted or unsupported launch intent");
        finishSilently();
    }

    private void handleBootstrap(ShortcutTokenStore tokenStore) {
        new Thread(() -> {
            try {
                RootAuthorizationUseCase rootAuthorizationUseCase =
                        new RootAuthorizationUseCase(new ProcessRootExecutor());
                ExecResult rootResult = rootAuthorizationUseCase.ensureRootReady();
                runOnUiThread(() -> {
                    if (!rootAuthorizationUseCase.isAuthorized(rootResult)) {
                        Log.e(TAG, "Root authorization failed: " + rootResult.getStderr());
                        Toast.makeText(
                                this,
                                getString(R.string.root_authorization_failed),
                                Toast.LENGTH_LONG
                        ).show();
                        finishSilently();
                        return;
                    }

                    Log.i(TAG, "Root authorization preflight passed");
                    requestPinnedShortcut(tokenStore);
                });
            } catch (Exception exception) {
                Log.e(TAG, "Root authorization check crashed", exception);
                runOnUiThread(() -> {
                    Toast.makeText(
                            this,
                            getString(R.string.root_authorization_failed),
                            Toast.LENGTH_LONG
                    ).show();
                    finishSilently();
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
        Intent shortcutIntent = new Intent(this, ShortcutEntryActivity.class)
                .setAction(ACTION_OPEN_WIRELESS_DEBUGGING)
                .putExtra(ShortcutTokenStore.EXTRA_SHORTCUT_TOKEN, token);

        ShortcutInfo shortcutInfo = new ShortcutInfo.Builder(this, ShortcutTokenStore.SHORTCUT_ID)
                .setShortLabel(getString(R.string.shortcut_short_label))
                .setLongLabel(getString(R.string.shortcut_long_label))
                .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut_wireless_debugging))
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

    private void handleDispatch() {
        AuthorizedOpenWirelessDebuggingUseCase useCase = new AuthorizedOpenWirelessDebuggingUseCase(
                new RootAuthorizationUseCase(new ProcessRootExecutor()),
                new OpenWirelessDebuggingUseCase(
                        new WirelessDebuggingCommandProvider(),
                        new ProcessRootExecutor()
                )
        );

        new Thread(() -> {
            try {
                Log.i(TAG, "Dispatching root command for wireless debugging");
                ExecResult result = useCase.open();
                runOnUiThread(() -> {
                    Log.i(
                            TAG,
                            "Root command completed. exitCode=" + result.getExitCode()
                                    + ", stdout=" + result.getStdout()
                                    + ", stderr=" + result.getStderr()
                    );
                    if (!result.isSuccess()) {
                        Log.e(TAG, "Root command failed: " + result.getStderr());
                        Toast.makeText(
                                this,
                                getString(R.string.root_command_failed),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                    finishSilently();
                });
            } catch (Exception exception) {
                Log.e(TAG, "Unable to open wireless debugging", exception);
                runOnUiThread(() -> {
                    Toast.makeText(
                            this,
                            getString(R.string.root_command_failed),
                            Toast.LENGTH_SHORT
                    ).show();
                    finishSilently();
                });
            }
        }, "wireless-debug-dispatch").start();
    }

    private void finishSilently() {
        finish();
        overridePendingTransition(0, 0);
    }
}
