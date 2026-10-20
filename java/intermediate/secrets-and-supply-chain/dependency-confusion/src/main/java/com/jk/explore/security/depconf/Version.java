package com.jk.explore.security.depconf;

/** A version like 1.4.2, compared number by number (so 1.10.0 is higher than 1.9.0). */
public record Version(int major, int minor, int patch) implements Comparable<Version> {

    public static Version parse(String text) {
        String[] p = text.split("\\.");
        if (p.length != 3) {
            throw new IllegalArgumentException("not a version: " + text);
        }
        return new Version(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
    }

    @Override
    public int compareTo(Version o) {
        int c = Integer.compare(major, o.major);
        if (c == 0) {
            c = Integer.compare(minor, o.minor);
        }
        return c == 0 ? Integer.compare(patch, o.patch) : c;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
