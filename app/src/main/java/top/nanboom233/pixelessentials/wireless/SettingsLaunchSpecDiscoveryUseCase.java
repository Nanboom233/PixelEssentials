package top.nanboom233.pixelessentials.wireless;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Build;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class SettingsLaunchSpecDiscoveryUseCase {
    private static final String SETTINGS_PACKAGE_NAME = "com.android.settings";

    private final Context context;
    private final WirelessDebuggingCandidateExtractor extractor;
    private final WirelessDebuggingCandidateRanker ranker;
    private final WirelessDebuggingTargetPageDetector targetPageDetector;

    public SettingsLaunchSpecDiscoveryUseCase(Context context) {
        this(
                context,
                new WirelessDebuggingCandidateExtractor(),
                new WirelessDebuggingCandidateRanker(),
                new WirelessDebuggingTargetPageDetector()
        );
    }

    SettingsLaunchSpecDiscoveryUseCase(
            Context context,
            WirelessDebuggingCandidateExtractor extractor,
            WirelessDebuggingCandidateRanker ranker
    ) {
        this(
                context,
                extractor,
                ranker,
                new WirelessDebuggingTargetPageDetector()
        );
    }

    SettingsLaunchSpecDiscoveryUseCase(
            Context context,
            WirelessDebuggingCandidateExtractor extractor,
            WirelessDebuggingCandidateRanker ranker,
            WirelessDebuggingTargetPageDetector targetPageDetector
    ) {
        this.context = context == null ? null : context.getApplicationContext();
        this.extractor = extractor;
        this.ranker = ranker;
        this.targetPageDetector = targetPageDetector;
    }

    public SettingsPackageSnapshot readSettingsPackageSnapshot() throws PackageManager.NameNotFoundException {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo = packageManager.getApplicationInfo(SETTINGS_PACKAGE_NAME, 0);
        PackageInfo packageInfo;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageInfo = packageManager.getPackageInfo(
                    SETTINGS_PACKAGE_NAME,
                    PackageManager.PackageInfoFlags.of(0)
            );
        } else {
            packageInfo = packageManager.getPackageInfo(SETTINGS_PACKAGE_NAME, 0);
        }
        return new SettingsPackageSnapshot(
                applicationInfo.sourceDir,
                packageInfo.getLongVersionCode(),
                packageInfo.lastUpdateTime
        );
    }

    public SettingsLaunchSpec discover(SettingsPackageSnapshot snapshot)
            throws PackageManager.NameNotFoundException, IOException, XmlPullParserException {
        Set<String> xmlNames = enumerateXmlNames(snapshot.getSourceDir());
        Resources resources = context.getPackageManager().getResourcesForApplication(SETTINGS_PACKAGE_NAME);
        List<SettingsXmlCandidate> candidates = new ArrayList<>();
        Map<String, Integer> targetPageScores = new HashMap<>();
        for (String xmlName : xmlNames) {
            int resourceId = resources.getIdentifier(xmlName, "xml", SETTINGS_PACKAGE_NAME);
            if (resourceId == 0) {
                continue;
            }
            XmlResourceParser parser = resources.getXml(resourceId);
            try {
                candidates.addAll(extractor.extract(xmlName, parser));
            } finally {
                parser.close();
            }

            XmlResourceParser targetPageParser = resources.getXml(resourceId);
            try {
                int targetPageScore = targetPageDetector.scoreTargetPage(xmlName, targetPageParser);
                if (targetPageScore > 0) {
                    targetPageScores.put(xmlName, targetPageScore);
                }
            } finally {
                targetPageParser.close();
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        List<SettingsXmlCandidate> verifiedCandidates = filterCandidatesByExistingFragments(snapshot, candidates);
        if (!verifiedCandidates.isEmpty()) {
            candidates = verifiedCandidates;
        }

        return ranker.rankBest(candidates, normalizeXmlNames(xmlNames), targetPageScores, snapshot);
    }

    private Set<String> enumerateXmlNames(String sourceDir) throws IOException {
        Set<String> xmlNames = new LinkedHashSet<>();
        try (ZipFile zipFile = new ZipFile(sourceDir)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith("res/") || !name.endsWith(".xml")) {
                    continue;
                }
                int lastSlash = name.lastIndexOf('/');
                if (lastSlash < 0) {
                    continue;
                }
                String directory = name.substring(4, lastSlash);
                if (!directory.startsWith("xml")) {
                    continue;
                }
                xmlNames.add(name.substring(lastSlash + 1, name.length() - 4));
            }
        }
        return xmlNames;
    }

    private Set<String> normalizeXmlNames(Set<String> xmlNames) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String xmlName : xmlNames) {
            normalized.add(xmlName == null ? "" : xmlName.toLowerCase());
        }
        return normalized;
    }

    private List<SettingsXmlCandidate> filterCandidatesByExistingFragments(
            SettingsPackageSnapshot snapshot,
            List<SettingsXmlCandidate> candidates
    ) throws IOException {
        Map<String, Boolean> presenceCache = new HashMap<>();
        List<SettingsXmlCandidate> verified = new ArrayList<>();
        for (SettingsXmlCandidate candidate : candidates) {
            String fragmentClassName = candidate.getFragmentClassName();
            Boolean exists = presenceCache.get(fragmentClassName);
            if (exists == null) {
                exists = dexContainsClass(snapshot.getSourceDir(), fragmentClassName);
                presenceCache.put(fragmentClassName, exists);
            }
            if (Boolean.TRUE.equals(exists)) {
                verified.add(candidate);
            }
        }
        return verified;
    }

    private boolean dexContainsClass(String sourceDir, String className) throws IOException {
        if (className == null || className.isBlank()) {
            return false;
        }

        byte[] descriptor = ("L" + className.replace('.', '/') + ";").getBytes(StandardCharsets.UTF_8);
        try (ZipFile zipFile = new ZipFile(sourceDir)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith("classes") || !name.endsWith(".dex")) {
                    continue;
                }
                if (streamContains(zipFile.getInputStream(entry), descriptor)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean streamContains(InputStream inputStream, byte[] needle) throws IOException {
        try (InputStream stream = inputStream; ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            byte[] haystack = outputStream.toByteArray();
            for (int start = 0; start <= haystack.length - needle.length; start++) {
                boolean matches = true;
                for (int index = 0; index < needle.length; index++) {
                    if (haystack[start + index] != needle[index]) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    return true;
                }
            }
            return false;
        }
    }
}
