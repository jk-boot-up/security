package com.jk.explore.security.cosignsigning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The reason text and the predicate reader are pure string work: no cosign and no registry. */
class CosignResultTest {

    @Test
    void successIsAccepted() {
        assertEquals("accepted", new Cosign.Result(true, "anything").reason());
    }

    @Test
    void eachKindOfFailureHasItsOwnReason() {
        assertEquals("claims do not match the gate", new Cosign.Result(false, "Error: missing or incorrect annotation").why());
        assertEquals("not signed by this key", new Cosign.Result(false, "none of the signatures do not match threshold 1").why());
        assertEquals("refused (no signature for this digest)", new Cosign.Result(false, "Error: no signatures found for image").reason());
        assertEquals("refused (the key could not be read)", new Cosign.Result(false, "Error: the key could not be read\n").reason());
    }

    @Test
    void predicateOfReadsTheStatementInsideaDsseEnvelope() {
        String statement = "{\"predicate\":{\"sbom\":\"present\"}}";
        String payload = Base64.getEncoder().encodeToString(statement.getBytes(StandardCharsets.UTF_8));
        String line = "noise\n{\"payload\":\"" + payload + "\",\"signatures\":[]}\nmore";
        Map<String, Object> predicate = Cosign.predicateOf(line);
        assertEquals("present", predicate.get("sbom"));
    }

    @Test
    void payloadsOfReadsWhatCosignSigned() {
        String output = "Verification for x --\n[{\"critical\":{\"identity\":{\"docker-reference\":\"127.0.0.1:15000/shop/app\"},"
                + "\"image\":{\"docker-manifest-digest\":\"sha256:ab\"},\"type\":\"cosign container image signature\"},"
                + "\"optional\":{\"version\":\"2.4.0\",\"channel\":\"stable\"}},"
                + "{\"critical\":{\"identity\":{\"docker-reference\":\"r\"},\"image\":{\"docker-manifest-digest\":\"sha256:ab\"},"
                + "\"type\":\"t\"},\"optional\":null}]\n";
        java.util.List<SignedPayload> payloads = Cosign.payloadsOf(output);
        assertEquals(2, payloads.size());
        SignedPayload first = payloads.getFirst();
        assertEquals("cosign container image signature", first.type());
        assertEquals("sha256:ab", first.digest());
        assertEquals("127.0.0.1:15000/shop/app", first.reference());
        assertEquals("channel=stable version=2.4.0", first.claimsText());
        assertEquals("(none)", payloads.get(1).claimsText());
    }
}
