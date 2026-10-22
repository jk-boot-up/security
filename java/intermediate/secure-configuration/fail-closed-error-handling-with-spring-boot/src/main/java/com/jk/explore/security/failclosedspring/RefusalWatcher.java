package com.jk.explore.security.failclosedspring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Reads the refusal counters, which is what an alerting rule would do: a closed door is only safe if someone hears it being knocked on. */
@Component
class RefusalWatcher {

    private final MeterRegistry meters;
    private final int alertAt;

    RefusalWatcher(MeterRegistry meters, @Value("${bank.alert.refusals}") int alertAt) {
        this.meters = meters;
        this.alertAt = alertAt;
    }

    Map<String, Integer> byReason() {
        Map<String, Integer> out = new TreeMap<>();
        for (Counter c : meters.find("payments.refused").counters()) {
            out.put(c.getId().getTag("reason"), (int) c.count());
        }
        return out;
    }

    boolean alert() {
        return byReason().values().stream().mapToInt(Integer::intValue).sum() >= alertAt;
    }
}
