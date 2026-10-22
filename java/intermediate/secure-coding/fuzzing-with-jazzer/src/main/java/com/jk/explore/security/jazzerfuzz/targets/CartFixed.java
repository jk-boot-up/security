package com.jk.explore.security.jazzerfuzz.targets;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.jk.explore.security.jazzerfuzz.Parsers;

/** The fuzz target for the fixed cart parser. */
public class CartFixed {

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        Parsers.cart(data.consumeRemainingAsAsciiString());
    }
}
