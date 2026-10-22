package com.jk.explore.security.depconfusion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** The two repositories of the lesson and the folders of the builds that use them. */
public final class Lab implements AutoCloseable {

    static final String DEPENDENCIES = """
                cp 'org.acme:json-lib:2.0.1'
                cp 'com.shop:pricing:[1.4,)'
                cp 'com.shop:audit:3.1.0'""";

    final Repo privateRepo;
    final Repo publicRepo;
    private final Path work;

    /** The company's repository holds the internal libraries; the public one holds a library everyone uses. */
    public Lab() throws IOException {
        this.work = Files.createTempDirectory("depconfusion");
        privateRepo = new Repo("private");
        publicRepo = new Repo("public");
        publicRepo.publish("org.acme", "json-lib", "2.0.1", "public");
        privateRepo.publish("com.shop", "pricing", "1.4.2", "private");
        privateRepo.publish("com.shop", "audit", "3.1.0", "private");
        privateRepo.publish("com.shop.tools", "signer", "1.0", "private");
    }

    Path work() {
        return work;
    }

    /** A {@code maven { }} block for a repository; {@code content} is the Groovy of its content filter, or empty. */
    static String maven(Repo repo, String content) {
        return "        maven {\n            name = '" + repo.name() + "'\n            url = '" + repo.url() + "'\n            allowInsecureProtocol = true\n"
                + (content.isEmpty() ? "" : "            content {\n" + content + "\n            }\n") + "        }";
    }

    Build build(String name, String repositories, String dependencies) throws IOException {
        return new Build(work.resolve(name), repositories, dependencies);
    }

    @Override
    public void close() {
        privateRepo.close();
        publicRepo.close();
    }
}
