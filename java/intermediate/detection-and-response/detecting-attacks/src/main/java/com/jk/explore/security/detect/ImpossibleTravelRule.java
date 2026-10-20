package com.jk.explore.security.detect;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Two successful logins by one account from places an aeroplane could not connect in the time between them. Addresses on the {@code knownEgress} list (the company VPN)
 * are skipped, because they show the VPN's city and not the person's.
 */
public final class ImpossibleTravelRule implements Rule {

    private final double maxKmPerHour;
    private final double minKm;
    private final Set<String> knownEgress;

    public ImpossibleTravelRule(double maxKmPerHour, double minKm, Set<String> knownEgress) {
        this.maxKmPerHour = maxKmPerHour;
        this.minKm = minKm;
        this.knownEgress = knownEgress;
    }

    @Override
    public String name() {
        return "impossible-travel" + (knownEgress.isEmpty() ? "" : "+vpn-list");
    }

    @Override
    public List<Alert> evaluate(List<Event> log) {
        Map<String, Event> last = new HashMap<>();
        List<Alert> alerts = new ArrayList<>();
        for (Event e : log) {
            if (!e.type().equals("login") || !e.success() || knownEgress.contains(e.ip())) {
                continue;
            }
            Event prev = last.put(e.user(), e);
            if (prev == null) {
                continue;
            }
            double km = prev.city().kmTo(e.city());
            double hours = Math.max(Duration.between(prev.time(), e.time()).toSeconds() / 3600.0, 1.0 / 3600);
            if (km >= minKm && km / hours > maxKmPerHour) {
                alerts.add(new Alert(name(), e.user(), e.time(), String.format("%s to %s, %.0f km in %.0f minutes", prev.city().name(), e.city().name(), km, hours * 60), List.of(prev, e)));
            }
        }
        return alerts;
    }
}
