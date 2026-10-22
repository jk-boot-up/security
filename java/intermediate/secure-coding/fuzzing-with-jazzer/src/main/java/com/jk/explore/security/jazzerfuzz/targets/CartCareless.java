package com.jk.explore.security.jazzerfuzz.targets;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.jk.explore.security.jazzerfuzz.Parsers;

/** The fuzz target for the careless cart parser. */
public class CartCareless {

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        Parsers.cartCareless(data.consumeRemainingAsAsciiString());
    }
}
