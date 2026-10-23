package com.jk.explore.security.threathunt;

import java.util.List;

/** The three kinds of record that the hunts read: name lookups with the bytes sent out, process starts, and logins. All times are seconds from the start of the week. */
public final class Events {

    public record Dns(String host, String domain, long at, long bytesOut) {
    }

    public record Proc(String host, String parent, String child, long at) {
    }

    public record Login(String user, String ip, long at) {

        public int hour() {
            return (int) (at / 3600 % 24);
        }

        public int day() {
            return (int) (at / 86400);
        }
    }

    /** A week of activity and the truth about it: which of the things in it are attacks. */
    public record Week(List<Dns> dns, List<Proc> procs, List<Login> logins) {
    }

    private Events() {
    }
}
