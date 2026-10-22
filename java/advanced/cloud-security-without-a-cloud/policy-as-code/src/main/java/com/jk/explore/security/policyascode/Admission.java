package com.jk.explore.security.policyascode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** The shape of a Kubernetes validating webhook: an AdmissionReview comes in, a response with allowed true or false goes out. */
public final class Admission {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** The answer to a review. */
    public record Response(boolean allowed, String message) {
    }

    private Admission() {
    }

    public static ObjectNode review(ObjectNode object) {
        ObjectNode r = JSON.createObjectNode();
        r.put("apiVersion", "admission.k8s.io/v1").put("kind", "AdmissionReview");
        r.putObject("request").put("uid", "705ab4f5").set("object", object);
        return r;
    }

    public static Response respond(List<String> denials) {
        return new Response(denials.isEmpty(), String.join("; ", denials));
    }

    public static Response handle(Path policy, ObjectNode object) throws IOException, InterruptedException {
        return respond(Opa.deny(policy, "main", review(object)));
    }
}
