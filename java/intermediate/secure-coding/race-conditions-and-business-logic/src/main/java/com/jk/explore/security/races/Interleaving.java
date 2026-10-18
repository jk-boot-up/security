package com.jk.explore.security.races;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs requests step by step in a written order, such as "ABAB": the letter says which request takes its next step. After the
 * order runs out, the unfinished requests take turns until they finish, or until a whole round passes in which every one of them
 * only waits, which is a deadlock.
 */
final class Interleaving {

    record Result(List<String> trace, boolean deadlock, int waits) {
    }

    private Interleaving() {
    }

    static Result run(String order, Steps... requests) {
        List<String> trace = new ArrayList<>();
        int waits = 0;
        for (char c : order.toCharArray()) {
            Steps request = requests[c - 'A'];
            if (request.finished()) {
                continue;
            }
            trace.add(request.step());
            if (request.waiting()) {
                waits++;
            }
        }
        while (true) {
            boolean anyLeft = false;
            boolean progress = false;
            for (Steps request : requests) {
                if (request.finished()) {
                    continue;
                }
                anyLeft = true;
                trace.add(request.step());
                if (request.waiting()) {
                    waits++;
                } else {
                    progress = true;
                }
            }
            if (!anyLeft) {
                return new Result(trace, false, waits);
            }
            if (!progress) {
                trace.add("every request is waiting for a lock another one holds: deadlock");
                return new Result(trace, true, waits);
            }
        }
    }
}
