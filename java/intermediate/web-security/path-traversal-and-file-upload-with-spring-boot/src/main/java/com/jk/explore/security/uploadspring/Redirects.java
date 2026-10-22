package com.jk.explore.security.uploadspring;

/** Where the customer is sent after an upload: the address in the "return to" parameter, judged or not. */
interface Redirects {

    String target(String returnTo);

    Redirects NAIVE = ReturnLink::naive;
    Redirects SAFE = ReturnLink::safe;
}
