package com.jk.explore.security.provenance;

/** The deployment gate's answer: deployed, or refused at one numbered check, with the reason. */
record Verdict(boolean allowed, int failedCheck, String reason) {

    static Verdict allow() {
        return new Verdict(true, 0, "all checks pass");
    }

    static Verdict refuse(int failedCheck, String reason) {
        return new Verdict(false, failedCheck, reason);
    }

    @Override
    public String toString() {
        return allowed ? "deployed" : "refused (" + reason + ")";
    }
}
