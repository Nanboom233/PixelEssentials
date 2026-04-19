package top.nanboom233.pixelessentials.wireless;

import android.content.Context;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import top.nanboom233.pixelessentials.root.ExecResult;
import top.nanboom233.pixelessentials.root.ProcessRootExecutor;
import top.nanboom233.pixelessentials.root.RootAuthorizationUseCase;
import top.nanboom233.pixelessentials.root.RootExecutor;

public final class WirelessDebuggingGateway {
    private final RootAuthorizationUseCase rootAuthorizationUseCase;
    private final RootExecutor rootExecutor;
    private final WirelessDebuggingCommandProvider commandProvider;
    private final WirelessDebuggingStateParser stateParser;
    private final SettingsLaunchSpecDiscoveryUseCase discoveryUseCase;
    private final SettingsLaunchSpecStore launchSpecStore;

    public WirelessDebuggingGateway(Context context) {
        ProcessRootExecutor sharedExecutor = new ProcessRootExecutor();
        this.rootAuthorizationUseCase = new RootAuthorizationUseCase(sharedExecutor);
        this.rootExecutor = sharedExecutor;
        this.commandProvider = new WirelessDebuggingCommandProvider();
        this.stateParser = new WirelessDebuggingStateParser();
        this.discoveryUseCase = new SettingsLaunchSpecDiscoveryUseCase(context);
        this.launchSpecStore = new SettingsLaunchSpecStore(context);
    }

    WirelessDebuggingGateway(
            RootAuthorizationUseCase rootAuthorizationUseCase,
            RootExecutor rootExecutor,
            WirelessDebuggingCommandProvider commandProvider,
            WirelessDebuggingStateParser stateParser,
            SettingsLaunchSpecDiscoveryUseCase discoveryUseCase,
            SettingsLaunchSpecStore launchSpecStore
    ) {
        this.rootAuthorizationUseCase = rootAuthorizationUseCase;
        this.rootExecutor = rootExecutor;
        this.commandProvider = commandProvider;
        this.stateParser = stateParser;
        this.discoveryUseCase = discoveryUseCase;
        this.launchSpecStore = launchSpecStore;
    }

    public PreparePinnedShortcutResult preparePinnedShortcut() throws IOException, InterruptedException {
        ExecResult authResult = rootAuthorizationUseCase.ensureRootReady();
        if (!rootAuthorizationUseCase.isAuthorized(authResult)) {
            return PreparePinnedShortcutResult.rootDenied(authResult);
        }

        try {
            SettingsPackageSnapshot snapshot = discoveryUseCase.readSettingsPackageSnapshot();
            SettingsLaunchSpec spec = discoveryUseCase.discover(snapshot);
            if (spec == null) {
                return PreparePinnedShortcutResult.discoveryFailed(
                        "Failed to discover wireless debugging launch spec"
                );
            }
            launchSpecStore.write(spec);
            return PreparePinnedShortcutResult.success(spec);
        } catch (XmlPullParserException exception) {
            return PreparePinnedShortcutResult.discoveryFailed(
                    exception.getMessage() == null ? "XML discovery failed" : exception.getMessage()
            );
        } catch (android.content.pm.PackageManager.NameNotFoundException exception) {
            return PreparePinnedShortcutResult.discoveryFailed("Settings package not found");
        }
    }

    public ExecResult open() throws IOException, InterruptedException {
        ExecResult authResult = rootAuthorizationUseCase.ensureRootReady();
        if (!rootAuthorizationUseCase.isAuthorized(authResult)) {
            return authResult;
        }

        try {
            SettingsPackageSnapshot snapshot = discoveryUseCase.readSettingsPackageSnapshot();
            SettingsLaunchSpec spec = launchSpecStore.read(snapshot);
            if (spec != null) {
                ExecResult cachedResult = rootExecutor.execute(commandProvider.getOpenCommand(spec));
                if (cachedResult.isSuccess()) {
                    return cachedResult;
                }
                launchSpecStore.clear();
            }

            SettingsLaunchSpec discoveredSpec = discoveryUseCase.discover(snapshot);
            if (discoveredSpec == null) {
                return new ExecResult(
                        false,
                        -1,
                        "",
                        "Failed to discover wireless debugging launch spec"
                );
            }

            ExecResult discoveredResult =
                    rootExecutor.execute(commandProvider.getOpenCommand(discoveredSpec));
            if (discoveredResult.isSuccess()) {
                launchSpecStore.write(discoveredSpec);
            } else {
                launchSpecStore.clear();
            }
            return discoveredResult;
        } catch (XmlPullParserException exception) {
            return new ExecResult(
                    false,
                    -1,
                    "",
                    exception.getMessage() == null ? "XML discovery failed" : exception.getMessage()
            );
        } catch (android.content.pm.PackageManager.NameNotFoundException exception) {
            return new ExecResult(false, -1, "", "Settings package not found");
        }
    }

    public WirelessDebuggingState readState() throws IOException, InterruptedException {
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

    public WirelessDebuggingState toggleState() throws IOException, InterruptedException {
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

    public static final class PreparePinnedShortcutResult {
        private final boolean success;
        private final boolean authorized;
        private final String errorMessage;
        private final SettingsLaunchSpec launchSpec;

        private PreparePinnedShortcutResult(
                boolean success,
                boolean authorized,
                String errorMessage,
                SettingsLaunchSpec launchSpec
        ) {
            this.success = success;
            this.authorized = authorized;
            this.errorMessage = errorMessage;
            this.launchSpec = launchSpec;
        }

        public static PreparePinnedShortcutResult success(SettingsLaunchSpec launchSpec) {
            return new PreparePinnedShortcutResult(true, true, null, launchSpec);
        }

        public static PreparePinnedShortcutResult rootDenied(ExecResult authResult) {
            return new PreparePinnedShortcutResult(
                    false,
                    false,
                    authResult == null ? "Root authorization failed" : authResult.getStderr(),
                    null
            );
        }

        public static PreparePinnedShortcutResult discoveryFailed(String errorMessage) {
            return new PreparePinnedShortcutResult(false, true, errorMessage, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isAuthorized() {
            return authorized;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public SettingsLaunchSpec getLaunchSpec() {
            return launchSpec;
        }
    }
}
