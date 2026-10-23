package com.jk.explore.security.zerotrust;

import java.util.List;
import java.util.Map;

/** Whoever answers "may this request happen?": OPA in the demo, a lambda in the unit tests. */
interface Decider {

    /** The answer, with the names of the rules that said no. */
    record Decision(boolean allow, List<String> reasons) {
    }

    Decision decide(Map<String, Object> input) throws Exception;
}
