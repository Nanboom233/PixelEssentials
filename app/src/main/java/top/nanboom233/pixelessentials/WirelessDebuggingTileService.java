package top.nanboom233.pixelessentials;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.widget.Toast;

import top.nanboom233.pixelessentials.wireless.WirelessDebuggingGateway;
import top.nanboom233.pixelessentials.wireless.WirelessDebuggingState;

public final class WirelessDebuggingTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        new Thread(this::refreshTileState, "qs-wireless-state").start();
    }

    @Override
    public void onClick() {
        super.onClick();
        unlockAndRun(() -> new Thread(this::runToggle, "qs-wireless-toggle").start());
    }

    private void refreshTileState() {
        try {
            WirelessDebuggingGateway gateway = new WirelessDebuggingGateway(this);
            WirelessDebuggingState state = gateway.readState();
            Log.i(
                    ShortcutEntryActivity.TAG,
                    "QS tile state refreshed. authorized=" + state.isAuthorized()
                            + ", enabled=" + state.isEnabled()
                            + ", error=" + state.getErrorMessage()
            );
            updateTile(state);
        } catch (Exception exception) {
            Log.e(ShortcutEntryActivity.TAG, "QS tile state refresh crashed", exception);
            updateTile(new WirelessDebuggingState(false, false, exception.getMessage()));
        }
    }

    private void runToggle() {
        try {
            WirelessDebuggingGateway gateway = new WirelessDebuggingGateway(this);
            WirelessDebuggingState state = gateway.toggleState();
            updateTile(state);
            if (!state.isAuthorized()) {
                Log.e(ShortcutEntryActivity.TAG, "QS tile toggle denied: " + state.getErrorMessage());
                showToast(getString(R.string.root_command_failed));
                return;
            }

            Log.i(
                    ShortcutEntryActivity.TAG,
                    "QS tile toggled wireless debugging. enabled=" + state.isEnabled()
                            + ", error=" + state.getErrorMessage()
            );
        } catch (Exception exception) {
            Log.e(ShortcutEntryActivity.TAG, "QS tile toggle crashed", exception);
            showToast(getString(R.string.root_command_failed));
        }
    }

    private void updateTile(WirelessDebuggingState state) {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }

        tile.setLabel(getString(R.string.tile_label));
        tile.setIcon(android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_tile_wireless_debugging));

        if (!state.isAuthorized()) {
            tile.setState(Tile.STATE_UNAVAILABLE);
            tile.setSubtitle(getString(R.string.tile_unavailable_subtitle));
        } else if (state.isEnabled()) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.setSubtitle(getString(R.string.tile_enabled_subtitle));
        } else {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setSubtitle(getString(R.string.tile_disabled_subtitle));
        }

        tile.updateTile();
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
