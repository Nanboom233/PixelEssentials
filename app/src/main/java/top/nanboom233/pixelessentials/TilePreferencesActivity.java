package top.nanboom233.pixelessentials;

import android.app.Activity;
import android.os.Bundle;

public final class TilePreferencesActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (new KeepScreenOnTilePreferencesHandler().handle(this, getIntent())) {
            return;
        }

        new WirelessDebuggingTilePreferencesHandler().handle(this);
    }

    void finishSilently() {
        finish();
        overridePendingTransition(0, 0);
    }
}
