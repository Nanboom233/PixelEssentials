package top.nanboom233.pixelessentials.shortcuts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

import top.nanboom233.pixelessentials.R;
import top.nanboom233.pixelessentials.ShortcutEntryActivity;

public final class ShortcutPinResultReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ShortcutTokenStore tokenStore = new ShortcutTokenStore(context);
        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        ShortcutPinCompatibilityDecider decider = new ShortcutPinCompatibilityDecider();
        ShortcutInfo shortcutInfo = findPinnedShortcut(shortcutManager);
        String activityClassName = shortcutInfo != null ? resolveShortcutComponentClassName(shortcutInfo) : null;
        ShortcutPinAliasAction action = decider.decide(
                shortcutInfo != null ? shortcutInfo.getId() : null,
                activityClassName
        );

        if (action == ShortcutPinAliasAction.DISABLE) {
            tokenStore.disableLauncherAlias();
            Log.i(ShortcutEntryActivity.TAG, "Pinned shortcut confirmed; launcher alias disabled");
            Toast.makeText(context, context.getString(R.string.shortcut_pinned_success), Toast.LENGTH_LONG).show();
            return;
        }

        if (action == ShortcutPinAliasAction.DISABLE_UNTIL_USED) {
            if (tokenStore.disableLauncherAliasUntilUsed()) {
                Log.i(
                        ShortcutEntryActivity.TAG,
                        "Pinned shortcut confirmed; launcher alias moved to disabled-until-used for compatibility"
                );
                Toast.makeText(
                        context,
                        context.getString(R.string.shortcut_pinned_success_hidden_until_used),
                        Toast.LENGTH_LONG
                ).show();
                return;
            }
            Log.w(
                    ShortcutEntryActivity.TAG,
                    "Pinned shortcut confirmed but launcher alias could not enter disabled-until-used state"
            );
        }

        Log.w(
                ShortcutEntryActivity.TAG,
                "Pinned shortcut confirmed but launcher alias kept enabled for compatibility. shortcutComponent="
                        + activityClassName
        );
        Toast.makeText(context, context.getString(R.string.shortcut_pinned_success_kept_launcher), Toast.LENGTH_LONG)
                .show();
    }

    private String resolveShortcutComponentClassName(ShortcutInfo shortcutInfo) {
        if (shortcutInfo.getIntents() != null && shortcutInfo.getIntents().length > 0) {
            Intent lastIntent = shortcutInfo.getIntents()[shortcutInfo.getIntents().length - 1];
            if (lastIntent.getComponent() != null) {
                return lastIntent.getComponent().getClassName();
            }
        }
        return shortcutInfo.getActivity() != null
                ? shortcutInfo.getActivity().getClassName()
                : null;
    }

    private ShortcutInfo findPinnedShortcut(ShortcutManager shortcutManager) {
        if (shortcutManager == null) {
            return null;
        }
        List<ShortcutInfo> pinnedShortcuts = shortcutManager.getPinnedShortcuts();
        for (ShortcutInfo shortcutInfo : pinnedShortcuts) {
            if (ShortcutTokenStore.SHORTCUT_ID.equals(shortcutInfo.getId())) {
                return shortcutInfo;
            }
        }
        return null;
    }
}
