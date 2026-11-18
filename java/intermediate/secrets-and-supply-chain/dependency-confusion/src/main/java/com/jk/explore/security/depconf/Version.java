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

    /** A teaching method: the first number that differs decides, for example "major 99 > 1". */
    public String whyHigher(Version o) {
        String[] part = {"major", "minor", "patch"};
        int[] a = {major, minor, patch};
        int[] b = {o.major, o.minor, o.patch};
        for (int i = 0; i < 3; i++) {
            if (a[i] != b[i]) {
                return part[i] + " " + a[i] + (a[i] > b[i] ? " > " : " < ") + b[i];
            }
        }
        return "equal";
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
