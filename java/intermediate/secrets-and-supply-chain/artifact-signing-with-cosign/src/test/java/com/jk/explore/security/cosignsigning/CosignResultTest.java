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
        assertTrue(new Cosign.Result(false, "Error: missing or incorrect annotation").reason().contains("the gate requires"));
        assertTrue(new Cosign.Result(false, "none of the signatures do not match threshold 1").reason().contains("no signature that this key made"));
        assertTrue(new Cosign.Result(false, "Error: no signatures found for image").reason().contains("no signature for this digest"));
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
}
