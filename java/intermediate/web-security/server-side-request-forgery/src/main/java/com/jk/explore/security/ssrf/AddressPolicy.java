package com.jk.explore.security.ssrf;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

/** Decides, from the real address and not from its spelling, whether a server may connect to it. */
public final class AddressPolicy {

    private AddressPolicy() {
    }

    public static boolean isPublic(InetAddress a) {
        if (a instanceof Inet6Address v6 && isMappedIpv4(v6)) {
            byte[] b = v6.getAddress();
            try {
                return isPublic(InetAddress.getByAddress(new byte[] {b[12], b[13], b[14], b[15]}));
            } catch (java.net.UnknownHostException e) {
                return false;
            }
        }
        if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress() || a.isMulticastAddress()) {
            return false;
        }
        if (a instanceof Inet4Address) {
            int first = a.getAddress()[0] & 0xff;
            int second = a.getAddress()[1] & 0xff;
            if (first == 100 && second >= 64 && second <= 127) {
                return false; // 100.64.0.0/10, shared address space
            }
            if (first == 0 || first >= 240) {
                return false;
            }
        } else if (a instanceof Inet6Address) {
            return (a.getAddress()[0] & 0xfe) != 0xfc; // fc00::/7 unique local
        }
        return true;
    }

    private static boolean isMappedIpv4(Inet6Address a) {
        byte[] b = a.getAddress();
        for (int i = 0; i < 10; i++) {
            if (b[i] != 0) {
                return false;
            }
        }
        return (b[10] & 0xff) == 0xff && (b[11] & 0xff) == 0xff;
    }
}
