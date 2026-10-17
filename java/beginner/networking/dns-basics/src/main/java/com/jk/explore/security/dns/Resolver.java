package com.jk.explore.security.dns;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A caching resolver in a simulation with a clock in seconds. It asks a question with a fresh id and source port, and believes the
 * first response that repeats the id, the port and the question. Answers are cached for their TTL, capped at a maximum.
 */
final class Resolver {

    /** A question that has been sent and is waiting for its answer. */
    record Pending(String name, int id, int port) {
    }

    private record Entry(int address, long expiresAt) {
    }

    private final TransactionIds ids;
    private final long maxTtl;
    private final Map<String, Entry> cache = new HashMap<>();
    private int upstreamQueries;

    Resolver(TransactionIds ids, long maxTtl) {
        this.ids = ids;
        this.maxTtl = maxTtl;
    }

    Optional<Integer> cached(String name, long now) {
        Entry entry = cache.get(name);
        if (entry == null || now >= entry.expiresAt()) {
            return Optional.empty();
        }
        return Optional.of(entry.address());
    }

    /** Sends a question upstream, unless the answer is cached. */
    Optional<Pending> ask(String name, long now) {
        if (cached(name, now).isPresent()) {
            return Optional.empty();
        }
        upstreamQueries++;
        return Optional.of(new Pending(name, ids.nextId(), ids.nextPort()));
    }

    /** Offers a response that arrived at the given port. It is believed only if id, port and question all match. */
    boolean accept(Pending pending, DnsMessage response, int arrivedAtPort, long now) {
        if (!response.response() || response.id() != pending.id() || arrivedAtPort != pending.port()) {
            return false;
        }
        if (response.questions().size() != 1 || !response.questions().get(0).name().equals(pending.name()) || response.answers().isEmpty()) {
            return false;
        }
        DnsMessage.Answer answer = response.answers().get(0);
        cache.put(pending.name(), new Entry(answer.address(), now + Math.min(answer.ttl(), maxTtl)));
        return true;
    }

    int upstreamQueries() {
        return upstreamQueries;
    }
}
