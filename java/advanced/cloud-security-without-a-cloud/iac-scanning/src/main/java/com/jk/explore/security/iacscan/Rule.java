package com.jk.explore.security.iacscan;

import java.util.function.Predicate;
import tools.jackson.databind.JsonNode;

/** A rule is a question about one container: true means it breaks the rule. The two samples are the rule's own tests. */
public record Rule(String id, String title, Predicate<Container> breaks, String bad, String good) {

    /** A container with the pod it runs in. */
    public record Container(JsonNode pod, JsonNode container) {
        JsonNode security() {
            return container.path("securityContext");
        }
    }
}
