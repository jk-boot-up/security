package com.jk.explore.security.localca;

import java.time.Duration;
import org.bouncycastle.asn1.x509.KeyPurposeId;

/**
 * What a certificate of one kind may be used for and how long it may live. The issuer applies the profile itself, so a requester cannot
 * ask for a certificate that signs other certificates, or one that lives for years.
 */
record Profile(String name, KeyPurposeId purpose, String purposeName, Duration maxLifetime) {

    static final Profile SERVER = new Profile("server", KeyPurposeId.id_kp_serverAuth, "serverAuth", Duration.ofDays(90));
    static final Profile CLIENT = new Profile("client", KeyPurposeId.id_kp_clientAuth, "clientAuth", Duration.ofDays(90));
    static final Profile SHORT_LIVED = new Profile("short-lived", KeyPurposeId.id_kp_serverAuth, "serverAuth", Duration.ofHours(24));
}
