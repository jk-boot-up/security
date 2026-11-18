package com.jk.explore.security.netiso;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A network layout on paper: which services are on which network, and which networks are internal (no route out). It works out, with no Docker, which pairs of services
 * can talk: two containers reach each other exactly when they share a network. The lab builds the same layout for real and measures it, and a test proves the two agree.
 */
public record Layout(Map<String, List<String>> networks, Set<String> internal) {

    /** Every service on one network that has a way out. */
    public static Layout flat() {
        Map<String, List<String>> n = new LinkedHashMap<>();
        n.put("flat", names());
        return new Layout(n, Set.of());
    }

    /** The usual three tiers: the front, the back (internal) and management (internal). */
    public static Layout tiers() {
        Map<String, List<String>> n = new LinkedHashMap<>();
        n.put("front", List.of("web", "app"));
        n.put("back", List.of("app", "db", "cache", "admin"));
        n.put("mgmt", List.of("admin", "ci"));
        return new Layout(n, Set.of("back", "mgmt"));
    }

    /** One internal network for each pair the intent allows, and, if the front is published, an edge network with a way out that only the front is on. */
    public static Layout of(Intent intent, boolean edge) {
        Map<String, List<String>> n = new LinkedHashMap<>();
        Set<String> internal = new TreeSet<>();
        int k = 0;
        for (String pair : intent.pairs()) {
            String net = "link" + (++k);
            n.put(net, List.of(pair.split("-")));
            internal.add(net);
        }
        if (edge) {
            n.put("edge", List.of("web"));
        }
        return new Layout(n, internal);
    }

    private static List<String> names() {
        return Service.shop().stream().map(Service::name).toList();
    }

    /** The pairs that can talk: for every network, every two of its members. */
    public Set<String> pairs() {
        Set<String> out = new TreeSet<>();
        for (List<String> members : networks.values()) {
            for (int i = 0; i < members.size(); i++) {
                for (int j = i + 1; j < members.size(); j++) {
                    out.add(Intent.pair(members.get(i), members.get(j)));
                }
            }
        }
        return out;
    }

    /** How many pairs one network joins: n services make n x (n - 1) / 2 pairs. */
    public int pairsOn(String network) {
        int n = networks.get(network).size();
        return n * (n - 1) / 2;
    }

    /** The networks a service is on, in layout order. A service on two or more is a pivot: it can be used to reach both sides. */
    public List<String> networksOf(String service) {
        List<String> out = new ArrayList<>();
        networks.forEach((net, members) -> {
            if (members.contains(service)) {
                out.add(net);
            }
        });
        return out;
    }

    /** The services that have a way out: those on at least one network that is not internal. */
    public Set<String> canLeave() {
        Set<String> out = new TreeSet<>();
        networks.forEach((net, members) -> {
            if (!internal.contains(net)) {
                out.addAll(members);
            }
        });
        return out;
    }
}
