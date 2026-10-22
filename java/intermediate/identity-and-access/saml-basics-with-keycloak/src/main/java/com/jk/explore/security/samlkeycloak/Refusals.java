package com.jk.explore.security.samlkeycloak;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/** Why the shop refused sign-ins, most recent last. The shopper is only told that the sign-in failed; the log keeps the reason. */
@Component
class Refusals {

    private final List<String> reasons = new CopyOnWriteArrayList<>();

    void add(String reason) {
        reasons.add(reason);
    }

    List<String> all() {
        return List.copyOf(reasons);
    }

    String last() {
        return reasons.isEmpty() ? "none" : reasons.getLast();
    }
}
