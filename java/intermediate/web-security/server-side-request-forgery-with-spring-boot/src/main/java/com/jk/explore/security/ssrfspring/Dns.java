package com.jk.explore.security.ssrfspring;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/** Name to addresses. The JDK's own resolver is the default; the demo's {@link Internet} answers for the invented names. */
interface Dns {

    List<InetAddress> resolve(String host) throws UnknownHostException;

    Dns SYSTEM = host -> List.of(InetAddress.getAllByName(host));
}
