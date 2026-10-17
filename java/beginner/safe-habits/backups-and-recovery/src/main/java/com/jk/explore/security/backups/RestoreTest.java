package com.jk.explore.security.backups;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** A restore test: restore into a scratch folder and ask the application's own question, "is there a usable orders file?" */
final class RestoreTest {

    record Result(boolean manifestOk, boolean ordersUsable, int orders) {
        boolean passed() {
            return manifestOk && ordersUsable;
        }
    }

    private RestoreTest() {
    }

    static Result run(Path root, String name, Path scratch) throws IOException {
        boolean manifestOk = BackupStore.verifyStored(root, name).ok();
        BackupStore.restore(root, name, scratch);
        Path orders = scratch.resolve("orders.csv");
        if (!Files.isRegularFile(orders)) {
            return new Result(manifestOk, false, 0);
        }
        var lines = Files.readAllLines(orders);
        boolean shaped = lines.size() > 1 && lines.stream().allMatch(line -> line.split(",", -1).length == 3);
        return new Result(manifestOk, shaped, shaped ? lines.size() - 1 : 0);
    }
}
