package top.nanboom233.pixelessentials.wireless;

import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class WirelessDebuggingTargetPageDetectorTest {
    @Test
    public void isStrongTargetPage_detectsWirelessDebuggingContentPage() throws Exception {
        WirelessDebuggingTargetPageDetector detector = new WirelessDebuggingTargetPageDetector();

        boolean result = detector.isStrongTargetPage(
                "adb_wireless_settings",
                parserFor(
                        List.of(
                                Map.of(
                                        "key",
                                        "adb_device_name_pref",
                                        "controller",
                                        "com.android.settings.development.AdbDeviceNamePreferenceController"
                                ),
                                Map.of("key", "adb_ip_addr_pref"),
                                Map.of(
                                        "key",
                                        "adb_pair_method_qrcode_pref",
                                        "controller",
                                        "com.android.settings.development.AdbQrCodePreferenceController"
                                )
                        )
                )
        );

        assertTrue(result);
    }

    @Test
    public void isStrongTargetPage_rejectsGenericDeveloperOptionsPage() throws Exception {
        WirelessDebuggingTargetPageDetector detector = new WirelessDebuggingTargetPageDetector();

        boolean result = detector.isStrongTargetPage(
                "development_settings",
                parserFor(
                        List.of(
                                Map.of("key", "enable_adb"),
                                Map.of("key", "clear_adb_keys")
                        )
                )
        );

        assertFalse(result);
    }

    private XmlPullParser parserFor(List<Map<String, String>> tags) {
        final int[] position = {0};
        final int[] eventType = {tags.isEmpty() ? XmlPullParser.END_DOCUMENT : XmlPullParser.START_TAG};
        return (XmlPullParser) Proxy.newProxyInstance(
                XmlPullParser.class.getClassLoader(),
                new Class[]{XmlPullParser.class},
                (proxy, method, args) -> {
                    Map<String, String> attributes =
                            position[0] < tags.size() ? tags.get(position[0]) : Map.of();
                    switch (method.getName()) {
                        case "getEventType":
                            return eventType[0];
                        case "next":
                            position[0]++;
                            eventType[0] = position[0] < tags.size()
                                    ? XmlPullParser.START_TAG
                                    : XmlPullParser.END_DOCUMENT;
                            return eventType[0];
                        case "getAttributeCount":
                            return attributes.size();
                        case "getAttributeName":
                            return attributes.keySet().toArray(new String[0])[(int) args[0]];
                        case "getAttributeValue":
                            if (args.length == 1) {
                                return attributes.values().toArray(new String[0])[(int) args[0]];
                            }
                            if (args.length == 2) {
                                return attributes.get(args[1]);
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
