package top.nanboom233.pixelessentials;

import android.util.Log;
import android.widget.Toast;

import top.nanboom233.pixelessentials.root.ExecResult;
import top.nanboom233.pixelessentials.wireless.WirelessDebuggingGateway;

public final class WirelessDebuggingTilePreferencesHandler {
    public void handle(TilePreferencesActivity activity) {
        new Thread(() -> {
            try {
                WirelessDebuggingGateway gateway = new WirelessDebuggingGateway(activity);
                ExecResult result = gateway.open();
                activity.runOnUiThread(() -> {
                    Log.i(
                            ShortcutEntryActivity.TAG,
                            "QS tile preferences requested open. exitCode=" + result.getExitCode()
                                    + ", stdout=" + result.getStdout()
                                    + ", stderr=" + result.getStderr()
                    );
                    if (!result.isSuccess()) {
                        Log.e(ShortcutEntryActivity.TAG, "Tile preferences open failed: " + result.getStderr());
                        Toast.makeText(activity, activity.getString(R.string.root_command_failed), Toast.LENGTH_SHORT).show();
                    }
                    activity.finishSilently();
                });
            } catch (Exception exception) {
                Log.e(ShortcutEntryActivity.TAG, "Tile preferences open crashed", exception);
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, activity.getString(R.string.root_command_failed), Toast.LENGTH_SHORT).show();
                    activity.finishSilently();
                });
            }
        }, "tile-preferences-open").start();
    }
}
