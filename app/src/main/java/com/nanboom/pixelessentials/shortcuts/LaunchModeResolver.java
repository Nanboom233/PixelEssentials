package com.nanboom.pixelessentials.shortcuts;

import com.nanboom.pixelessentials.ShortcutEntryActivity;

public final class LaunchModeResolver {
    public LaunchMode resolve(
            String action,
            String providedToken,
            String storedToken,
            boolean launcherAliasEnabled
    ) {
        if (ShortcutEntryActivity.ACTION_OPEN_WIRELESS_DEBUGGING.equals(action)
                && storedToken != null
                && storedToken.equals(providedToken)) {
            return LaunchMode.DISPATCH;
        }

        if ("android.intent.action.MAIN".equals(action) && launcherAliasEnabled) {
            return LaunchMode.BOOTSTRAP;
        }

        return LaunchMode.IGNORE;
    }
}
