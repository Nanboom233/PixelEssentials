package com.nanboom.pixelessentials.wireless;

import com.nanboom.pixelessentials.root.ExecResult;
import com.nanboom.pixelessentials.root.RootExecutor;

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
