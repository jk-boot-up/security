package com.jk.explore.security.patching;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A software bill of materials: the list of every component a program is built from, and which depends on which.
 * The format is a small CycloneDX-like JSON file, not the full standard.
 */
final class Sbom {

    private final String root;
    private final Map<String, Component> components;
    private final Set<String> usedFeatures;

    private Sbom(String root, Map<String, Component> components, Set<String> usedFeatures) {
        this.root = root;
        this.components = components;
        this.usedFeatures = usedFeatures;
    }

    static Sbom parse(String json) {
        Map<String, Object> document = Json.object(Json.parse(json));
        Map<String, Object> metadata = Json.object(document.get("metadata"));
        String root = Json.string(metadata.get("component"));
        Set<String> features = new LinkedHashSet<>();
        for (Object feature : Json.array(metadata.get("usedFeatures"))) {
            features.add(Json.string(feature));
        }
        Map<String, Component> components = new LinkedHashMap<>();
        for (Object entry : Json.array(document.get("components"))) {
            Map<String, Object> item = Json.object(entry);
            List<String> dependsOn = new ArrayList<>();
            for (Object dependency : Json.array(item.get("dependsOn"))) {
                dependsOn.add(Json.string(dependency));
            }
            Component component = new Component(Json.string(item.get("name")), Json.string(item.get("version")), List.copyOf(dependsOn));
            if (components.put(component.ref(), component) != null) {
                throw new IllegalArgumentException("duplicate component " + component.ref());
            }
        }
        if (!components.containsKey(root)) {
            throw new IllegalArgumentException("the root component " + root + " is not in the list");
        }
        for (Component component : components.values()) {
            SemVer.parse(component.version());
            for (String dependency : component.dependsOn()) {
                if (!components.containsKey(dependency)) {
                    throw new IllegalArgumentException(component.ref() + " depends on unknown component " + dependency);
                }
            }
        }
        return new Sbom(root, components, Set.copyOf(features));
    }

    String root() {
        return root;
    }

    Collection<Component> components() {
        return components.values();
    }

    Set<String> usedFeatures() {
        return usedFeatures;
    }

    /** The libraries the program's own build names. */
    List<String> direct() {
        return components.get(root).dependsOn();
    }

    /** Every component the root can reach, apart from the root itself, in the order of the list. */
    Set<String> libraries() {
        Set<String> seen = new java.util.HashSet<>();
        Deque<String> queue = new ArrayDeque<>(List.of(root));
        while (!queue.isEmpty()) {
            for (String next : components.get(queue.poll()).dependsOn()) {
                if (seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        Set<String> ordered = new LinkedHashSet<>();
        for (String ref : components.keySet()) {
            if (seen.contains(ref)) {
                ordered.add(ref);
            }
        }
        return ordered;
    }

    /** The shortest chain of dependencies from the root to the component, or nothing if it cannot be reached. */
    Optional<List<String>> pathTo(String ref) {
        Map<String, String> cameFrom = new HashMap<>();
        Deque<String> queue = new ArrayDeque<>(List.of(root));
        cameFrom.put(root, null);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(ref)) {
                List<String> path = new ArrayList<>();
                for (String step = current; step != null; step = cameFrom.get(step)) {
                    path.add(0, step);
                }
                return Optional.of(path);
            }
            for (String next : components.get(current).dependsOn()) {
                if (!cameFrom.containsKey(next)) {
                    cameFrom.put(next, current);
                    queue.add(next);
                }
            }
        }
        return Optional.empty();
    }

    /** The dependency tree as indented lines. A component that appears twice is listed each time. */
    List<String> tree() {
        List<String> lines = new ArrayList<>();
        walk(root, 0, new LinkedHashSet<>(), lines);
        return lines;
    }

    private void walk(String ref, int depth, Set<String> inProgress, List<String> lines) {
        lines.add("  ".repeat(depth) + ref);
        if (!inProgress.add(ref)) {
            return;
        }
        for (String next : components.get(ref).dependsOn()) {
            if (!inProgress.contains(next)) {
                walk(next, depth + 1, inProgress, lines);
            }
        }
        inProgress.remove(ref);
    }
}
