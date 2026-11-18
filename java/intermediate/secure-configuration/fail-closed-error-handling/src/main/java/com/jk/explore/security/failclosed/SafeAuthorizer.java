package com.jk.explore.security.failclosed;

/**
 * Failing closed. The decision starts as "refused" and only an explicit, successful check within its time budget approves. A timeout, an
 * exception or a value that cannot be read is a refusal, with a reference the customer can quote and the detail in the log.
 */
final class SafeAuthorizer {

    static final long FRAUD_BUDGET_MS = 300;

    private final FraudService fraud;
    private final long dailyLimitCents;
    private final References references;
    private final SecurityLog log;

    SafeAuthorizer(FraudService fraud, long dailyLimitCents, References references, SecurityLog log) {
        this.fraud = fraud;
        this.dailyLimitCents = dailyLimitCents;
        this.references = references;
        this.log = log;
    }

    Decision authorize(Payment payment, String limitText) {
        String reference = references.next();
        try {
            if (checkedFraud(payment)) {
                return refuse(reference, payment, "looks fraudulent", "fraud check flagged the payment");
            }
            long spentToday = parseCents(limitText);
            if (spentToday + payment.cents() > dailyLimitCents) {
                return refuse(reference, payment, "over the daily limit", "daily limit reached");
            }
            return Decision.approve(reference);
        } catch (ServiceException e) {
            return refuse(reference, payment, "could not be checked", e.getMessage());
        } catch (RuntimeException e) {
            return refuse(reference, payment, "could not be checked", "unexpected " + e.getClass().getSimpleName());
        }
    }

    /** Calls the fraud service with a time budget: an answer that does not come in time is no answer, and so a refusal. */
    private boolean checkedFraud(Payment payment) throws ServiceException {
        return fraud.risky(payment, FRAUD_BUDGET_MS);
    }

    private static long parseCents(String text) {
        if (text == null || !text.matches("[0-9]{1,12}")) {
            throw new IllegalArgumentException("spent-today is not a plain number");
        }
        return Long.parseLong(text);
    }

    private Decision refuse(String reference, Payment payment, String reason, String detail) {
        log.write(reference + " refused " + payment.id() + " card " + payment.card() + ": " + detail);
        return Decision.refuse(reason, reference);
    }
}
