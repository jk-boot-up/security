package com.jk.explore.security.securepipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.json.JsonMapper;

/** A small CycloneDX software bill of materials made from the dependencies a build.gradle declares for the code that ships. */
final class Sbom {

    private static final Pattern DECLARATION = Pattern.compile("^\\s*(?:implementation|api|runtimeOnly)\\s+'([^:']+):([^:']+):([^']+)'", Pattern.MULTILINE);

    /** One library with its coordinates. */
    record Component(String group, String name, String version) {

        String purl() {
            return "pkg:maven/" + group + "/" + name + "@" + version;
        }
    }

    private Sbom() {
    }

    static List<Component> components(String buildGradle) {
        List<Component> found = new ArrayList<>();
        Matcher m = DECLARATION.matcher(buildGradle);
        while (m.find()) {
            found.add(new Component(m.group(1), m.group(2), m.group(3)));
        }
        return found;
    }

    static String cycloneDx(List<Component> components) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Component c : components) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", "library");
            m.put("group", c.group());
            m.put("name", c.name());
            m.put("version", c.version());
            m.put("purl", c.purl());
            list.add(m);
        }
        Map<String, Object> bom = new LinkedHashMap<>();
        bom.put("bomFormat", "CycloneDX");
        bom.put("specVersion", "1.5");
        bom.put("version", 1);
        bom.put("components", list);
        return new JsonMapper().writeValueAsString(bom);
    }
}
