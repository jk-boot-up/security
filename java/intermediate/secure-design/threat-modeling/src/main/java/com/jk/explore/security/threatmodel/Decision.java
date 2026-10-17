package com.jk.explore.security.threatmodel;

/**
 * What the team decided to do about one threat. Only people decide: the rules find threats, the register records decisions.
 * A mitigation names the control, which is also the name of the check that proves it. An accepted risk names an owner and a review day.
 */
record Decision(Type type, String detail, String owner, int reviewDay) {

    enum Type {
        MITIGATE, ACCEPT, TRANSFER, AVOID
    }

    static Decision mitigate(String control) {
        return new Decision(Type.MITIGATE, control, null, 0);
    }

    static Decision accept(String reason, String owner, int reviewDay) {
        return new Decision(Type.ACCEPT, reason, owner, reviewDay);
    }

    static Decision transfer(String toWhom) {
        return new Decision(Type.TRANSFER, toWhom, null, 0);
    }

    static Decision avoid(String how) {
        return new Decision(Type.AVOID, how, null, 0);
    }
}
