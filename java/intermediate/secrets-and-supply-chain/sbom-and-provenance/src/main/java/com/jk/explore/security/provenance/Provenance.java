package com.jk.explore.security.provenance;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * An in-toto Statement with a SLSA Provenance v1 predicate: the artifact (by digest) is the subject, and the predicate says how it was
 * built: the build type, the source repository and branch it was asked to build, the exact commit it resolved, and the builder that ran it.
 */
final class Provenance {

    static final String STATEMENT_TYPE = "https://in-toto.io/Statement/v1";
    static final String PREDICATE_TYPE = "https://slsa.dev/provenance/v1";
    static final String BUILD_TYPE = "https://build.shop.example/gradle-jar/v1";

    private Provenance() {
    }

    static byte[] statement(String artifactName, byte[] artifact, Source source, String builderId, String invocationId) {
        Map<String, Object> statement = Json.object(
                "_type", STATEMENT_TYPE,
                "subject", List.of(Json.object(
                        "name", artifactName,
                        "digest", Json.object("sha256", Digests.hex(Digests.sha256(artifact))))),
                "predicateType", PREDICATE_TYPE,
                "predicate", Json.object(
                        "buildDefinition", Json.object(
                                "buildType", BUILD_TYPE,
                                "externalParameters", Json.object("repository", source.repository(), "ref", source.ref()),
                                "internalParameters", Json.object(),
                                "resolvedDependencies", List.of(Json.object(
                                        "uri", "git+" + source.repository() + "@" + source.ref(),
                                        "digest", Json.object("gitCommit", source.commit())))),
                        "runDetails", Json.object(
                                "builder", Json.object("id", builderId),
                                "metadata", Json.object(
                                        "invocationId", invocationId,
                                        "startedOn", "2026-10-01T09:00:00Z",
                                        "finishedOn", "2026-10-01T09:04:00Z"))));
        return Json.write(statement).getBytes(StandardCharsets.UTF_8);
    }
}
