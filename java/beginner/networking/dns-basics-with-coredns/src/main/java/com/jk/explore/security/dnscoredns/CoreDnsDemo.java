package com.jk.explore.security.dnscoredns;

import java.io.IOException;

/**
 * Runs the online store story in five numbered acts against a real CoreDNS container and prints repeatable results.
 * It needs a running Docker, so its test is tagged "docker". Ports, ids and the countdown of times to live are not printed;
 * only facts that are the same on every run are.
 */
public class CoreDnsDemo {

    static final String WWW = "www." + ShopZone.ZONE;

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    public static String run() throws IOException, InterruptedException, BadMessageException {
        StringBuilder out = new StringBuilder();
        try (CoreDns server = CoreDns.start(ShopZone.corefile(), ShopZone.zoneFile(ShopZone.GOOD_ADDRESS, ShopZone.GOOD_TTL))) {
            act1RealServer(out, server);
            act2UdpAndTcp(out, server);
            act3MissingNames(out, server);
            act4Cache(out, server);
        }
        act5WrongRecord(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String rcode(int code) {
        return switch (code) {
            case 0 -> "NOERROR";
            case 3 -> "NXDOMAIN";
            case 5 -> "REFUSED";
            default -> "rcode " + code;
        };
    }

    private static void act1RealServer(StringBuilder out, CoreDns server) throws IOException, BadMessageException {
        line(out, "ONE. A real DNS server, in a container.");
        line(out, "  the zone file for " + ShopZone.ZONE + " says " + WWW + " is " + ShopZone.GOOD_ADDRESS + " for " + ShopZone.GOOD_TTL + " seconds");
        DnsMessage.Answer answer = Client.udp(server.udpPort(), DnsMessage.query(0x1001, WWW)).message().answers().get(0);
        line(out, "  asked over UDP, CoreDNS answers " + answer.name() + " is " + Address.text(answer.address()));
        DnsMessage reply = Client.udp(server.udpPort(), DnsMessage.query(0x1002, WWW)).message();
        line(out, "  the answer repeats the question's id: " + (reply.id() == 0x1002) + ", and says it is a response: " + reply.response());
        line(out, "  the client trusts it because the id and the question match, and nothing else");
    }

    private static void act2UdpAndTcp(StringBuilder out, CoreDns server) throws IOException, BadMessageException {
        line(out, "TWO. The same question over UDP and over TCP.");
        Client.Reply udp = Client.udp(server.udpPort(), DnsMessage.query(0x2001, WWW));
        Client.Reply tcp = Client.tcp(server.tcpPort(), DnsMessage.query(0x2002, WWW));
        line(out, "  both give the same address: " + (udp.message().answers().get(0).address() == tcp.message().answers().get(0).address()));
        line(out, "  over TCP each message starts with its length in two bytes, so the answer is " + (tcp.bytes() - udp.bytes())
                + " bytes longer");
        line(out, "  TCP is used when an answer does not fit in a datagram, and to copy a whole zone between servers");
        line(out, "  neither one signs or encrypts anything");
    }

    private static void act3MissingNames(StringBuilder out, CoreDns server) throws IOException, BadMessageException {
        line(out, "THREE. Names that do not exist, names it will not answer, and a rewrite.");
        DnsMessage missing = Client.udp(server.udpPort(), DnsMessage.query(0x3001, "nope." + ShopZone.ZONE)).message();
        line(out, "  nope." + ShopZone.ZONE + ": " + rcode(missing.rcode()) + " with " + missing.answers().size() + " answers");
        DnsMessage outside = Client.udp(server.udpPort(), DnsMessage.query(0x3002, "www.other.example")).message();
        line(out, "  www.other.example: " + rcode(outside.rcode()) + ", because this server only answers for " + ShopZone.ZONE);
        DnsMessage legacy = Client.udp(server.udpPort(), DnsMessage.query(0x3003, "legacy." + ShopZone.ZONE)).message();
        line(out, "  legacy." + ShopZone.ZONE + " is rewritten to " + WWW + " and the answer is " + Address.text(legacy.answers().get(0).address())
                + " under the name asked for: " + legacy.answers().get(0).name());
        line(out, "  a rewrite is a rule in the server, invisible to the client");
    }

    private static void act4Cache(StringBuilder out, CoreDns server) throws IOException, BadMessageException, InterruptedException {
        line(out, "FOUR. The cache and the countdown of the time to live.");
        long first = Client.udp(server.udpPort(), DnsMessage.query(0x4001, WWW)).message().answers().get(0).ttl();
        Thread.sleep(1500);
        long second = Client.udp(server.udpPort(), DnsMessage.query(0x4002, WWW)).message().answers().get(0).ttl();
        Thread.sleep(1500);
        long third = Client.udp(server.udpPort(), DnsMessage.query(0x4003, WWW)).message().answers().get(0).ttl();
        line(out, "  the cache plugin keeps answers for at most " + ShopZone.CACHE_SECONDS + " seconds, so the zone's " + ShopZone.GOOD_TTL
                + " is cut to " + ShopZone.CACHE_SECONDS + ": " + (second <= ShopZone.CACHE_SECONDS));
        line(out, "  each answer from the cache has a smaller time to live than the one before: " + (third < second));
        line(out, "  the first answer, from the zone file, was not counted down: " + (first >= second));
        line(out, "  a cache makes DNS fast, and keeps a false answer until its time runs out");
    }

    private static void act5WrongRecord(StringBuilder out) throws IOException, InterruptedException, BadMessageException {
        line(out, "FIVE. A zone with a wrong record.");
        try (CoreDns server = CoreDns.start(ShopZone.corefile(), ShopZone.zoneFile(ShopZone.WRONG_ADDRESS, ShopZone.WRONG_TTL))) {
            DnsMessage.Answer answer = Client.udp(server.udpPort(), DnsMessage.query(0x5001, WWW)).message().answers().get(0);
            line(out, "  someone changes the record for " + WWW + " to " + ShopZone.WRONG_ADDRESS + " with a time to live of " + ShopZone.WRONG_TTL + " seconds");
            line(out, "  CoreDNS serves it exactly as it serves the right one: " + Address.text(answer.address()) + "  <- THE SERVER CANNOT TELL");
            line(out, "  the cache setting cuts the " + ShopZone.WRONG_TTL + " seconds to " + ShopZone.CACHE_SECONDS + " in what it hands out: "
                    + (answer.ttl() <= ShopZone.CACHE_SECONDS) + ", but a resolver that does not cap would keep it for a day");
        }
        line(out, "  fixing the file fixes the server, but every resolver that cached the wrong answer keeps it until its time runs out");
        line(out, "  the defences are around the file: who may change it, review of changes, a short time to live for important names");
        line(out, "  DNSSEC signs the records, so a resolver can see that a record was not made by the zone's owner (described here, not run)");
    }
}
