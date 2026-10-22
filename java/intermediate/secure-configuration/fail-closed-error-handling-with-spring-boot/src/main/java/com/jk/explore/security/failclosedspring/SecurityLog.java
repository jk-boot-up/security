package com.jk.explore.security.failclosedspring;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/** The bank's own log of refusals, kept in memory so the demo can read it. A card number is never written in full. */
@Component
class SecurityLog {

    private final List<String> lines = new CopyOnWriteArrayList<>();

    void refusal(String reference, String reason, String card) {
        lines.add(reference + " refused: " + reason + (card.isEmpty() ? "" : ", card ending " + card.substring(Math.max(0, card.length() - 4))));
    }

    List<String> lines() {
        return List.copyOf(lines);
    }
}
