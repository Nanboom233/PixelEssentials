package top.nanboom233.pixelessentials.wireless;

import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class WirelessDebuggingCandidateExtractorTest {
    @Test
    public void extract_findsWirelessDebuggingPreferenceCandidate() throws Exception {
        WirelessDebuggingCandidateExtractor extractor = new WirelessDebuggingCandidateExtractor();
        List<SettingsXmlCandidate> candidates = extractor.extract(
                "development_settings",
                parserFor(
                        "adb_wireless_settings",
                        "com.android.settings.development.WirelessDebuggingFragment"
                )
        );

        assertEquals(1, candidates.size());
        assertEquals("adb_wireless_settings", candidates.get(0).getKey());
        assertEquals(
                "com.android.settings.development.WirelessDebuggingFragment",
                candidates.get(0).getFragmentClassName()
        );
        assertEquals(false, candidates.get(0).isToggleEntryKey());
    }

    @Test
    public void extract_ignoresGenericWirelessPageWithoutWirelessDebuggingSignal() throws Exception {
        WirelessDebuggingCandidateExtractor extractor = new WirelessDebuggingCandidateExtractor();
        List<SettingsXmlCandidate> candidates = extractor.extract(
                "wireless_settings",
                parserFor(
                        "wifi_settings",
                        "com.android.settings.wifi.WifiSettings"
                )
        );

        assertEquals(0, candidates.size());
    }

    private XmlPullParser parserFor(String key, String fragment) {
        final int[] eventType = {XmlPullParser.START_TAG};
        return (XmlPullParser) Proxy.newProxyInstance(
                XmlPullParser.class.getClassLoader(),
                new Class[]{XmlPullParser.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getEventType":
                            return eventType[0];
                        case "next":
                            eventType[0] = XmlPullParser.END_DOCUMENT;
                            return eventType[0];
                        case "getAttributeCount":
                            return 2;
                        case "getAttributeName":
                            return ((int) args[0]) == 0 ? "key" : "fragment";
                        case "getAttributeValue":
                            if (args.length == 1) {
                                return ((int) args[0]) == 0 ? key : fragment;
                            }
                            return null;
                        default:
                            Class<?> returnType = method.getReturnType();
                            if (returnType == boolean.class) {
                                return false;
                            }
                            if (returnType == int.class) {
                                return 0;
                            }
                            return null;
                    }
                }
        );
    }
}
