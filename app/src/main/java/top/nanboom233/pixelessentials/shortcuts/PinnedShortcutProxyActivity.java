package top.nanboom233.pixelessentials.shortcuts;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import top.nanboom233.pixelessentials.R;
import top.nanboom233.pixelessentials.ShortcutEntryActivity;
import top.nanboom233.pixelessentials.root.ExecResult;
import top.nanboom233.pixelessentials.wireless.WirelessDebuggingGateway;

public final class PinnedShortcutProxyActivity extends Activity {
    private volatile boolean dispatched;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (dispatched) {
            finishSilently();
            return;
        }
        dispatched = true;

        if (!ShortcutEntryActivity.ACTION_OPEN_WIRELESS_DEBUGGING.equals(
                getIntent() != null ? getIntent().getAction() : null
        )) {
            Log.w(ShortcutEntryActivity.TAG, "Ignoring unsupported shortcut action");
            finishSilently();
            return;
        }

        ShortcutTokenStore tokenStore = new ShortcutTokenStore(this);
        String providedToken = getIntent() != null
                ? getIntent().getStringExtra(ShortcutTokenStore.EXTRA_SHORTCUT_TOKEN)
                : null;
        if (!tokenStore.isValidToken(providedToken)) {
            Log.w(ShortcutEntryActivity.TAG, "Ignoring untrusted shortcut launch");
            finishSilently();
            return;
        }

        handleDispatch();
    }

    private void handleDispatch() {
        new Thread(() -> {
            try {
                WirelessDebuggingGateway gateway = new WirelessDebuggingGateway(this);
                Log.i(ShortcutEntryActivity.TAG, "Dispatching pinned shortcut root command");
                ExecResult result = gateway.open();
                runOnUiThread(() -> {
                    Log.i(
                            ShortcutEntryActivity.TAG,
                            "Pinned shortcut command completed. exitCode=" + result.getExitCode()
                                    + ", stdout=" + result.getStdout()
                                    + ", stderr=" + result.getStderr()
                    );
                    if (!result.isSuccess()) {
                        Log.e(ShortcutEntryActivity.TAG, "Pinned shortcut command failed: " + result.getStderr());
                        Toast.makeText(
                                this,
                                getString(R.string.root_command_failed),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                    finishSilently();
                });
            } catch (Exception exception) {
                Log.e(ShortcutEntryActivity.TAG, "Pinned shortcut dispatch crashed", exception);
                runOnUiThread(() -> {
                    Toast.makeText(
                            this,
                            getString(R.string.root_command_failed),
                            Toast.LENGTH_SHORT
                    ).show();
                    finishSilently();
                });
            }
        }, "pinned-shortcut-dispatch").start();
    }

    private void finishSilently() {
        finish();
        overridePendingTransition(0, 0);
    }
}
