package top.nanboom233.pixelessentials.wireless;

import top.nanboom233.pixelessentials.root.ExecResult;
import top.nanboom233.pixelessentials.root.RootExecutor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class OpenWirelessDebuggingUseCaseTest {
    @Test
    public void open_delegatesToExecutor() throws Exception {
        RootExecutor executor = command -> new ExecResult(
                command.contains("AdbWirelessDebuggingFragment"),
                0,
                "ok",
                ""
        );

        OpenWirelessDebuggingUseCase useCase = new OpenWirelessDebuggingUseCase(
                new WirelessDebuggingCommandProvider(),
                executor
        );

        ExecResult result = useCase.open();
        assertTrue(result.isSuccess());
        assertEquals("ok", result.getStdout());
    }
}
