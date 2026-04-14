package top.nanboom233.pixelessentials.wireless;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public final class WirelessDebuggingCommandProviderTest {
    @Test
    public void getOpenCommand_containsExpectedTargets() {
        String command = new WirelessDebuggingCommandProvider().getOpenCommand();
        assertTrue(command.contains("com.android.settings/.SubSettings"));
        assertTrue(command.contains("com.android.settings.development.AdbWirelessDebuggingFragment"));
    }
}
