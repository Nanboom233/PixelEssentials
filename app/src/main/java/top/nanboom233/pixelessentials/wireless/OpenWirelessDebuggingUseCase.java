package top.nanboom233.pixelessentials.wireless;

import top.nanboom233.pixelessentials.root.ExecResult;
import top.nanboom233.pixelessentials.root.RootExecutor;

import java.io.IOException;

public final class OpenWirelessDebuggingUseCase {
    private final WirelessDebuggingCommandProvider commandProvider;
    private final RootExecutor rootExecutor;

    public OpenWirelessDebuggingUseCase(
            WirelessDebuggingCommandProvider commandProvider,
            RootExecutor rootExecutor
    ) {
        this.commandProvider = commandProvider;
        this.rootExecutor = rootExecutor;
    }

    public ExecResult open() throws IOException, InterruptedException {
        return rootExecutor.execute(commandProvider.getOpenCommand());
    }
}
