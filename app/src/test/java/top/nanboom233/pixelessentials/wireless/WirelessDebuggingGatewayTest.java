package top.nanboom233.pixelessentials.wireless;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import top.nanboom233.pixelessentials.root.ExecResult;
import top.nanboom233.pixelessentials.root.RootAuthorizationUseCase;
import top.nanboom233.pixelessentials.root.RootExecutor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class WirelessDebuggingGatewayTest {
    private static final String SETTINGS_HOST = "com.android.settings/.SubSettings";

    @Test
    public void preparePinnedShortcut_writesDiscoveredSpec() throws Exception {
        SettingsPackageSnapshot snapshot = new SettingsPackageSnapshot("settings.apk", 1L, 2L);
        SettingsLaunchSpec spec = new SettingsLaunchSpec(
                SETTINGS_HOST,
                "com.android.settings.development.WirelessDebuggingFragment",
                "adb_wireless_settings",
                "development_settings",
                "adb_wireless_settings",
                220,
                snapshot.getVersionCode(),
                snapshot.getLastUpdateTime(),
                2
        );
        FakeLaunchSpecStore store = new FakeLaunchSpecStore(null);
        WirelessDebuggingGateway gateway = new WirelessDebuggingGateway(
                authorizedRoot(),
                command -> new ExecResult(true, 0, "", ""),
                new WirelessDebuggingCommandProvider(),
                new WirelessDebuggingStateParser(),
                new FakeDiscoveryUseCase(snapshot, spec),
                store
        );

        WirelessDebuggingGateway.PreparePinnedShortcutResult result = gateway.preparePinnedShortcut();

        assertTrue(result.isSuccess());
        assertEquals(spec, result.getLaunchSpec());
        assertEquals(spec, store.getWrittenSpec());
    }

    @Test
    public void open_invalidatesCacheAndRediscovers() throws Exception {
        SettingsPackageSnapshot snapshot = new SettingsPackageSnapshot("settings.apk", 3L, 4L);
        SettingsLaunchSpec cachedSpec = new SettingsLaunchSpec(
                SETTINGS_HOST,
                "com.android.settings.development.WirelessDebuggingFragment",
                "toggle_adb_wireless",
                "development_settings",
                "adb_wireless_settings",
                150,
                snapshot.getVersionCode(),
                snapshot.getLastUpdateTime(),
                2
        );
        SettingsLaunchSpec discoveredSpec = new SettingsLaunchSpec(
                SETTINGS_HOST,
                "com.android.settings.development.WirelessDebuggingFragment",
                "adb_wireless_settings",
                "development_settings",
                "adb_wireless_settings",
                260,
                snapshot.getVersionCode(),
                snapshot.getLastUpdateTime(),
                2
        );
        FakeLaunchSpecStore store = new FakeLaunchSpecStore(cachedSpec);
        WirelessDebuggingCommandProvider provider = new WirelessDebuggingCommandProvider();
        AtomicInteger executeCount = new AtomicInteger(0);
        RootExecutor executor = command -> {
            int current = executeCount.incrementAndGet();
            if (current == 1) {
                return new ExecResult(false, 1, "", "cached failed");
            }
            return new ExecResult(true, 0, command, "");
        };
        WirelessDebuggingGateway gateway = new WirelessDebuggingGateway(
                authorizedRoot(),
                executor,
                provider,
                new WirelessDebuggingStateParser(),
                new FakeDiscoveryUseCase(snapshot, discoveredSpec),
                store
        );

        ExecResult result = gateway.open();

        assertTrue(result.isSuccess());
        assertTrue(store.wasCleared());
        assertEquals(discoveredSpec, store.getWrittenSpec());
        assertEquals(provider.getOpenCommand(discoveredSpec), result.getStdout());
    }

    @Test
    public void open_usesCachedSpec() throws Exception {
        SettingsPackageSnapshot snapshot = new SettingsPackageSnapshot("settings.apk", 5L, 6L);
        SettingsLaunchSpec spec = new SettingsLaunchSpec(
                SETTINGS_HOST,
                "com.android.settings.development.WirelessDebuggingFragment",
                "adb_wireless_settings",
                "development_settings",
                "adb_wireless_settings",
                200,
                snapshot.getVersionCode(),
                snapshot.getLastUpdateTime(),
                2
        );
        FakeLaunchSpecStore store = new FakeLaunchSpecStore(spec);
        WirelessDebuggingCommandProvider provider = new WirelessDebuggingCommandProvider();
        WirelessDebuggingGateway gateway = new WirelessDebuggingGateway(
                authorizedRoot(),
                command -> new ExecResult(true, 0, command, ""),
                provider,
                new WirelessDebuggingStateParser(),
                new FakeDiscoveryUseCase(snapshot, spec),
                store
        );

        ExecResult result = gateway.open();

        assertTrue(result.isSuccess());
        assertEquals(provider.getOpenCommand(spec), result.getStdout());
    }

    @Test
    public void readState_returnsParsedEnabledValue() throws Exception {
        WirelessDebuggingGateway gateway = new WirelessDebuggingGateway(
                authorizedRoot(),
                command -> new ExecResult(true, 0, "1", ""),
                new WirelessDebuggingCommandProvider(),
                new WirelessDebuggingStateParser(),
                new FakeDiscoveryUseCase(new SettingsPackageSnapshot("settings.apk", 1L, 2L), null),
                new FakeLaunchSpecStore(null)
        );

        WirelessDebuggingState state = gateway.readState();

        assertTrue(state.isAuthorized());
        assertTrue(state.isEnabled());
    }

    @Test
    public void toggleState_verifiesReadbackAfterToggle() throws Exception {
        AtomicInteger calls = new AtomicInteger(0);
        WirelessDebuggingGateway gateway = new WirelessDebuggingGateway(
                authorizedRoot(),
                command -> {
                    int call = calls.incrementAndGet();
                    if (call == 1) {
                        return new ExecResult(true, 0, "0", "");
                    }
                    if (call == 2) {
                        return new ExecResult(true, 0, "", "");
                    }
                    return new ExecResult(true, 0, "1", "");
                },
                new WirelessDebuggingCommandProvider(),
                new WirelessDebuggingStateParser(),
                new FakeDiscoveryUseCase(new SettingsPackageSnapshot("settings.apk", 1L, 2L), null),
                new FakeLaunchSpecStore(null)
        );

        WirelessDebuggingState state = gateway.toggleState();

        assertTrue(state.isAuthorized());
        assertTrue(state.isEnabled());
    }

    @Test
    public void preparePinnedShortcut_returnsRootDeniedWhenUnauthorized() throws Exception {
        WirelessDebuggingGateway gateway = new WirelessDebuggingGateway(
                deniedRoot(),
                command -> new ExecResult(true, 0, "", ""),
                new WirelessDebuggingCommandProvider(),
                new WirelessDebuggingStateParser(),
                new FakeDiscoveryUseCase(new SettingsPackageSnapshot("settings.apk", 1L, 2L), null),
                new FakeLaunchSpecStore(null)
        );

        WirelessDebuggingGateway.PreparePinnedShortcutResult result = gateway.preparePinnedShortcut();

        assertFalse(result.isSuccess());
        assertFalse(result.isAuthorized());
    }

    @Test
    public void open_returnsFailureWhenDiscoveryFindsNoSpec() throws Exception {
        SettingsPackageSnapshot snapshot = new SettingsPackageSnapshot("settings.apk", 7L, 8L);
        FakeLaunchSpecStore store = new FakeLaunchSpecStore(null);
        WirelessDebuggingGateway gateway = new WirelessDebuggingGateway(
                authorizedRoot(),
                command -> new ExecResult(true, 0, "", ""),
                new WirelessDebuggingCommandProvider(),
                new WirelessDebuggingStateParser(),
                new FakeDiscoveryUseCase(snapshot, null),
                store
        );

        ExecResult result = gateway.open();

        assertFalse(result.isSuccess());
        assertEquals("Failed to discover wireless debugging launch spec", result.getStderr());
        assertNull(store.getWrittenSpec());
    }

    private static RootAuthorizationUseCase authorizedRoot() {
        return new RootAuthorizationUseCase(command -> new ExecResult(true, 0, "uid=0(root)", ""));
    }

    private static RootAuthorizationUseCase deniedRoot() {
        return new RootAuthorizationUseCase(command -> new ExecResult(false, 1, "", "denied"));
    }

    private static final class FakeDiscoveryUseCase extends SettingsLaunchSpecDiscoveryUseCase {
        private final SettingsPackageSnapshot snapshot;
        private final SettingsLaunchSpec spec;

        private FakeDiscoveryUseCase(SettingsPackageSnapshot snapshot, SettingsLaunchSpec spec) {
            super(null, new WirelessDebuggingCandidateExtractor(), new WirelessDebuggingCandidateRanker());
            this.snapshot = snapshot;
            this.spec = spec;
        }

        @Override
        public SettingsPackageSnapshot readSettingsPackageSnapshot() {
            return snapshot;
        }

        @Override
        public SettingsLaunchSpec discover(SettingsPackageSnapshot snapshot) {
            return spec;
        }
    }

    private static final class FakeLaunchSpecStore extends SettingsLaunchSpecStore {
        private final SettingsLaunchSpec cachedSpec;
        private SettingsLaunchSpec writtenSpec;
        private boolean cleared;

        private FakeLaunchSpecStore(SettingsLaunchSpec cachedSpec) {
            super(null);
            this.cachedSpec = cachedSpec;
        }

        @Override
        public SettingsLaunchSpec read(SettingsPackageSnapshot snapshot) {
            if (cachedSpec == null) {
                return null;
            }
            if (cachedSpec.getSettingsVersionCode() != snapshot.getVersionCode()
                    || cachedSpec.getSettingsLastUpdateTime() != snapshot.getLastUpdateTime()) {
                return null;
            }
            return cachedSpec;
        }

        @Override
        public void write(SettingsLaunchSpec spec) {
            this.writtenSpec = spec;
        }

        @Override
        public void clear() {
            this.cleared = true;
            this.writtenSpec = null;
        }

        private SettingsLaunchSpec getWrittenSpec() {
            return writtenSpec;
        }

        private boolean wasCleared() {
            return cleared;
        }
    }
}
