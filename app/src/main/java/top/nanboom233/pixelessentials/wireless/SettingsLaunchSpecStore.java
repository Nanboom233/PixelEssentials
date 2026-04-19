package top.nanboom233.pixelessentials.wireless;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsLaunchSpecStore {
    static final int CURRENT_SCHEMA_VERSION = 3;

    private static final String PREFS_NAME = "wireless_debugging_launch_spec";
    private static final String KEY_HOST_ACTIVITY_CLASS_NAME = "host_activity_class_name";
    private static final String KEY_FRAGMENT_CLASS_NAME = "fragment_class_name";
    private static final String KEY_FRAGMENT_ARGS_KEY = "fragment_args_key";
    private static final String KEY_ENTRY_XML_NAME = "entry_xml_name";
    private static final String KEY_TARGET_PAGE_XML_NAME = "target_page_xml_name";
    private static final String KEY_CONFIDENCE = "confidence";
    private static final String KEY_SETTINGS_VERSION_CODE = "settings_version_code";
    private static final String KEY_SETTINGS_LAST_UPDATE_TIME = "settings_last_update_time";
    private static final String KEY_DISCOVERY_SCHEMA_VERSION = "discovery_schema_version";

    private final SharedPreferences sharedPreferences;
    private final int discoverySchemaVersion;

    public SettingsLaunchSpecStore(Context context) {
        this(
                context == null
                        ? null
                        : context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
                CURRENT_SCHEMA_VERSION
        );
    }

    SettingsLaunchSpecStore(SharedPreferences sharedPreferences, int discoverySchemaVersion) {
        this.sharedPreferences = sharedPreferences;
        this.discoverySchemaVersion = discoverySchemaVersion;
    }

    public SettingsLaunchSpec read(SettingsPackageSnapshot snapshot) {
        if (sharedPreferences == null) {
            return null;
        }
        String fragmentClassName = sharedPreferences.getString(KEY_FRAGMENT_CLASS_NAME, null);
        if (fragmentClassName == null || fragmentClassName.isBlank()) {
            return null;
        }

        long storedVersionCode = sharedPreferences.getLong(KEY_SETTINGS_VERSION_CODE, -1L);
        long storedLastUpdateTime = sharedPreferences.getLong(KEY_SETTINGS_LAST_UPDATE_TIME, -1L);
        if (storedVersionCode != snapshot.getVersionCode()
                || storedLastUpdateTime != snapshot.getLastUpdateTime()) {
            return null;
        }

        int storedSchemaVersion = sharedPreferences.getInt(KEY_DISCOVERY_SCHEMA_VERSION, -1);
        if (storedSchemaVersion != discoverySchemaVersion) {
            return null;
        }

        return new SettingsLaunchSpec(
                sharedPreferences.getString(KEY_HOST_ACTIVITY_CLASS_NAME, "com.android.settings/.SubSettings"),
                fragmentClassName,
                sharedPreferences.getString(KEY_FRAGMENT_ARGS_KEY, null),
                sharedPreferences.getString(KEY_ENTRY_XML_NAME, null),
                sharedPreferences.getString(KEY_TARGET_PAGE_XML_NAME, null),
                sharedPreferences.getInt(KEY_CONFIDENCE, 0),
                storedVersionCode,
                storedLastUpdateTime,
                storedSchemaVersion
        );
    }

    public void write(SettingsLaunchSpec spec) {
        if (sharedPreferences == null) {
            return;
        }
        boolean persisted = sharedPreferences.edit()
                .putString(KEY_HOST_ACTIVITY_CLASS_NAME, spec.getHostActivityClassName())
                .putString(KEY_FRAGMENT_CLASS_NAME, spec.getFragmentClassName())
                .putString(KEY_FRAGMENT_ARGS_KEY, spec.getFragmentArgsKey())
                .putString(KEY_ENTRY_XML_NAME, spec.getEntryXmlName())
                .putString(KEY_TARGET_PAGE_XML_NAME, spec.getTargetPageXmlName())
                .putInt(KEY_CONFIDENCE, spec.getConfidence())
                .putLong(KEY_SETTINGS_VERSION_CODE, spec.getSettingsVersionCode())
                .putLong(KEY_SETTINGS_LAST_UPDATE_TIME, spec.getSettingsLastUpdateTime())
                .putInt(KEY_DISCOVERY_SCHEMA_VERSION, spec.getDiscoverySchemaVersion())
                .commit();
        if (!persisted) {
            throw new IllegalStateException("Failed to persist wireless debugging launch spec");
        }
    }

    public void clear() {
        if (sharedPreferences == null) {
            return;
        }
        boolean cleared = sharedPreferences.edit().clear().commit();
        if (!cleared) {
            throw new IllegalStateException("Failed to clear wireless debugging launch spec");
        }
    }
}
