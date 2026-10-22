package com.jk.explore.security.jazzerfuzz.targets;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.jk.explore.security.jazzerfuzz.Parsers;

/** The fuzz target for the fixed order line parser: it may refuse, and nothing else may be thrown. */
public class OrderLineFixed {

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        Parsers.orderLine(data.consumeRemainingAsAsciiString());
    }
}
