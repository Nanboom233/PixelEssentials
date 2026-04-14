package com.nanboom.pixelessentials.shortcuts;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import java.util.UUID;

public final class ShortcutTokenStore {
    public static final String SHORTCUT_ID = "wireless_debugging";
    public static final String EXTRA_SHORTCUT_TOKEN = "com.nanboom.pixelessentials.extra.SHORTCUT_TOKEN";
    public static final String LAUNCHER_ALIAS_NAME = "com.nanboom.pixelessentials.LauncherAlias";

    private static final String PREFS_NAME = "shortcut_prefs";
    private static final String KEY_TOKEN = "shortcut_token";

    private final Context context;
    private final SharedPreferences sharedPreferences;

    public ShortcutTokenStore(Context context) {
        this.context = context.getApplicationContext();
        this.sharedPreferences = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String peekToken() {
        return sharedPreferences.getString(KEY_TOKEN, null);
    }

    public String getOrCreateToken() {
        String existing = peekToken();
        if (existing != null) {
            return existing;
        }
        String generated = UUID.randomUUID().toString();
        sharedPreferences.edit().putString(KEY_TOKEN, generated).apply();
        return generated;
    }

    public boolean isLauncherAliasEnabled() {
        PackageManager packageManager = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, LAUNCHER_ALIAS_NAME);
        int state = packageManager.getComponentEnabledSetting(componentName);
        return state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                || state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    public void disableLauncherAlias() {
        PackageManager packageManager = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, LAUNCHER_ALIAS_NAME);
        packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        );
    }
}
