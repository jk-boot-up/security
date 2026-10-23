package com.jk.explore.security.secretscan;

import java.io.IOException;
import java.util.Base64;

/** The bank's repository as it grew: five commits, five real secrets, four look-alikes, and a clean-up commit that removes two files. */
final class BankRepo {

    private BankRepo() {
    }

    static Git build() throws IOException, InterruptedException {
        Git g = Git.init();
        g.write("src/main/resources/app.properties", "payment.key=" + Planted.STRIPE.value() + "\ndb.url=jdbc:postgresql://db/bank\ndb.password=" + Planted.PASSWORD.value() + "\ninstance.id=" + Planted.UUID.value() + "\n");
        g.write("docs/README.md", "Set the cloud key, for example AWS_ACCESS_KEY_ID=" + Planted.AWS_DOC.value() + "\n");
        g.write("checksums.txt", "payments.jar " + Planted.HASH.value() + "\n");
        g.write("src/test/resources/fixture.properties", "payment.key=" + Planted.TEST_KEY.value() + "\n");
        g.commit("first version of the payments service");
        g.write("deploy.env", "AWS_ACCESS_KEY_ID=" + Planted.AWS.value() + "\n");
        g.commit("deploy script");
        g.write("ci.properties", "github.token=" + Planted.GITHUB.value() + "\n");
        g.commit("build on every push");
        g.must("rm", "-q", "src/main/resources/app.properties", "deploy.env");
        g.write("src/main/resources/app.properties", "db.url=jdbc:postgresql://db/bank\n");
        g.commit("remove the secrets from the files");
        g.write("k8s/secret.yaml", "apiVersion: v1\nkind: Secret\nmetadata: {name: payments}\ndata:\n  key: " + Base64.getEncoder().encodeToString(Planted.BASE64.value().getBytes()) + "\n");
        g.commit("store the key in a Kubernetes Secret");
        return g;
    }

    /** A repository with one clean commit. */
    static Git clean() throws IOException, InterruptedException {
        Git g = Git.init();
        g.write("README.md", "payments\n");
        g.commit("start");
        return g;
    }
}
