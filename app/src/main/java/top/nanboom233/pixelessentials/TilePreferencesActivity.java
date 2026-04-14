package top.nanboom233.pixelessentials;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import top.nanboom233.pixelessentials.root.ExecResult;
import top.nanboom233.pixelessentials.root.ProcessRootExecutor;
import top.nanboom233.pixelessentials.root.RootAuthorizationUseCase;
import top.nanboom233.pixelessentials.wireless.AuthorizedOpenWirelessDebuggingUseCase;
import top.nanboom233.pixelessentials.wireless.OpenWirelessDebuggingUseCase;
import top.nanboom233.pixelessentials.wireless.WirelessDebuggingCommandProvider;

public final class TilePreferencesActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Thread(() -> {
            try {
                AuthorizedOpenWirelessDebuggingUseCase useCase = new AuthorizedOpenWirelessDebuggingUseCase(
                        new RootAuthorizationUseCase(new ProcessRootExecutor()),
                        new OpenWirelessDebuggingUseCase(
                                new WirelessDebuggingCommandProvider(),
                                new ProcessRootExecutor()
                        )
                );
                ExecResult result = useCase.open();
                runOnUiThread(() -> {
                    Log.i(
                            ShortcutEntryActivity.TAG,
                            "QS tile preferences requested open. exitCode=" + result.getExitCode()
                                    + ", stdout=" + result.getStdout()
                                    + ", stderr=" + result.getStderr()
                    );
                    if (!result.isSuccess()) {
                        Log.e(ShortcutEntryActivity.TAG, "Tile preferences open failed: " + result.getStderr());
                        Toast.makeText(this, getString(R.string.root_command_failed), Toast.LENGTH_SHORT).show();
                    }
                    finishSilently();
                });
            } catch (Exception exception) {
                Log.e(ShortcutEntryActivity.TAG, "Tile preferences open crashed", exception);
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.root_command_failed), Toast.LENGTH_SHORT).show();
                    finishSilently();
                });
            }
        }, "tile-preferences-open").start();
    }

    private void finishSilently() {
        finish();
        overridePendingTransition(0, 0);
    }
}
