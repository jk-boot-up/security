package com.jk.explore.security.patchingtrivy;

/** A problem the team knows about itself, with the versions it affects: from (included) up to until (not included). Used to cross-check the scanner. */
record OwnAdvisory(String id, String packageName, Version from, Version until) {

    boolean affects(String name, String version) {
        if (!packageName.equals(name)) {
            return false;
        }
        Version v = Version.parse(version);
        return v.compareTo(from) >= 0 && v.isOlderThan(until);
    }

    /** Two well known problems, with ranges from their public advisories (Log4Shell and the Commons Text interpolation problem). */
    static java.util.List<OwnAdvisory> known() {
        return java.util.List.of(
                new OwnAdvisory("CVE-2021-44228", "org.apache.logging.log4j:log4j-core", Version.parse("2.0-beta9"), Version.parse("2.15.0")),
                new OwnAdvisory("CVE-2022-42889", "org.apache.commons:commons-text", Version.parse("1.5"), Version.parse("1.10.0")));
    }
}
