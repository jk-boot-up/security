package com.jk.explore.security.securitychaos;

/** A payment request: who pays, how much, and the serial of the client certificate that signed it. */
record Payment(String id, String account, int amount, String certSerial) {
}
