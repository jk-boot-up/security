package com.jk.explore.security.securitychaos;

/** Where the decisions of the payments service are written for the auditors. */
interface AuditTrail extends AutoCloseable {

    /** Takes one decision record. False means the trail could not keep it and the caller must not go on. */
    boolean record(String id);

    /** Waits for what is in flight and, where the trail can, sends what could not be sent before. */
    void flush();

    @Override
    void close();
}
