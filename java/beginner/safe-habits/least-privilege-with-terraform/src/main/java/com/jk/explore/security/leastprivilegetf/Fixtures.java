package com.jk.explore.security.leastprivilegetf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** The five policy documents the Terraform code in infra/terraform writes, saved as the JSON that OpenTofu emitted for them. */
final class Fixtures {

    

    private Fixtures() {
    }

    static String json(String name) {
        try (InputStream in = Fixtures.class.getResourceAsStream("/policies/" + name + ".json")) {
            if (in == null) {
                throw new IllegalArgumentException("no policy named " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static PolicyDocument policy(String name) {
        return PolicyDocument.parse(name, json(name));
    }
}
