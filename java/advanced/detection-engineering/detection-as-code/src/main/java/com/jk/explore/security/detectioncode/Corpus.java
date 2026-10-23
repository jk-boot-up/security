package com.jk.explore.security.detectioncode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** A week of recorded events from a bank's hosts and applications, made by a seeded generator, with the truth of every event. */
record Corpus(List<Ev> events, Map<Integer, Truth> truth) {

    static final long START = Instant.parse("2026-03-02T00:00:00Z").toEpochMilli();
    static final long DAY = 86_400_000L;
    static final int DAYS = 7;

    /** An event before it has a number. */
    record Pending(long ms, Map<String, String> f, String kind, String rule, String technique, int instance) {
    }

    /** What the generators add events to. */
    static final class Sink {
        final Random r;
        final List<Pending> out = new ArrayList<>();
        private int instances;

        Sink(Random r) {
            this.r = r;
        }

        int newInstance() {
            return ++instances;
        }

        void add(long ms, Map<String, String> f, String kind, String rule, String technique, int instance) {
            out.add(new Pending(ms, f, kind, rule, technique, instance));
        }

        void benign(long ms, Map<String, String> f) {
            add(ms, f, "benign", "-", "-", 0);
        }

        long at(int day, int second) {
            return START + day * DAY + second * 1000L;
        }

        long anyTime(int day) {
            return at(day, r.nextInt(86_400));
        }
    }

    static Map<String, String> m(String... kv) {
        Map<String, String> f = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            f.put(kv[i], kv[i + 1]);
        }
        return f;
    }

    static Corpus generate(long seed) {
        Sink s = new Sink(new Random(seed));
        Benign.events(s);
        Lookalikes.events(s);
        Attacks.events(s);
        s.out.sort(Comparator.comparingLong(Pending::ms));
        List<Ev> events = new ArrayList<>();
        Map<Integer, Truth> truth = new LinkedHashMap<>();
        for (Pending p : s.out) {
            int id = events.size() + 1;
            events.add(new Ev(id, p.ms(), p.f()));
            truth.put(id, new Truth(id, p.kind(), p.rule(), p.technique(), p.instance()));
        }
        return new Corpus(List.copyOf(events), truth);
    }

    static int day(Ev e) {
        return (int) ((e.ms() - START) / DAY);
    }

    /** The events as JSON lines, one per event, with the id and a timestamp and nothing about the truth. */
    void write(Path file) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Files.writeString(file, String.join("\n", events.stream().map(Ev::json).toList()) + "\n");
    }
}
