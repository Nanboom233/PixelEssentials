package top.nanboom233.pixelessentials.wireless;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class WirelessDebuggingCommandProviderTest {
    private static final String SETTINGS_HOST = "com.android.settings/.SubSettings";

    @Test
    public void getOpenCommand_containsExpectedSettingsLaunchSpecTarget() {
        SettingsLaunchSpec spec = new SettingsLaunchSpec(
                SETTINGS_HOST,
                "com.android.settings.development.WirelessDebuggingFragment",
                "adb_wireless_settings",
                "development_settings",
                "adb_wireless_settings",
                200,
                1L,
                2L,
                SettingsLaunchSpecStore.CURRENT_SCHEMA_VERSION
        );
        String command = new WirelessDebuggingCommandProvider().getOpenCommand(spec);
        assertTrue(command.contains(SETTINGS_HOST));
        assertTrue(command.contains("com.android.settings.development.WirelessDebuggingFragment"));
        assertTrue(command.contains(":settings:fragment_args_key"));
        assertTrue(command.contains("adb_wireless_settings"));
    }

    @Test
    public void getOpenCommand_withoutArgsKey_omitsFragmentArgsExtra() {
        String command = new WirelessDebuggingCommandProvider().getOpenCommand(
                "com.android.settings.development.WirelessDebuggingFragment",
                null
        );
        assertTrue(command.contains("com.android.settings.development.WirelessDebuggingFragment"));
        assertFalse(command.contains(":settings:fragment_args_key"));
    }

    @Test
    public void getStateCommand_returnsExpectedCommand() {
        WirelessDebuggingCommandProvider provider = new WirelessDebuggingCommandProvider();
        assertEquals("settings get global adb_wifi_enabled", provider.getStateCommand());
    }

    @Test
    public void getEnableDisableCommands_returnExpectedCommands() {
        WirelessDebuggingCommandProvider provider = new WirelessDebuggingCommandProvider();
        assertEquals("settings put global adb_wifi_enabled 1", provider.getEnableCommand());
        assertEquals("settings put global adb_wifi_enabled 0", provider.getDisableCommand());
    }
}
