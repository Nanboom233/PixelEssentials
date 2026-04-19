package top.nanboom233.pixelessentials.keepscreenon;

import android.content.ComponentName;
import android.content.Context;
import android.service.quicksettings.TileService;

import top.nanboom233.pixelessentials.KeepScreenOnTileService;

public final class KeepScreenOnTileRefresher {
    private KeepScreenOnTileRefresher() {
    }

    public static void requestRefresh(Context context) {
        TileService.requestListeningState(context, new ComponentName(context, KeepScreenOnTileService.class));
    }
}
