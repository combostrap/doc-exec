package com.combostrap.docExec;

import com.combostrap.docExec.util.Fs;
import com.combostrap.docExec.util.JarManifest;
import com.combostrap.docExec.util.JarManifestNotFoundException;
import com.combostrap.docExec.util.Xml;
import picocli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class DocExecutorCliVersionProvider implements CommandLine.IVersionProvider {


    /**
     * The property that starts with the following prefixes are shown
     * when version is asked
     */
    static private final List<String> attributeVersionPrefixes = List.of("git", "java", "os", "package");


    @Override
    public String[] getVersion() {
        List<String> versionString = getVersionsInfo();
        if (versionString == null) {
            // array of size zero
            return new String[0];
        }
        return versionString.toArray(new String[0]);
    }

    /**
     * @return all version infos
     */
    private static List<String> getVersionsInfo() {
        try {
            return JarManifest.createFor(DocExecutorCliVersionProvider.class)
                    .getMap()
                    .entrySet()
                    .stream()
                    .filter(entry -> attributeVersionPrefixes.stream().anyMatch(entry.getKey().toLowerCase()::startsWith))
                    .map(entry -> entry.getKey() + " : " + entry.getValue())
                    .sorted()
                    .collect(Collectors.toList());
        } catch (JarManifestNotFoundException e) {
            // Not in a jar
            Path pomXml = Fs.closest(Paths.get(""), "pom.xml");
            if (pomXml == null) {
                return null;
            }
            String version = Xml.extractString(pomXml, "/project/version/text()");
            if (version == null) {
                return null;
            }
            return List.of(version);
        }
    }
}
