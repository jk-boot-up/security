package com.jk.explore.security.failclosedspring;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Asks the fraud service about a payment, with a time budget for the whole call. Any failure is an exception, never a guess. */
@Component
class FraudClient {

    record Verdict(String risk) {
    }

    /** The fraud call failed; {@code slow} says whether it ran out of time rather than failing outright. */
    static class CallFailedException extends RuntimeException {

        final boolean slow;

        CallFailedException(boolean slow, Throwable cause) {
            super(cause.toString(), cause);
            this.slow = slow;
        }
    }

    private final RestClient client;
    private final String url;
    private final long budgetMs;

    FraudClient(@Value("${bank.fraud.url}") String url, @Value("${bank.fraud.budget-ms}") long budgetMs) {
        this.url = url;
        this.budgetMs = budgetMs;
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofMillis(budgetMs)).build());
        factory.setReadTimeout(Duration.ofMillis(budgetMs));
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    boolean looksFraudulent(Payment payment) {
        long start = System.nanoTime();
        Verdict verdict;
        try {
            verdict = client.post().uri(url + "/check").body(payment).retrieve().body(Verdict.class);
        } catch (RuntimeException e) {
            // the JDK client reports a read timeout as a cancelled request, so the time spent is what tells slow from broken
            long spentMs = (System.nanoTime() - start) / 1_000_000;
            throw new CallFailedException(spentMs >= budgetMs * 9 / 10, e);
        }
        if (verdict == null || verdict.risk() == null) {
            throw new IllegalStateException("no verdict in the fraud service's answer");
        }
        return switch (verdict.risk()) {
            case "high" -> true;
            case "low" -> false;
            default -> throw new IllegalStateException("unknown risk " + verdict.risk());
        };
    }
}
