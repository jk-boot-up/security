package com.jk.explore.security.patching;

/** A version of the form major.minor.patch, compared as numbers: 1.10.0 is newer than 1.9.0. */
record SemVer(int major, int minor, int patch) implements Comparable<SemVer> {

    static SemVer parse(String text) {
        String[] parts = text.trim().split("\\.", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("not a version of the form 1.2.3: " + text);
        }
        try {
            int[] numbers = new int[3];
            for (int i = 0; i < 3; i++) {
                if (parts[i].isEmpty() || !parts[i].chars().allMatch(Character::isDigit)) {
                    throw new NumberFormatException(parts[i]);
                }
                numbers[i] = Integer.parseInt(parts[i]);
            }
            return new SemVer(numbers[0], numbers[1], numbers[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not a version of the form 1.2.3: " + text, e);
        }
    }

    @Override
    public int compareTo(SemVer other) {
        int byMajor = Integer.compare(major, other.major);
        if (byMajor != 0) {
            return byMajor;
        }
        int byMinor = Integer.compare(minor, other.minor);
        return byMinor != 0 ? byMinor : Integer.compare(patch, other.patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
