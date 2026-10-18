package com.jk.explore.security.asvs;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Every requirement judged against one application at one level: met, failed, not applicable, or still to be checked by a person. */
final class Assessment {

    enum Status { MET, FAILED, NOT_APPLICABLE, MANUAL }

    record Result(Requirement requirement, Status status) {
    }

    private final List<Result> results;

    private Assessment(List<Result> results) {
        this.results = List.copyOf(results);
    }

    static Assessment of(List<Requirement> requirements, Level level, SampleApp app) {
        List<Result> results = new ArrayList<>();
        for (Requirement r : requirements) {
            Status status;
            if (!level.covers(r.level())) {
                status = Status.NOT_APPLICABLE;
            } else if (r.verification() == Verification.MANUAL) {
                status = Status.MANUAL;
            } else {
                status = Checks.named(r.check()).passes().test(app) ? Status.MET : Status.FAILED;
            }
            results.add(new Result(r, status));
        }
        return new Assessment(results);
    }

    List<Result> results() {
        return results;
    }

    Map<Status, Integer> totals() {
        Map<Status, Integer> totals = new EnumMap<>(Status.class);
        for (Status s : Status.values()) {
            totals.put(s, 0);
        }
        results.forEach(r -> totals.merge(r.status(), 1, Integer::sum));
        return totals;
    }

    /** Per chapter, in the order chapters first appear: how many requirements are met, failed and manual (not-applicable ones left out). */
    Map<String, Map<Status, Integer>> byChapter() {
        Map<String, Map<Status, Integer>> chapters = new LinkedHashMap<>();
        for (Result r : results) {
            if (r.status() == Status.NOT_APPLICABLE) {
                continue;
            }
            chapters.computeIfAbsent(r.requirement().chapter(), c -> new EnumMap<>(Status.class)).merge(r.status(), 1, Integer::sum);
        }
        return chapters;
    }

    List<String> failing() {
        return results.stream().filter(r -> r.status() == Status.FAILED).map(r -> r.requirement().id()).toList();
    }

    /** Requirements that failed here but not in the earlier assessment: what a release gate refuses. */
    List<String> newlyFailingSince(Assessment earlier) {
        List<String> before = earlier.failing();
        return failing().stream().filter(id -> !before.contains(id)).toList();
    }
}
