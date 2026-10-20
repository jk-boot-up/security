package com.jk.explore.security.ssrf;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * The world the store's server can reach, as a table from address and path to a response. Every secret here is a harmless marker.
 * What matters is which addresses are "inside": the cloud metadata address, the server itself, and the office network.
 */
public final class Network {

    static final String METADATA = "169.254.169.254";
    static final String CREDENTIALS_PATH = "/latest/meta-data/iam/credentials";
    static final String DEMO_SECRET = "AccessKeyId=DEMO-NOT-A-REAL-KEY";

    private final Map<String, Map<String, Response>> servers = new HashMap<>();

    public void serve(String address, String path, Response response) {
        servers.computeIfAbsent(canonical(address), a -> new HashMap<>()).put(path, response);
    }

    /** One request to one address. The host name travels along, but the address is where the connection goes. */
    public Response get(InetAddress address, String path) {
        Map<String, Response> server = servers.get(address.getHostAddress());
        if (server == null) {
            throw new Unreachable("connection refused: " + address.getHostAddress());
        }
        return server.getOrDefault(path, Response.notFound());
    }

    private static String canonical(String address) {
        try {
            return InetAddress.getByName(address).getHostAddress();
        } catch (java.net.UnknownHostException e) {
            throw new IllegalArgumentException(address, e);
        }
    }

    /** The store's world: a public image host, the attacker's host, the metadata service, the server itself and an internal admin page. */
    public static Network world() {
        Network n = new Network();
        n.serve("93.184.216.34", "/cat.png", Response.ok("image/png", "PNG-cat"));
        n.serve("93.184.216.34", "/report.html", Response.ok("text/html", "<html>not an image</html>"));
        n.serve("93.184.216.34", "/huge.png", Response.ok("image/png", "x".repeat(5000)));
        n.serve("203.0.113.9", "/go", Response.redirect("http://169.254.169.254" + CREDENTIALS_PATH));
        n.serve("203.0.113.9", "/to-public", Response.redirect("http://images.example/cat.png"));
        n.serve("203.0.113.9", "/cat.png", Response.ok("image/png", "PNG-attacker-cat"));
        n.serve(METADATA, CREDENTIALS_PATH, Response.ok("text/plain", DEMO_SECRET));
        n.serve("127.0.0.1", "/actuator/env", Response.ok("text/plain", "DB_PASSWORD=demo-not-real"));
        n.serve("10.0.0.5", "/admin", Response.ok("text/html", "internal admin panel: 3 customers"));
        return n;
    }

    public static Dns dns() {
        Dns d = new Dns();
        d.add("images.example", "93.184.216.34");
        d.add("localhost", "127.0.0.1");
        d.add("attacker.example", "203.0.113.9");
        d.add("intranet.attacker.example", "10.0.0.5");
        d.add("dual.attacker.example", "93.184.216.34", "10.0.0.5");
        d.addRebinding("rebind.attacker.example", "203.0.113.9", "10.0.0.5");
        return d;
    }
}
