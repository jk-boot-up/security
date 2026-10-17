package com.jk.explore.security.patching;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** One known problem in one library. The ids are fictional and exist only for this demo. */
record Advisory(String id, String component, VersionRange affected, SemVer fixed, String vector, String feature, String summary) {

    static List<Advisory> parse(String json) {
        Map<String, Object> document = Json.object(Json.parse(json));
        List<Advisory> advisories = new ArrayList<>();
        for (Object entry : Json.array(document.get("advisories"))) {
            Map<String, Object> item = Json.object(entry);
            Advisory advisory = new Advisory(Json.string(item.get("id")), Json.string(item.get("component")),
                    VersionRange.parse(Json.string(item.get("affected"))), SemVer.parse(Json.string(item.get("fixed"))),
                    Json.string(item.get("vector")), Json.string(item.get("feature")), Json.string(item.get("summary")));
            Cvss31.base(advisory.vector());
            advisories.add(advisory);
        }
        return List.copyOf(advisories);
    }
}
