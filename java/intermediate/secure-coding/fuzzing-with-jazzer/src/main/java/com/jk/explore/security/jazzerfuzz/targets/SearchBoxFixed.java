package com.jk.explore.security.jazzerfuzz.targets;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.jk.explore.security.jazzerfuzz.Parsers;

/** The fuzz target for the search box that quotes what the customer typed. */
public class SearchBoxFixed {

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        Parsers.search(data.consumeRemainingAsAsciiString());
    }
}
