package com.jk.explore.security.netiso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Builds each layout with real Docker networks and containers, measures what reaches what, and prints the measurement next to what the layout works out on paper.
 * Container names, addresses and host ports are never printed, so the output is the same on every run.
 */
public class NetworkIsolationDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        Intent intent = Intent.shop();
        act1(out, intent);
        act2(out, intent);
        act3(out, intent);
        act4(out, intent);
        act5(out, intent);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String spaced(Iterable<String> names) {
        return String.join(" ", names);
    }

    /** The layout's networks, one per line, with their members and whether they have a way out. */
    private static void layout(StringBuilder out, Layout layout) {
        layout.networks().forEach((net, members) -> line(out, String.format("    %-6s %-26s %s", net, spaced(members), layout.internal().contains(net) ? "internal" : "has a way out")));
    }

    /** How the number of pairs is worked out: n x (n - 1) / 2 for each network, then together without counting a pair twice. */
    private static void pairsWorkedOut(StringBuilder out, Layout layout, Set<String> measured) {
        int sum = 0;
        for (String net : layout.networks().keySet()) {
            int n = layout.networks().get(net).size();
            sum += layout.pairsOn(net);
            line(out, String.format("    %-6s %d services  %d x %d / 2 = %d", net, n, n, n - 1, layout.pairsOn(net)));
        }
        if (layout.networks().size() > 1) {
            line(out, String.format("    together %d, as distinct pairs %d", sum, layout.pairs().size()));
        }
        line(out, "    measured " + measured.size() + " of 15 possible pairs");
    }

    private static void againstTheDesign(StringBuilder out, Intent intent, Set<String> measured) {
        line(out, "  AGAINST THE DESIGN (" + intent.pairs().size() + " pairs wanted):");
        List<String> extra = new ArrayList<>(intent.extra(measured));
        line(out, "    extra    " + extra.size() + (extra.isEmpty() ? "" : "  " + spaced(extra.subList(0, Math.min(4, extra.size())))));
        for (int i = 4; i < extra.size(); i += 4) {
            line(out, "             " + spaced(extra.subList(i, Math.min(i + 4, extra.size()))));
        }
        line(out, "    missing  " + intent.missing(measured).size() + (intent.missing(measured).isEmpty() ? "" : "  " + spaced(intent.missing(measured))));
    }

    private static void act1(StringBuilder out, Intent intent) throws Exception {
        line(out, "ONE. One flat network.");
        Layout flat = Layout.flat();
        line(out, "  THE SERVICES (one container each, listening on a port):");
        List<Service> shop = Service.shop();
        for (Service s : shop) {
            line(out, String.format("    %-6s %-5d %s", s.name(), s.port(), s.role()));
        }
        line(out, "  THE LAYOUT:");
        layout(out, flat);
        try (Lab lab = new Lab()) {
            lab.apply(flat, Set.of());
            Thread.sleep(1000);
            line(out, "  THE ATTACK: web, the public front, is taken over");
            line(out, "  WHAT web REACHES BY NAME (measured with nc):");
            line(out, "    " + spaced(lab.reachesByName("web")) + "  <- EVERYTHING");
            Set<String> pairs = lab.pairs();
            line(out, "  HOW MANY PAIRS CAN TALK (n services make n x (n-1) / 2):");
            pairsWorkedOut(out, flat, pairs);
            againstTheDesign(out, intent, pairs);
        }
    }

    private static void act2(StringBuilder out, Intent intent) throws Exception {
        line(out, "TWO. Three networks: front, back, management.");
        Layout tiers = Layout.tiers();
        line(out, "  THE LAYOUT:");
        layout(out, tiers);
        try (Lab lab = new Lab()) {
            lab.apply(tiers, Set.of("web"));
            Thread.sleep(1000);
            line(out, "  WHAT web REACHES (measured):");
            line(out, "    by name     " + spaced(lab.reachesByName("web")));
            line(out, "    by address  " + spaced(lab.reachesByAddress("web")) + "   (every address it can learn)");
            Set<String> pairs = lab.pairs();
            line(out, "  HOW MANY PAIRS CAN TALK:");
            pairsWorkedOut(out, tiers, pairs);
            againstTheDesign(out, intent, pairs);
            line(out, "  WHY: everything on back talks to everything else on back");
        }
    }

    private static void act3(StringBuilder out, Intent intent) throws Exception {
        line(out, "THREE. The design as code, and a layout built from it.");
        line(out, "  THE INTENT (the pairs that may talk):");
        List<String> wanted = new ArrayList<>(intent.pairs());
        line(out, "    " + spaced(wanted.subList(0, 3)));
        line(out, "    " + spaced(wanted.subList(3, wanted.size())));
        Layout built = Layout.of(intent, true);
        line(out, "  THE LAYOUT BUILT FROM IT (one network per pair):");
        layout(out, built);
        try (Lab lab = new Lab()) {
            lab.build(intent, true);
            Thread.sleep(1000);
            Set<String> pairs = lab.pairs();
            line(out, "  THE MEASUREMENT:");
            line(out, "    pairs " + pairs.size() + ", extra " + intent.extra(pairs).size() + ", missing " + intent.missing(pairs).size());
            line(out, "    web reaches by name " + spaced(lab.reachesByName("web")) + ", by address " + spaced(lab.reachesByAddress("web")));
            line(out, "  THE DRIFT: someone with the Docker socket runs");
            line(out, "    docker network create --internal oops");
            line(out, "    docker network connect oops web, and the same for db");
            lab.network("oops", true);
            lab.join("web", "oops");
            lab.join("db", "oops");
            Thread.sleep(500);
            Set<String> drifted = lab.pairs();
            line(out, "  THE MEASUREMENT AFTER THE DRIFT:");
            line(out, "    pairs " + drifted.size() + ", extra " + spaced(intent.extra(drifted)) + "  <- FOUND BY MEASURING");
        }
    }

    private static void act4(StringBuilder out, Intent intent) throws Exception {
        line(out, "FOUR. The edge: what is published, and what can leave.");
        Layout built = Layout.of(intent, true);
        try (Lab lab = new Lab()) {
            Service evil = new Service("evil", 4444, "a server on the internet");
            lab.build(intent, true);
            lab.start(evil, "edge", false);
            Thread.sleep(1000);
            line(out, "  THE WAY IN: ports published to the host (127.0.0.1)");
            int web = lab.publishedPort("web");
            line(out, "    web    " + (web > 0 ? "published" : "not published") + ", the host connects: " + (hostConnects(web) ? "yes" : "no"));
            for (String s : List.of("app", "db", "cache", "admin", "ci")) {
                line(out, String.format("    %-6s %s", s, lab.publishedPort(s) > 0 ? "published" : "not published"));
            }
            line(out, "  THE WAY OUT: an attacker's server, evil:4444, on edge");
            line(out, "  WHO REACHES IT (measured):");
            List<String> canLeave = new ArrayList<>();
            for (String from : lab.names()) {
                if (!from.equals("evil")) {
                    boolean leaves = lab.connects(from, "evil", 4444);
                    if (leaves) {
                        canLeave.add(from);
                    }
                    line(out, String.format("    %-6s %s", from, leaves ? "yes  <- THE FRONT HAS A WAY OUT" : "no"));
                }
            }
            line(out, "  HOW IT IS WORKED OUT:");
            line(out, "    only edge has a way out, and only " + spaced(built.canLeave()) + " is on edge");
            line(out, "    worked out " + spaced(built.canLeave()) + ", measured " + spaced(canLeave));
        }
    }

    private static boolean hostConnects(int port) {
        if (port <= 0) {
            return false;
        }
        try (java.net.Socket s = new java.net.Socket()) {
            s.connect(new java.net.InetSocketAddress("127.0.0.1", port), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void act5(StringBuilder out, Intent intent) {
        line(out, "FIVE. The bill.");
        Layout built = Layout.of(intent, true);
        line(out, "  THE PIVOTS (services on two or more networks):");
        for (Service s : Service.shop()) {
            List<String> nets = built.networksOf(s.name());
            if (nets.size() > 1) {
                line(out, String.format("    %-6s %d networks  %s", s.name(), nets.size(), spaced(nets)));
            }
        }
        line(out, "    a taken-over pivot reaches every network it is on");
        line(out, "  WHAT THE NETWORK DOES NOT DECIDE:");
        line(out, "    reachable is not authorised: db still checks who app is");
        line(out, "    the Docker socket can join anything to any network");
        line(out, "  WHAT IT COSTS:");
        line(out, "    one network per pair: " + built.networks().size() + " networks for " + Service.shop().size() + " services");
        line(out, "    at scale: security groups and network policies, tested");
        line(out, "    the same way, against the intent, on a schedule");
    }
}
