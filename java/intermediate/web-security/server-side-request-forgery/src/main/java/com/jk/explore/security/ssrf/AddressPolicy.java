package com.jk.explore.security.ssrf;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

/** Decides, from the real address and not from its spelling, whether a server may connect to it. */
public final class AddressPolicy {

    private AddressPolicy() {
    }

    public static boolean isPublic(InetAddress a) {
        return kind(a).equals("public");
    }

    /** The kind of address, read from its bytes: "public", or the reason it is inside (loopback, link-local, private...). */
    public static String kind(InetAddress a) {
        if (a instanceof Inet6Address v6 && isMappedIpv4(v6)) {
            byte[] b = v6.getAddress();
            try {
                return kind(InetAddress.getByAddress(new byte[] {b[12], b[13], b[14], b[15]}));
            } catch (java.net.UnknownHostException e) {
                return "invalid";
            }
        }
        if (a.isAnyLocalAddress()) {
            return "unspecified";
        }
        if (a.isLoopbackAddress()) {
            return "loopback"; // 127.0.0.0/8, ::1: the server itself
        }
        if (a.isLinkLocalAddress()) {
            return "link-local"; // 169.254.0.0/16, fe80::/10: the cloud metadata service lives here
        }
        if (a.isSiteLocalAddress()) {
            return "private"; // 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16
        }
        if (a.isMulticastAddress()) {
            return "multicast";
        }
        if (a instanceof Inet4Address) {
            int first = a.getAddress()[0] & 0xff;
            int second = a.getAddress()[1] & 0xff;
            if (first == 100 && second >= 64 && second <= 127) {
                return "shared"; // 100.64.0.0/10, shared address space
            }
            if (first == 0 || first >= 240) {
                return "reserved";
            }
        } else if (a instanceof Inet6Address && (a.getAddress()[0] & 0xfe) == 0xfc) {
            return "unique local"; // fc00::/7
        }
        return "public";
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
