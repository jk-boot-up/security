package com.jk.explore.security.accesscontrol;

/** Anything that answers requests: the naive store and the guarded one. */
interface Store {

    Reply handle(Request request);
}
