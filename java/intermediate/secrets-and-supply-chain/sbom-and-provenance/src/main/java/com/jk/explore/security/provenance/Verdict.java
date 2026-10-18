package com.jk.explore.security.provenance;

/** The deployment gate's answer, with the reason. */
record Verdict(boolean allowed, String reason) {

    static Verdict allow() {
        return new Verdict(true, "all checks pass");
    }

    static Verdict refuse(String reason) {
        return new Verdict(false, reason);
    }

    @Override
    public String toString() {
        return allowed ? "deployed" : "refused (" + reason + ")";
    }
}
