package top.nanboom233.pixelessentials.wireless;

import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class WirelessDebuggingCandidateRankerTest {
    private static final String SETTINGS_HOST = "com.android.settings/.SubSettings";

    @Test
    public void rankBest_prefersCandidateWithWirelessFragmentAndCompanionXml() {
        WirelessDebuggingCandidateRanker ranker = new WirelessDebuggingCandidateRanker();
        SettingsPackageSnapshot snapshot = new SettingsPackageSnapshot("settings.apk", 7L, 8L);

        SettingsLaunchSpec spec = ranker.rankBest(
                List.of(
                        new SettingsXmlCandidate(
                                "development_settings",
                                "adb_wireless_settings",
                                "com.android.settings.development.WirelessDebuggingFragment",
                                210,
                                false
                        ),
                        new SettingsXmlCandidate(
                                "development_settings",
                                "developer_options",
                                "com.android.settings.development.AdbWirelessDebuggingFragment",
                                180,
                                false
                        )
                ),
                Set.of("development_settings", "adb_wireless_settings"),
                Map.of("adb_wireless_settings", 260),
                snapshot
        );

        assertEquals(SETTINGS_HOST, spec.getHostActivityClassName());
        assertEquals(
                "com.android.settings.development.WirelessDebuggingFragment",
                spec.getFragmentClassName()
        );
        assertEquals("adb_wireless_settings", spec.getFragmentArgsKey());
        assertEquals("development_settings", spec.getEntryXmlName());
        assertEquals("adb_wireless_settings", spec.getTargetPageXmlName());
    }

    @Test
    public void rankBest_doesNotEmitArgsKeyWithoutCompanionXmlOrStrongSignal() {
        WirelessDebuggingCandidateRanker ranker = new WirelessDebuggingCandidateRanker();
        SettingsPackageSnapshot snapshot = new SettingsPackageSnapshot("settings.apk", 9L, 10L);

        SettingsLaunchSpec spec = ranker.rankBest(
                List.of(
                        new SettingsXmlCandidate(
                                "development_settings",
                                "internal_only",
                                "com.android.settings.development.AdbWirelessDebuggingFragment",
                                150,
                                false
                        )
                ),
                Set.of("development_settings"),
                Map.of(),
                snapshot
        );

        assertNull(spec.getFragmentArgsKey());
        assertEquals("development_settings", spec.getEntryXmlName());
        assertNull(spec.getTargetPageXmlName());
    }

    @Test
    public void rankBest_prefersDetectedTargetPageKeyOverToggleEntryKey() {
        WirelessDebuggingCandidateRanker ranker = new WirelessDebuggingCandidateRanker();
        SettingsPackageSnapshot snapshot = new SettingsPackageSnapshot("settings.apk", 11L, 12L);

        SettingsLaunchSpec spec = ranker.rankBest(
                List.of(
                        new SettingsXmlCandidate(
                                "development_settings",
                                "toggle_adb_wireless",
                                "com.android.settings.development.WirelessDebuggingFragment",
                                200,
                                true
                        )
                ),
                Set.of("development_settings", "adb_wireless_settings"),
                Map.of("adb_wireless_settings", 240),
                snapshot
        );

        assertEquals("adb_wireless_settings", spec.getFragmentArgsKey());
        assertEquals("adb_wireless_settings", spec.getTargetPageXmlName());
    }
}
