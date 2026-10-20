package com.jk.explore.security.depconf;

/** An allowed range of versions: {@code [from, below)}. An exact pin is a range of one version. */
public record VersionRange(Version from, Version below) {

    public static VersionRange exactly(String version) {
        Version v = Version.parse(version);
        return new VersionRange(v, new Version(v.major(), v.minor(), v.patch() + 1));
    }

    /** "1.x": at least 1.0.0 and below 2.0.0. */
    public static VersionRange major(int major) {
        return new VersionRange(new Version(major, 0, 0), new Version(major + 1, 0, 0));
    }

    /** "at least 1.0": no upper limit, the usual way a requirement is written when nobody thought about it. */
    public static VersionRange atLeast(int major) {
        return new VersionRange(new Version(major, 0, 0), new Version(Integer.MAX_VALUE, 0, 0));
    }

    public static VersionRange any() {
        return new VersionRange(new Version(0, 0, 0), new Version(Integer.MAX_VALUE, 0, 0));
    }

    @Override
    public String toString() {
        return below.major() == Integer.MAX_VALUE ? ">= " + from : "[" + from + ", " + below + ")";
    }

    public boolean contains(Version v) {
        return v.compareTo(from) >= 0 && v.compareTo(below) < 0;
    }
}
