package com.nanboom.pixelessentials.wireless;

public final class WirelessDebuggingCommandProvider {
    public String getStateCommand() {
        return "settings get global adb_wifi_enabled";
    }

    public String getEnableCommand() {
        return "settings put global adb_wifi_enabled 1";
    }

    public String getDisableCommand() {
        return "settings put global adb_wifi_enabled 0";
    }

    public String getOpenCommand() {
        return "am start -W -f 0x10000000 -n com.android.settings/.SubSettings --es \":settings:show_fragment\" "
                + "\"com.android.settings.development.AdbWirelessDebuggingFragment\"";
    }
}
