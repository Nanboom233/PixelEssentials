package com.nanboom.pixelessentials.wireless;

import com.nanboom.pixelessentials.root.ExecResult;
import com.nanboom.pixelessentials.root.RootAuthorizationUseCase;
import com.nanboom.pixelessentials.root.RootExecutor;

import java.io.IOException;

public final class ToggleWirelessDebuggingUseCase {
    private final RootAuthorizationUseCase rootAuthorizationUseCase;
    private final RootExecutor rootExecutor;
    private final WirelessDebuggingCommandProvider commandProvider;
    private final WirelessDebuggingStateParser stateParser;

    public ToggleWirelessDebuggingUseCase(
            RootAuthorizationUseCase rootAuthorizationUseCase,
            RootExecutor rootExecutor,
            WirelessDebuggingCommandProvider commandProvider,
            WirelessDebuggingStateParser stateParser
    ) {
        this.rootAuthorizationUseCase = rootAuthorizationUseCase;
        this.rootExecutor = rootExecutor;
        this.commandProvider = commandProvider;
        this.stateParser = stateParser;
    }

    public WirelessDebuggingState toggle() throws IOException, InterruptedException {
        ExecResult authResult = rootAuthorizationUseCase.ensureRootReady();
        if (!rootAuthorizationUseCase.isAuthorized(authResult)) {
            return new WirelessDebuggingState(false, false, authResult.getStderr());
        }

        ExecResult currentResult = rootExecutor.execute(commandProvider.getStateCommand());
        if (!currentResult.isSuccess()) {
            return new WirelessDebuggingState(true, false, currentResult.getStderr());
        }

        boolean currentlyEnabled = stateParser.parseEnabled(currentResult.getStdout());
        ExecResult toggleResult = rootExecutor.execute(
                currentlyEnabled ? commandProvider.getDisableCommand() : commandProvider.getEnableCommand()
        );
        if (!toggleResult.isSuccess()) {
            return new WirelessDebuggingState(true, currentlyEnabled, toggleResult.getStderr());
        }

        ExecResult verifyResult = rootExecutor.execute(commandProvider.getStateCommand());
        if (!verifyResult.isSuccess()) {
            return new WirelessDebuggingState(true, currentlyEnabled, verifyResult.getStderr());
        }

        return new WirelessDebuggingState(
                true,
                stateParser.parseEnabled(verifyResult.getStdout()),
                verifyResult.getStderr()
        );
    }
}
