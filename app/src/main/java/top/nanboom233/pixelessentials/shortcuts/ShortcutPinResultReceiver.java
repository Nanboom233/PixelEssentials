package top.nanboom233.pixelessentials.shortcuts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import top.nanboom233.pixelessentials.R;
import top.nanboom233.pixelessentials.ShortcutEntryActivity;

public final class ShortcutPinResultReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ShortcutTokenStore tokenStore = new ShortcutTokenStore(context);
        tokenStore.disableLauncherAlias();
        Log.i(ShortcutEntryActivity.TAG, "Pinned shortcut confirmed; launcher alias disabled");
        Toast.makeText(context, context.getString(R.string.shortcut_pinned_success), Toast.LENGTH_LONG).show();
    }
}
