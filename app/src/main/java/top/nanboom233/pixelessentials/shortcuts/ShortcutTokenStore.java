package top.nanboom233.pixelessentials.shortcuts;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import java.util.UUID;

public final class ShortcutTokenStore {
    public static final String SHORTCUT_ID = "wireless_debugging";
    public static final String EXTRA_SHORTCUT_TOKEN = "top.nanboom233.pixelessentials.extra.SHORTCUT_TOKEN";
    public static final String LAUNCHER_ALIAS_NAME = "top.nanboom233.pixelessentials.LauncherAlias";
    public static final String PINNED_SHORTCUT_PROXY_ACTIVITY_NAME =
            "top.nanboom233.pixelessentials.shortcuts.PinnedShortcutProxyActivity";

    private static final String PREFS_NAME = "shortcut_prefs";
    private static final String KEY_TOKEN = "shortcut_token";

    private final Context context;
    private final SharedPreferences sharedPreferences;

    public ShortcutTokenStore(Context context) {
        this(
                context.getApplicationContext(),
                context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        );
    }

    ShortcutTokenStore(Context context, SharedPreferences sharedPreferences) {
        this.context = context;
        this.sharedPreferences = sharedPreferences;
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
        boolean persisted = sharedPreferences.edit().putString(KEY_TOKEN, generated).commit();
        if (!persisted) {
            throw new IllegalStateException("Failed to persist shortcut token");
        }
        return generated;
    }

    public boolean isValidToken(String providedToken) {
        String storedToken = peekToken();
        return storedToken != null && storedToken.equals(providedToken);
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

    public boolean disableLauncherAliasUntilUsed() {
        PackageManager packageManager = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, LAUNCHER_ALIAS_NAME);
        try {
            packageManager.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED,
                    PackageManager.DONT_KILL_APP
            );
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
