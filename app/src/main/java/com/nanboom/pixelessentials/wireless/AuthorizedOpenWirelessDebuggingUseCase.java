package com.nanboom.pixelessentials.wireless;

import com.nanboom.pixelessentials.root.ExecResult;
import com.nanboom.pixelessentials.root.RootAuthorizationUseCase;

import java.io.IOException;

public final class AuthorizedOpenWirelessDebuggingUseCase {
    private final RootAuthorizationUseCase rootAuthorizationUseCase;
    private final OpenWirelessDebuggingUseCase openWirelessDebuggingUseCase;

    public AuthorizedOpenWirelessDebuggingUseCase(
            RootAuthorizationUseCase rootAuthorizationUseCase,
            OpenWirelessDebuggingUseCase openWirelessDebuggingUseCase
    ) {
        this.rootAuthorizationUseCase = rootAuthorizationUseCase;
        this.openWirelessDebuggingUseCase = openWirelessDebuggingUseCase;
    }

    public ExecResult open() throws IOException, InterruptedException {
        ExecResult authResult = rootAuthorizationUseCase.ensureRootReady();
        if (!rootAuthorizationUseCase.isAuthorized(authResult)) {
            return authResult;
        }
        return openWirelessDebuggingUseCase.open();
    }
}
