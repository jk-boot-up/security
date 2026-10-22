package com.jk.explore.security.failclosedspring;

/** Decides a payment. There are two implementations, chosen by the {@code bank.mode} property. */
interface PaymentService {

    Decision decide(Payment payment);
}
