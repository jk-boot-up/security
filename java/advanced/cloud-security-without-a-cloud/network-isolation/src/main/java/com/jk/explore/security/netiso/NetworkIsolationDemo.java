package com.jk.explore.security.netiso;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class NetworkIsolationDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        Intent intent = Intent.shop();
        act1(out);
        act2(out, intent);
        act3(out, intent);
        act4(out, intent);
        act5(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1(StringBuilder out) throws Exception {
        line(out, "ONE. One flat network.");
        try (Lab lab = new Lab()) {
            lab.network("flat", false);
            for (Service s : Service.shop()) {
                lab.start(s, "flat", false);
            }
            Thread.sleep(1000);
            line(out, "  six containers (" + String.join(", ", lab.names()) + ") on one Docker network, each listening on its port");
            line(out, "  the front is taken over; what it reaches by name: " + lab.reachesByName("web") + "  <- THE DATABASE, THE CACHE, THE ADMIN PANEL AND THE BUILD SERVER");
            int pairs = lab.pairs().size();
            line(out, "  pairs that can talk, in either direction: " + pairs + " of 15; the design wanted " + Intent.shop().pairs().size());
        }
    }

    private static void act2(StringBuilder out, Intent intent) throws Exception {
        line(out, "TWO. Three networks: front, back, management.");
        try (Lab lab = new Lab()) {
            lab.network("front", false);
            lab.network("back", true);
            lab.network("mgmt", true);
            for (Service s : Service.shop()) {
                String net = switch (s.name()) {
                    case "web", "app" -> "front";
                    case "admin", "ci" -> "mgmt";
                    default -> "back";
                };
                lab.start(s, net, s.name().equals("web"));
            }
            lab.join("app", "back");
            lab.join("admin", "back");
            Thread.sleep(1000);
            line(out, "  web and app on front; app, db, cache and admin on back (no route out); admin and ci on mgmt");
            line(out, "  the front is taken over: " + lab.reachesByName("web") + ", and by address, after scanning every address it can learn: " + lab.reachesByAddress("web"));
            Set<String> pairs = lab.pairs();
            line(out, "  pairs that can talk: " + pairs.size() + " of 15; the design wanted " + intent.pairs().size() + "; extra: " + intent.extra(pairs).size() + " " + intent.extra(pairs) + ", missing: " + intent.missing(pairs).size());
            line(out, "  everything on a network can talk to everything else on it, so the coarse layout lets the cache talk to the database and the admin panel to the cache, which nobody asked for");
        }
    }

    private static void act3(StringBuilder out, Intent intent) throws Exception {
        line(out, "THREE. The design as code, and a layout built from it.");
        line(out, "  the intent: " + intent.pairs().size() + " pairs " + intent.pairs());
        try (Lab lab = new Lab()) {
            lab.build(intent, true);
            Thread.sleep(1000);
            Set<String> pairs = lab.pairs();
            line(out, "  one internal network for each pair, and each service on the networks of its pairs: measured " + pairs.size() + " pairs, extra " + intent.extra(pairs).size() + ", missing " + intent.missing(pairs).size());
            line(out, "  the front is taken over: by name " + lab.reachesByName("web") + ", by address " + lab.reachesByAddress("web"));
            lab.network("oops", true);
            lab.join("web", "oops");
            lab.join("db", "oops");
            Thread.sleep(500);
            Set<String> drifted = lab.pairs();
            line(out, "  someone with the Docker socket joins web and db to one more network: extra " + intent.extra(drifted) + "  <- DRIFT, FOUND BY MEASURING AGAINST THE INTENT");
        }
    }

    private static void act4(StringBuilder out, Intent intent) throws Exception {
        line(out, "FOUR. The edge: what is published, and what can leave.");
        try (Lab lab = new Lab()) {
            Service evil = new Service("evil", 4444, "a server on the internet");
            lab.build(intent, true);
            lab.start(evil, "edge", false);
            Thread.sleep(1000);
            int web = lab.publishedPort("web");
            int app = lab.publishedPort("app");
            line(out, "  published to the host: web " + (web > 0 ? "yes" : "no") + ", app " + (app > 0 ? "yes" : "no") + ", db " + (lab.publishedPort("db") > 0 ? "yes" : "no") + ", and the host can open web's port: " + hostConnects(web));
            List<String> canLeave = new java.util.ArrayList<>();
            for (String from : lab.names()) {
                if (!from.equals("evil") && lab.connects(from, "evil", 4444)) {
                    canLeave.add(from);
                }
            }
            line(out, "  an attacker's server on the internet side; the services that can reach it: " + canLeave + "  <- INTERNAL NETWORKS HAVE NO WAY OUT, THE FRONT HAS ONE");
            line(out, "  a taken-over database cannot send anything out, and a taken-over front still can: its way out is the next thing to close");
        }
    }

    private static boolean hostConnects(int port) {
        try (java.net.Socket s = new java.net.Socket()) {
            s.connect(new java.net.InetSocketAddress("127.0.0.1", port), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void act5(StringBuilder out) {
        line(out, "FIVE. The bill.");
        line(out, "  names are scoped to networks: a service that is on two networks (app is on four) is a pivot, and the pivots are where an attacker goes next");
        line(out, "  reachable is not authorised: the database port is open to the application, and whether the application may use it is for the database to decide");
        line(out, "  whoever can use the Docker socket can join any container to any network, so the socket is worth more than every rule above");
        line(out, "  one network for each pair does not scale to a hundred services, which is why clouds have security groups and clusters have network policies, and why those need testing as this does");
        line(out, "  measure against the intent, in the build and on a schedule: a diagram of the network is a wish, and the measurement is the network");
    }
}
