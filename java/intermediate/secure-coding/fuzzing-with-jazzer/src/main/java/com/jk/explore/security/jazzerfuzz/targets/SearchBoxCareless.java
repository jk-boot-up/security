package com.jk.explore.security.jazzerfuzz.targets;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.jk.explore.security.jazzerfuzz.Parsers;

/** The fuzz target for a search box that hands the customer's words to the regular expression engine as they are. */
public class SearchBoxCareless {

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        Parsers.searchCareless(data.consumeRemainingAsAsciiString());
    }
}
