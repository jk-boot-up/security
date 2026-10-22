package com.jk.explore.security.ssrfspring;

import java.net.InetAddress;

/** Which addresses a server may fetch from for a customer: public ones only. */
final class AddressPolicy {

    private AddressPolicy() {
    }

    /** The JDK has already turned every spelling (decimal, short, mapped IPv6) into an {@link InetAddress}, so only the address is judged. */
    static boolean isPublic(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return false;
        }
        byte[] b = address.getAddress();
        if (b.length == 4) {
            int first = b[0] & 0xff;
            int second = b[1] & 0xff;
            boolean carrierGradeNat = first == 100 && second >= 64 && second <= 127;
            boolean reserved = first == 0 || first >= 240 || (first == 192 && second == 0 && b[2] == 0);
            return !carrierGradeNat && !reserved;
        }
        return (b[0] & 0xfe) != 0xfc; // fc00::/7, the unique local addresses
    }
}
