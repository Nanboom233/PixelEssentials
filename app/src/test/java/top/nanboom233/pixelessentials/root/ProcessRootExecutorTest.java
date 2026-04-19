package top.nanboom233.pixelessentials.root;

import top.nanboom233.pixelessentials.wireless.WirelessDebuggingCommandProvider;
import top.nanboom233.pixelessentials.wireless.SettingsLaunchSpec;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ProcessRootExecutorTest {
    private static final int TEST_DISCOVERY_SCHEMA_VERSION = 2;
    private static final String SETTINGS_HOST = "com.android.settings/.SubSettings";

    @Test
    public void isAllowedCommand_acceptsKnownWirelessDebuggingCommands() {
        WirelessDebuggingCommandProvider provider = new WirelessDebuggingCommandProvider();

        assertTrue(ProcessRootExecutor.isAllowedCommand("id"));
        assertTrue(ProcessRootExecutor.isAllowedCommand("settings get global adb_wifi_enabled"));
        assertTrue(ProcessRootExecutor.isAllowedCommand("settings put global adb_wifi_enabled 1"));
        assertTrue(ProcessRootExecutor.isAllowedCommand("settings put global adb_wifi_enabled 0"));
        assertTrue(ProcessRootExecutor.isAllowedCommand("logcat -c"));
        assertTrue(
                ProcessRootExecutor.isAllowedCommand(
                        provider.getOpenCommand(
                                new SettingsLaunchSpec(
                                        SETTINGS_HOST,
                                        "com.android.settings.development.WirelessDebuggingFragment",
                                        "adb_wireless_settings",
                                        "development_settings",
                                        "adb_wireless_settings",
                                        200,
                                        1L,
                                        2L,
                                        TEST_DISCOVERY_SCHEMA_VERSION
                                )
                        )
                )
        );
        assertTrue(
                ProcessRootExecutor.isAllowedCommand(
                        provider.getOpenCommand(
                                new SettingsLaunchSpec(
                                        SETTINGS_HOST,
                                        "com.android.settings.development.WirelessDebuggingFragment",
                                        "toggle_adb_wireless",
                                        "development_settings",
                                        "adb_wireless_settings",
                                        230,
                                        1L,
                                        2L,
                                        TEST_DISCOVERY_SCHEMA_VERSION
                                )
                        )
                )
        );
    }

    @Test
    public void isAllowedCommand_rejectsInjectedWirelessDebuggingCommandShape() {
        assertFalse(
                ProcessRootExecutor.isAllowedCommand(
                        "am start -W -f 0x10000000 -n com.android.settings/.SubSettings "
                                + "--es \":settings:show_fragment\" "
                                + "\"com.android.settings.development.WirelessDebuggingFragment\" "
                                + "--es \":settings:fragment_args_key\" \"toggle_adb_wireless\\\" --user 0\""
                )
        );
    }

    @Test
    public void execute_rejectsNonWhitelistedCommandWithoutRunningIt() throws Exception {
        ProcessRootExecutor executor = new ProcessRootExecutor();

        ExecResult result = executor.execute("sh -c whoami");

        assertFalse(result.isSuccess());
        assertEquals(-1, result.getExitCode());
        assertEquals("", result.getStdout());
        assertEquals("Rejected non-whitelisted root command", result.getStderr());
    }
}
