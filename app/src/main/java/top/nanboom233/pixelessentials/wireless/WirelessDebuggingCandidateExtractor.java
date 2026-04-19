package top.nanboom233.pixelessentials.wireless;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WirelessDebuggingCandidateExtractor {
    public List<SettingsXmlCandidate> extract(String sourceXmlName, XmlPullParser parser)
            throws IOException, XmlPullParserException {
        List<SettingsXmlCandidate> candidates = new ArrayList<>();
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String key = getAttributeByName(parser, "key");
                String fragment = getAttributeByName(parser, "fragment");
                int signalScore = scoreSignal(sourceXmlName, key, fragment);
                if (signalScore > 0) {
                    candidates.add(
                            new SettingsXmlCandidate(
                                    sourceXmlName,
                                    key,
                                    fragment,
                                    signalScore,
                                    isToggleEntryKey(normalize(key))
                            )
                    );
                }
            }
            eventType = parser.next();
        }
        return candidates;
    }

    private int scoreSignal(String sourceXmlName, String key, String fragment) {
        if (fragment == null || fragment.isBlank()) {
            return 0;
        }

        String normalizedXmlName = normalize(sourceXmlName);
        String normalizedKey = normalize(key);
        String normalizedFragment = normalize(fragment);

        int score = 0;
        if (hasWirelessDebuggingSignal(normalizedFragment)) {
            score += 120;
        }
        if (hasWirelessDebuggingSignal(normalizedKey)) {
            score += 90;
        }
        if (hasWirelessDebuggingSignal(normalizedXmlName)
                && hasWirelessDebuggingSignal(normalizedFragment)) {
            score += 40;
        }
        return score;
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

    private boolean hasWirelessDebuggingSignal(String value) {
        return value.contains("wirelessdebugging")
                || value.contains("adbwireless")
                || (value.contains("wireless") && value.contains("debug"))
                || (value.contains("wireless") && value.contains("adb"));
    }

    private boolean isToggleEntryKey(String value) {
        return value.startsWith("toggle_")
                || value.startsWith("enable_")
                || value.startsWith("disable_");
    }
}
