package top.nanboom233.pixelessentials.wireless;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Locale;

public final class WirelessDebuggingTargetPageDetector {
    public boolean isStrongTargetPage(String sourceXmlName, XmlPullParser parser)
            throws IOException, XmlPullParserException {
        return scoreTargetPage(sourceXmlName, parser) >= 100;
    }

    public int scoreTargetPage(String sourceXmlName, XmlPullParser parser)
            throws IOException, XmlPullParserException {
        int score = scoreXmlName(sourceXmlName);
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                score += scoreAttribute(getAttributeByName(parser, "key"));
                score += scoreAttribute(getAttributeByName(parser, "controller"));
                score += scoreAttribute(getAttributeByName(parser, "fragment"));
            }
            eventType = parser.next();
        }
        return score;
    }

    private int scoreXmlName(String sourceXmlName) {
        String normalized = normalize(sourceXmlName);
        int score = 0;
        if (normalized.contains("adb_wireless")) {
            score += 60;
        }
        if (normalized.contains("wireless_debugging")) {
            score += 60;
        }
        if (normalized.contains("wireless") && normalized.contains("settings")) {
            score += 20;
        }
        return score;
    }

    private int scoreAttribute(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return 0;
        }
        if (normalized.contains("adbdevicenamepreferencecontroller")) {
            return 80;
        }
        if (normalized.contains("adbqrcodepreferencecontroller")) {
            return 80;
        }
        if (normalized.contains("adb_device_name_pref")) {
            return 70;
        }
        if (normalized.contains("adb_ip_addr_pref")) {
            return 70;
        }
        if (normalized.contains("adb_pair_method_qrcode_pref")) {
            return 70;
        }
        if (normalized.contains("adb_pair_method_code_pref")) {
            return 70;
        }
        if (normalized.contains("adb_paired_devices_category")) {
            return 60;
        }
        if (normalized.contains("adb_pairing_methods_category")) {
            return 60;
        }
        if (normalized.contains("adb_wireless_footer_category")) {
            return 50;
        }
        return 0;
    }

    private String getAttributeByName(XmlPullParser parser, String attributeName) {
        for (int index = 0; index < parser.getAttributeCount(); index++) {
            String name = parser.getAttributeName(index);
            if (attributeName.equals(name)) {
                return parser.getAttributeValue(index);
            }
        }
        return null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
