package com.jk.explore.security.accesscontrol;

/** The code behind one route. */
@FunctionalInterface
interface Handler {

    Reply handle(Call call);
}
