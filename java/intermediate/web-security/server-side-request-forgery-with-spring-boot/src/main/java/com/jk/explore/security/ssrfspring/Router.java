package com.jk.explore.security.ssrfspring;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/** Where a connection to an address really goes. In production it goes there; in the demo the invented addresses lead to local servers. */
interface Router {

    InetSocketAddress route(InetAddress address, int port);

    Router DIRECT = InetSocketAddress::new;
}
