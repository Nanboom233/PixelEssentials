package top.nanboom233.pixelessentials.wireless;

public final class WirelessDebuggingCommandProvider {
    private static final String DEFAULT_HOST_ACTIVITY = "com.android.settings/.SubSettings";

    public String getStateCommand() {
        return "settings get global adb_wifi_enabled";
    }

    public String getEnableCommand() {
        return "settings put global adb_wifi_enabled 1";
    }

    public String getDisableCommand() {
        return "settings put global adb_wifi_enabled 0";
    }

    public String getOpenCommand(SettingsLaunchSpec spec) {
        return getOpenCommand(
                spec.getHostActivityClassName(),
                spec.getFragmentClassName(),
                spec.getFragmentArgsKey()
        );
    }

    public String getOpenCommand(String fragmentClassName, String fragmentArgsKey) {
        return getOpenCommand(DEFAULT_HOST_ACTIVITY, fragmentClassName, fragmentArgsKey);
    }

    public String getOpenCommand(
            String hostActivityClassName,
            String fragmentClassName,
            String fragmentArgsKey
    ) {
        String command = "am start -W -f 0x10000000 -n " + hostActivityClassName + " "
                + "--es \":settings:show_fragment\" \"" + fragmentClassName + "\"";
        if (fragmentArgsKey != null && !fragmentArgsKey.isBlank()) {
            command += " --es \":settings:fragment_args_key\" \"" + fragmentArgsKey + "\"";
        }
        return command;
    }
}
