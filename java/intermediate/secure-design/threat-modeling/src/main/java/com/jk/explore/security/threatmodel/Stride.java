package com.jk.explore.security.threatmodel;

/** The six STRIDE categories, each the opposite of a property the system should have. */
enum Stride {
    SPOOFING("S", "Spoofing", "authenticity"),
    TAMPERING("T", "Tampering", "integrity"),
    REPUDIATION("R", "Repudiation", "non-repudiation"),
    INFORMATION_DISCLOSURE("I", "Information disclosure", "confidentiality"),
    DENIAL_OF_SERVICE("D", "Denial of service", "availability"),
    ELEVATION_OF_PRIVILEGE("E", "Elevation of privilege", "authorization");

    final String letter;
    final String label;
    final String property;

    Stride(String letter, String label, String property) {
        this.letter = letter;
        this.label = label;
        this.property = property;
    }
}
