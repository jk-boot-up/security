package com.jk.explore.security.accesscontrol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The routes a store serves, in the order they were added, with the handler for each. */
final class RouteTable {

    record Entry(Route route, Handler handler) {
    }

    record Match(Route route, Handler handler, Map<String, String> params) {
    }

    private final List<Entry> entries = new ArrayList<>();

    RouteTable add(String method, String pattern, Handler handler) {
        entries.add(new Entry(new Route(method, pattern), handler));
        return this;
    }

    Optional<Match> find(Request request) {
        for (Entry entry : entries) {
            Optional<Map<String, String>> params = entry.route().match(request);
            if (params.isPresent()) {
                return Optional.of(new Match(entry.route(), entry.handler(), params.get()));
            }
        }
        return Optional.empty();
    }

    List<Route> routes() {
        return entries.stream().map(Entry::route).toList();
    }
}
