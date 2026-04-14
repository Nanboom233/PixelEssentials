package top.nanboom233.pixelessentials.wireless;

import top.nanboom233.pixelessentials.root.ExecResult;
import top.nanboom233.pixelessentials.root.RootAuthorizationUseCase;
import top.nanboom233.pixelessentials.root.RootExecutor;

import java.io.IOException;

public final class ReadWirelessDebuggingStateUseCase {
    private final RootAuthorizationUseCase rootAuthorizationUseCase;
    private final RootExecutor rootExecutor;
    private final WirelessDebuggingCommandProvider commandProvider;
    private final WirelessDebuggingStateParser stateParser;

    public ReadWirelessDebuggingStateUseCase(
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

    public WirelessDebuggingState read() throws IOException, InterruptedException {
        ExecResult authResult = rootAuthorizationUseCase.ensureRootReady();
        if (!rootAuthorizationUseCase.isAuthorized(authResult)) {
            return new WirelessDebuggingState(false, false, authResult.getStderr());
        }

        ExecResult stateResult = rootExecutor.execute(commandProvider.getStateCommand());
        if (!stateResult.isSuccess()) {
            return new WirelessDebuggingState(true, false, stateResult.getStderr());
        }

        return new WirelessDebuggingState(
                true,
                stateParser.parseEnabled(stateResult.getStdout()),
                stateResult.getStderr()
        );
    }
}
