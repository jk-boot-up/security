package com.jk.explore.security.runtimesec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the text records of Linux audit into events. */
public final class Audit {

    private static final Pattern ID = Pattern.compile("msg=audit\\([0-9.]+:(\\d+)\\):");
    private static final Pattern KEY = Pattern.compile(" key=\"([a-z]+)\"");
    private static final Pattern COMM = Pattern.compile(" comm=\"([^\"]*)\"");
    private static final Pattern SUCCESS = Pattern.compile(" success=(yes|no)");
    private static final Pattern CONTAINER = Pattern.compile("cri-containerd-([0-9a-f]{64})\\.scope");
    private static final Pattern NAME = Pattern.compile(" name=\"([^\"]*)\"");
    private static final Pattern NAMETYPE = Pattern.compile(" nametype=([A-Z]+)");
    private static final Pattern CWD = Pattern.compile(" cwd=\"([^\"]*)\"");
    private static final Pattern SADDR = Pattern.compile(" saddr=([0-9A-Fa-f]+)");

    private Audit() {
    }

    /** The container id inside a cgroup path, or null when the process is not in a container. */
    public static String containerId(String cgroup) {
        Matcher m = CONTAINER.matcher(cgroup);
        return m.find() ? m.group(1) : null;
    }

    /** An IPv4 socket address in the hex form audit prints (family, port, address) as ip:port; null for any other family. */
    public static String address(String hex) {
        if (hex.length() < 16) {
            return null;
        }
        int family = Integer.parseInt(hex.substring(2, 4) + hex.substring(0, 2), 16);
        if (family != 2) {
            return null;
        }
        int port = Integer.parseInt(hex.substring(4, 8), 16);
        StringBuilder ip = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i > 0) {
                ip.append('.');
            }
            ip.append(Integer.parseInt(hex.substring(8 + 2 * i, 10 + 2 * i), 16));
        }
        return ip + ":" + port;
    }

    /** Groups the records by event number. Records of an event that has no SYSCALL record are dropped. */
    public static List<Event> parse(String log) {
        Map<String, Builder> events = new LinkedHashMap<>();
        for (String line : log.split("\n")) {
            Matcher id = ID.matcher(line);
            if (!id.find()) {
                continue;
            }
            Builder b = events.computeIfAbsent(id.group(1), k -> new Builder(k));
            if (line.startsWith("type=SYSCALL")) {
                b.syscall = true;
                b.key = first(KEY, line);
                b.comm = first(COMM, line);
                b.success = "yes".equals(first(SUCCESS, line));
                int cg = line.indexOf(" CG=");
                b.containerId = cg < 0 ? null : containerId(line.substring(cg));
            } else if (line.startsWith("type=CWD")) {
                String cwd = first(CWD, line);
                b.cwd = cwd == null ? "/" : cwd;
            } else if (line.startsWith("type=PATH")) {
                String name = first(NAME, line);
                if (name != null) {
                    ("PARENT".equals(first(NAMETYPE, line)) ? b.parents : b.names).add(name);
                }
            } else if (line.startsWith("type=SOCKADDR")) {
                String hex = first(SADDR, line);
                b.address = hex == null ? null : address(hex);
            }
        }
        List<Event> out = new ArrayList<>();
        for (Builder b : events.values()) {
            if (b.syscall) {
                out.add(b.build());
            }
        }
        return out;
    }

    private static String first(Pattern p, String line) {
        Matcher m = p.matcher(line);
        return m.find() ? m.group(1) : null;
    }

    private static final class Builder {
        final String id;
        boolean syscall;
        String key;
        String comm;
        boolean success;
        String containerId;
        String cwd = "/";
        String address;
        final List<String> names = new ArrayList<>();
        final List<String> parents = new ArrayList<>();

        Builder(String id) {
            this.id = id;
        }

        Event build() {
            List<String> paths = new ArrayList<>();
            // A refused open names no file, only the folder it was to be made in.
            for (String n : names.isEmpty() ? parents : names) {
                paths.add(n.startsWith("/") ? n : (cwd.endsWith("/") ? cwd : cwd + "/") + n);
            }
            return new Event(id, key, comm, success, containerId, paths, address);
        }
    }
}
