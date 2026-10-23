package com.jk.explore.security.controlsevidence;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/** Decides what the evidence of a control says on a given day. */
public final class Assessor {

    /** What a control looks like to an auditor. */
    public enum Status {
        PASSING, PARTIAL, FAILING, STALE, ATTESTED, NONE
    }

    static final int AUTOMATED_DAYS = 90;
    static final int MANUAL_DAYS = 365;

    private final Path evidenceDir;
    private final LocalDate today;

    public Assessor(Path evidenceDir, LocalDate today) {
        this.evidenceDir = evidenceDir;
        this.today = today;
    }

    public Optional<Evidence> evidenceOf(Control c) throws IOException {
        if (c.evidence().isEmpty()) {
            return Optional.empty();
        }
        if (c.evidence().startsWith("attestation:")) {
            return Optional.of(Evidence.attested(LocalDate.parse(c.evidence().substring("attestation:".length()))));
        }
        return Optional.of(Evidence.read(evidenceDir.resolve(c.evidence())));
    }

    public long ageInDays(Evidence e) {
        return ChronoUnit.DAYS.between(e.ran(), today);
    }

    public Status assess(Control c) throws IOException {
        return assess(c, evidenceOf(c));
    }

    public Status assess(Control c, Optional<Evidence> evidence) {
        if (evidence.isEmpty()) {
            return Status.NONE;
        }
        Evidence e = evidence.get();
        if (ageInDays(e) > (c.automated() ? AUTOMATED_DAYS : MANUAL_DAYS)) {
            return Status.STALE;
        }
        if (!c.automated()) {
            return Status.ATTESTED;
        }
        if (!e.failed().isEmpty()) {
            return Status.FAILING;
        }
        return e.skipped() > 0 ? Status.PARTIAL : Status.PASSING;
    }
}
