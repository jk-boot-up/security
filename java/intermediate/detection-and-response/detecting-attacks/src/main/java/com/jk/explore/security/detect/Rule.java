package com.jk.explore.security.detect;

import java.util.List;

/** A detection rule: reads the whole log, in time order, and returns alerts. */
public interface Rule {

    String name();

    List<Alert> evaluate(List<Event> log);
}
