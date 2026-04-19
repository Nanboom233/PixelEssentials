package top.nanboom233.pixelessentials.wireless;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class WirelessDebuggingCandidateRanker {
    private static final String DEFAULT_HOST_ACTIVITY = "com.android.settings/.SubSettings";

    public SettingsLaunchSpec rankBest(
            List<SettingsXmlCandidate> candidates,
            Set<String> availableXmlNames,
            Map<String, Integer> targetPageScores,
            SettingsPackageSnapshot snapshot
    ) {
        SettingsXmlCandidate bestCandidate = null;
        int bestScore = Integer.MIN_VALUE;
        for (SettingsXmlCandidate candidate : candidates) {
            int score = score(candidate, availableXmlNames);
            if (score > bestScore) {
                bestScore = score;
                bestCandidate = candidate;
            }
        }

        if (bestCandidate == null) {
            return null;
        }

        String targetPageXmlName = selectBestTargetPageKey(targetPageScores);

        return new SettingsLaunchSpec(
                DEFAULT_HOST_ACTIVITY,
                bestCandidate.getFragmentClassName(),
                resolveArgsKey(bestCandidate, availableXmlNames, targetPageXmlName),
                bestCandidate.getSourceXmlName(),
                targetPageXmlName,
                bestScore,
                snapshot.getVersionCode(),
                snapshot.getLastUpdateTime(),
                SettingsLaunchSpecStore.CURRENT_SCHEMA_VERSION
        );
    }

    private int score(SettingsXmlCandidate candidate, Set<String> availableXmlNames) {
        int score = candidate.getSignalScore();
        String xmlName = normalize(candidate.getSourceXmlName());
        String key = normalize(candidate.getKey());

        if (key.contains("adb_wireless")) {
            score += 90;
        }
        if (key.contains("wireless_debugging")) {
            score += 70;
        }
        if (xmlName.contains("adb_wireless")) {
            score += 50;
        }
        if (xmlName.contains("development")) {
            score += 20;
        }
        if (!key.isBlank() && availableXmlNames.contains(key)) {
            score += 40;
        }
        return score;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String resolveArgsKey(
            SettingsXmlCandidate candidate,
            Set<String> availableXmlNames,
            String targetPageXmlName
    ) {
        String key = normalize(candidate.getKey());
        if (key.isBlank()) {
            return targetPageXmlName;
        }
        if (candidate.isToggleEntryKey() && targetPageXmlName != null) {
            return targetPageXmlName;
        }
        if (availableXmlNames.contains(key)
                || key.contains("adb_wireless")
                || key.contains("wireless_debugging")) {
            return candidate.getKey();
        }
        return targetPageXmlName;
    }

    private String selectBestTargetPageKey(Map<String, Integer> targetPageScores) {
        String bestXmlName = null;
        int bestScore = Integer.MIN_VALUE;
        for (Map.Entry<String, Integer> entry : targetPageScores.entrySet()) {
            String xmlName = entry.getKey();
            String normalizedXmlName = normalize(xmlName);
            int score = entry.getValue();
            if (normalizedXmlName.contains("adb_wireless")) {
                score += 40;
            }
            if (normalizedXmlName.contains("wireless_debugging")) {
                score += 30;
            }
            if (normalizedXmlName.contains("wireless") && normalizedXmlName.contains("settings")) {
                score += 10;
            }
            if (score > bestScore) {
                bestScore = score;
                bestXmlName = xmlName;
            }
        }
        return bestXmlName;
    }
}
