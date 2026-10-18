package com.jk.explore.security.provenance;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;

/**
 * What the deployment gate checks before an artifact runs: a signature from a trusted builder key over the statement, the statement
 * about this exact artifact, built by the expected builder, from the expected repository and branch. Every check must pass; anything
 * else is refused.
 */
record Policy(Map<String, PublicKey> trustedKeys, String builderId, String repository, String ref) {

    Verdict check(String envelope, String artifactName, byte[] artifact) {
        if (envelope == null) {
            return Verdict.refuse("no provenance");
        }
        Dsse.Opened opened;
        try {
            opened = Dsse.open(envelope, trustedKeys);
        } catch (Dsse.BadEnvelopeException e) {
            return Verdict.refuse(e.getMessage());
        }
        Object statement;
        try {
            statement = Json.read(new String(opened.payload(), StandardCharsets.UTF_8));
        } catch (Json.BadJsonException e) {
            return Verdict.refuse("the statement is not valid JSON");
        }
        if (!Provenance.STATEMENT_TYPE.equals(Json.text(statement, "_type"))
                || !Provenance.PREDICATE_TYPE.equals(Json.text(statement, "predicateType"))) {
            return Verdict.refuse("not a SLSA provenance statement");
        }
        if (!subjectMatches(statement, artifactName, artifact)) {
            return Verdict.refuse("the statement is about another artifact");
        }
        if (!builderId.equals(Json.text(statement, "predicate", "runDetails", "builder", "id"))) {
            return Verdict.refuse("built by a builder that is not trusted");
        }
        if (!repository.equals(Json.text(statement, "predicate", "buildDefinition", "externalParameters", "repository"))) {
            return Verdict.refuse("built from another repository");
        }
        if (!ref.equals(Json.text(statement, "predicate", "buildDefinition", "externalParameters", "ref"))) {
            return Verdict.refuse("built from another branch");
        }
        return Verdict.allow();
    }

    private static boolean subjectMatches(Object statement, String artifactName, byte[] artifact) {
        if (!(Json.at(statement, "subject") instanceof List<?> subjects)) {
            return false;
        }
        byte[] digest = Digests.sha256(artifact);
        for (Object subject : subjects) {
            String hex = Json.text(subject, "digest", "sha256");
            if (artifactName.equals(Json.text(subject, "name")) && hex != null && hex.length() == 64
                    && Digests.same(Digests.unhex(hex), digest)) {
                return true;
            }
        }
        return false;
    }
}
