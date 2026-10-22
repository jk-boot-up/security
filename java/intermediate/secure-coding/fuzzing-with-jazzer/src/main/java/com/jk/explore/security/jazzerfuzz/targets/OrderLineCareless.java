package com.jk.explore.security.jazzerfuzz.targets;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.jk.explore.security.jazzerfuzz.Parsers;

/** The fuzz target for the careless order line parser: anything but a refusal is a finding. */
public class OrderLineCareless {

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        Parsers.orderLineCareless(data.consumeRemainingAsAsciiString());
    }
}
