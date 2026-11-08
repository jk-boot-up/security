package com.jk.explore.security.backups;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. */
public class BackupsDemo {

    static final String ORDERS = "id,customer,total\n1001,ana,49\n1002,ben,15\n";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Keys are random, so only facts that do not depend on them are printed. */
    public static String run() throws IOException, GeneralSecurityException, BackupVault.WrongKeyException {
        StringBuilder out = new StringBuilder();
        Path dir = Files.createTempDirectory("backups-demo");
        try {
            act1FullAndIncremental(out, dir.resolve("one"));
            act2Trust(out, dir.resolve("two"));
            act3RulesAndRetention(out);
            act4Ransomware(out, dir.resolve("four"));
            act5Objectives(out, dir.resolve("five"));
        } finally {
            deleteAll(dir);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    static void deleteAll(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    static Path shop(Path dir) throws IOException {
        Path live = dir.resolve("live");
        Files.createDirectories(live);
        Files.writeString(live.resolve("orders.csv"), ORDERS);
        Files.writeString(live.resolve("customers.csv"), "id,name\nana,Ana\nben,Ben\n");
        Files.writeString(live.resolve("inventory.csv"), "sku,stock\nA-1,40\nB-2,7\n");
        return live;
    }

    private static String pad(String text, int width) {
        return String.format("%-" + width + "s", text);
    }

    /** The names of the files a snapshot itself stored, sorted. */
    static List<String> storedNames(Path root, String name) throws IOException {
        Path data = root.resolve(name).resolve("data");
        try (Stream<Path> walk = Files.walk(data)) {
            return walk.filter(Files::isRegularFile)
                    .map(file -> data.relativize(file).toString())
                    .sorted()
                    .toList();
        }
    }

    private static void act1FullAndIncremental(StringBuilder out, Path dir) throws IOException {
        line(out, "ONE. Full and incremental backups, with a manifest.");
        Path live = shop(dir);
        Path root = dir.resolve("backups");
        line(out, "  THE SHOP'S FILES ON DAY 1");
        line(out, "    " + String.join("  ", Manifest.of(live).entries().keySet()));
        line(out, "  THE CHANGES");
        line(out, "    day 2: orders.csv gets order 1003, refunds.csv is new");
        line(out, "    day 3: customers.csv gets the customer cy");
        BackupStore.full(live, root, "day1");
        Files.writeString(live.resolve("orders.csv"), ORDERS + "1003,cy,99\n");
        Files.writeString(live.resolve("refunds.csv"), "order,amount\n1002,15\n");
        BackupStore.incremental(live, root, "day2", "day1");
        Files.writeString(live.resolve("customers.csv"), "id,name\nana,Ana\nben,Ben\ncy,Cy\n");
        BackupStore.incremental(live, root, "day3", "day2");
        line(out, "  THE BACKUPS");
        line(out, "    day    kind         files stored");
        String[][] days = {{"day1", "full"}, {"day2", "incremental"}, {"day3", "incremental"}};
        for (String[] day : days) {
            List<String> stored = storedNames(root, day[0]);
            line(out, "    " + pad(day[0].replace("day", "day "), 7) + pad(day[1], 13)
                    + String.join(" ", stored).replace(".csv", ""));
        }
        Manifest manifest = BackupStore.state(root, "day3");
        line(out, "  THE MANIFEST OF DAY 3 (SHA-256, FIRST 16 HEX DIGITS)");
        manifest.entries().forEach((file, hash) ->
                line(out, "    " + hash.substring(0, 16) + "  " + file));
        line(out, "  THE RESTORE OF DAY 3");
        for (String file : manifest.entries().keySet()) {
            Path found = BackupStore.locate(root, "day3", file);
            String from = root.relativize(found).getName(0).toString();
            line(out, "    " + pad(file, 15) + "from " + from.replace("day", "day "));
        }
        Manifest.Problems problems = BackupStore.restore(root, "day3", dir.resolve("restored"));
        line(out, "    problems against the manifest: " + problems.count());
        line(out, "    a restore needs the whole chain: day 1, 2 and 3");
    }

    private static void act2Trust(StringBuilder out, Path dir) throws IOException {
        line(out, "TWO. Can the backup be trusted? Only a restore test says.");
        Path live = shop(dir);
        Path root = dir.resolve("backups");
        BackupStore.full(live, root, "day1");
        Files.writeString(live.resolve("orders.csv"), ORDERS + "1003,cy,99\n");
        BackupStore.incremental(live, root, "day2", "day1");
        line(out, "  THE DAMAGE TO THE STORED BACKUP");
        Path stored = root.resolve("day2").resolve("data").resolve("orders.csv");
        byte[] bytes = Files.readAllBytes(stored);
        bytes[bytes.length - 3] ^= 0x01;
        Files.write(stored, bytes);
        line(out, "    one byte of orders.csv in day 2 is changed");
        Files.delete(root.resolve("day1").resolve("data").resolve("inventory.csv"));
        line(out, "    inventory.csv in day 1 is deleted");
        Manifest.Problems found = BackupStore.verifyStored(root, "day2");
        line(out, "  WHAT THE MANIFEST CHECK FINDS");
        line(out, "    changed: " + String.join(" ", found.changed()));
        line(out, "    missing: " + String.join(" ", found.missing()));
        Path partialLive = dir.resolve("partial");
        Files.createDirectories(partialLive);
        Files.writeString(partialLive.resolve("customers.csv"), "id,name\nana,Ana\n");
        BackupStore.full(partialLive, root, "partial");
        BackupStore.full(live, root, "good");
        line(out, "  THE TWO BACKUPS TESTED");
        line(out, "    partial: the job skipped orders.csv, it was in use");
        line(out, "    good:    every file copied");
        RestoreTest.Result partial = RestoreTest.run(root, "partial", dir.resolve("scratch-partial"));
        RestoreTest.Result good = RestoreTest.run(root, "good", dir.resolve("scratch-good"));
        line(out, "  THE RESTORE TEST RESULTS");
        line(out, "    backup   manifest  orders file  orders  passed");
        line(out, "    partial  " + pad(partial.manifestOk() ? "ok" : "bad", 10)
                + pad(partial.ordersUsable() ? "usable" : "missing", 13)
                + pad(String.valueOf(partial.orders()), 8) + partial.passed());
        line(out, "    good     " + pad(good.manifestOk() ? "ok" : "bad", 10)
                + pad(good.ordersUsable() ? "usable" : "missing", 13)
                + pad(String.valueOf(good.orders()), 8) + good.passed());
        line(out, "    <- A BACKUP NOBODY RESTORED IS A HOPE");
    }

    private static void rule(StringBuilder out, List<ThreeTwoOne.Copy> copies) {
        for (ThreeTwoOne.Copy copy : copies) {
            String flags = (copy.offsite() ? ", off site" : "") + (copy.immutable() ? ", locked" : "");
            line(out, "    " + pad(copy.place(), 27) + copy.medium() + flags);
        }
        ThreeTwoOne.Result result = ThreeTwoOne.check(copies);
        line(out, "    3 copies " + result.threeCopies() + ", 2 media " + result.twoMedia());
        line(out, "    1 off site " + result.oneOffsite() + ", 1 locked " + result.oneImmutable()
                + ": " + result.score() + " of 4");
    }

    private static void act3RulesAndRetention(StringBuilder out) {
        line(out, "THREE. The 3-2-1 rule, and how long to keep backups.");
        var careless = List.of(
                new ThreeTwoOne.Copy("shop server", "ssd", false, false),
                new ThreeTwoOne.Copy("same server, other folder", "ssd", false, false));
        var careful = List.of(
                new ThreeTwoOne.Copy("shop server", "ssd", false, false),
                new ThreeTwoOne.Copy("office NAS", "disk", false, false),
                new ThreeTwoOne.Copy("bucket, other region", "object store", true, true));
        line(out, "  BEFORE THE FIX: TWO COPIES ON ONE SERVER");
        rule(out, careless);
        line(out, "  AFTER THE FIX: THREE PLACES");
        rule(out, careful);
        LocalDate today = LocalDate.of(2026, 2, 4);
        List<LocalDate> days = new ArrayList<>();
        for (int i = 399; i >= 0; i--) {
            days.add(today.minusDays(i));
        }
        line(out, "  THE SNAPSHOTS");
        line(out, "    " + days.size() + " daily, " + days.getFirst() + " to " + today);
        line(out, "  THE POLICY");
        line(out, "    keep 7 daily, 4 weekly (Sundays), 12 monthly (1st)");
        var daily = Retention.keep(days, today, 7, 0, 0);
        var weekly = Retention.keep(days, today, 0, 4, 0);
        var monthly = Retention.keep(days, today, 0, 0, 12);
        var kept = Retention.keep(days, today, 7, 4, 12);
        line(out, "  WHAT IS KEPT");
        line(out, "    daily    " + daily.size() + "  " + daily.first() + " to " + daily.last());
        line(out, "    weekly   " + weekly.size() + "  " + weekly.first() + " to " + weekly.last());
        line(out, "    monthly " + monthly.size() + "  " + monthly.first() + " to " + monthly.last());
        int both = daily.size() + weekly.size() + monthly.size() - kept.size();
        line(out, "    " + daily.size() + " + " + weekly.size() + " + " + monthly.size() + " - "
                + both + " in two lists = " + kept.size() + " kept, "
                + (days.size() - kept.size()) + " pruned");
        line(out, "    oldest kept " + kept.first() + ": a year in " + kept.size() + " snapshots");
    }

    private static void act4Ransomware(StringBuilder out, Path dir) throws IOException, GeneralSecurityException {
        line(out, "FOUR. Ransomware, and the copy it cannot change.");
        Path live = shop(dir);
        Path mounted = dir.resolve("mounted-backup");
        Files.createDirectories(mounted);
        Map<String, byte[]> originals = new TreeMap<>();
        for (String name : List.of("orders.csv", "customers.csv", "inventory.csv")) {
            originals.put(name, Files.readAllBytes(live.resolve(name)));
            Files.copy(live.resolve(name), mounted.resolve(name));
        }
        Ransomware.VersionedStore store = new Ransomware.VersionedStore();
        originals.forEach((name, data) -> store.append(name, data, 100));
        Path readOnly = dir.resolve("read-only-backup");
        Files.createDirectories(readOnly);
        for (String name : originals.keySet()) {
            Files.write(readOnly.resolve(name), originals.get(name));
            Files.setPosixFilePermissions(readOnly.resolve(name), PosixFilePermissions.fromString("r--------"));
        }
        line(out, "  THE COPIES BEFORE THE ATTACK");
        line(out, "    shop server        3 files  writable");
        line(out, "    mounted backup     3 files  writable");
        line(out, "    versioned store    3 files  only adds, at time 100");
        line(out, "    read-only folder   3 files  r--------");
        line(out, "  THE ATTACK");
        line(out, "    at time 200 it encrypts every file it can write");
        Ransomware.Attack onLive = Ransomware.encryptInPlace(live);
        Ransomware.Attack onMounted = Ransomware.encryptInPlace(mounted);
        originals.forEach((name, data) -> store.append(name, "locked".getBytes(StandardCharsets.UTF_8), 200));
        Ransomware.Attack stopped = Ransomware.encryptInPlace(readOnly);
        boolean same = java.util.Arrays.equals(originals.get("orders.csv"),
                Files.readAllBytes(live.resolve("orders.csv")));
        boolean restored = originals.entrySet().stream()
                .allMatch(e -> store.latestBefore(e.getKey(), 199)
                        .map(v -> java.util.Arrays.equals(v, e.getValue()))
                        .orElse(false));
        line(out, "  AFTER THE ATTACK");
        line(out, "    shop server        " + onLive.encrypted() + " encrypted, orders.csv " + (same ? "intact" : "lost"));
        line(out, "    mounted backup     " + onMounted.encrypted() + " encrypted  <- WENT WITH THE DATA");
        line(out, "    versioned store    " + store.versionCount("orders.csv")
                + " versions: time 100 and 200");
        line(out, "    read-only folder   " + stopped.encrypted() + " encrypted, " + stopped.blocked() + " blocked");
        line(out, "  WHAT CAN BE RESTORED");
        line(out, "    versioned store, version before 200: all back " + restored);
        for (String name : originals.keySet()) {
            Files.setPosixFilePermissions(readOnly.resolve(name), PosixFilePermissions.fromString("rw-------"));
        }
        Ransomware.Attack again = Ransomware.encryptInPlace(readOnly);
        line(out, "    read-only folder, after the same user runs chmod u+w:");
        line(out, "      " + again.encrypted() + " encrypted  <- THE SHOP'S ACCOUNT COULD UNLOCK IT");
    }

    private static void act5Objectives(StringBuilder out, Path dir) throws IOException, GeneralSecurityException, BackupVault.WrongKeyException {
        line(out, "FIVE. Recovery point, recovery time, and a sealed backup.");
        long nightlyAt = 120;
        long hourlyAt = 900;
        long failure = 930;
        long detected = 950;
        long restore = 95;
        long perHour = 12;
        Objectives.Outcome nightly = Objectives.outcome(nightlyAt, failure, detected, restore, perHour);
        Objectives.Outcome hourly = Objectives.outcome(hourlyAt, failure, detected, restore, perHour);
        line(out, "  THE TIMELINE");
        line(out, "    " + Objectives.clock(nightlyAt) + "  the last nightly backup");
        line(out, "    " + Objectives.clock(failure) + "  the order database fails");
        line(out, "    " + Objectives.clock(detected) + "  someone notices");
        line(out, "    " + restore + " minutes for the restore, " + perHour + " orders an hour");
        line(out, "  THE RECOVERY POINT (RPO): THE WORK LOST");
        line(out, "    " + Objectives.clock(failure) + " - " + Objectives.clock(nightlyAt) + " = "
                + Objectives.clock(nightly.rpoMinutes()) + ", " + String.format("%.1f", nightly.rpoMinutes() / 60.0)
                + " h x " + perHour + " = " + nightly.ordersLost() + " orders lost");
        line(out, "  THE RECOVERY TIME (RTO): UNTIL THE SHOP IS BACK");
        line(out, "    " + Objectives.clock(detected) + " + " + restore + " min = " + Objectives.clock(detected + restore)
                + ", " + Objectives.clock(detected + restore) + " - " + Objectives.clock(failure) + " = "
                + Objectives.clock(nightly.rtoMinutes()));
        line(out, "  WITH AN HOURLY BACKUP, THE LAST AT " + Objectives.clock(hourlyAt));
        line(out, "    RPO " + Objectives.clock(failure) + " - " + Objectives.clock(hourlyAt) + " = "
                + Objectives.clock(hourly.rpoMinutes()) + ", " + String.format("%.1f", hourly.rpoMinutes() / 60.0)
                + " h x " + perHour + " = " + hourly.ordersLost() + " orders lost");
        line(out, "    RTO " + Objectives.clock(hourly.rtoMinutes()) + ", the same: backups do not speed restores");
        Path live = shop(dir);
        Map<String, byte[]> files = new TreeMap<>();
        for (String name : List.of("orders.csv", "customers.csv", "inventory.csv")) {
            files.put(name, Files.readAllBytes(live.resolve(name)));
        }
        byte[] key = BackupVault.newKey();
        byte[] archive = BackupVault.seal(key, "nightly-day-30", files);
        line(out, "  THE ARCHIVE TO SEAL");
        line(out, "    " + files.size() + " files, with customer names such as Ana");
        line(out, "    name nightly-day-30, a random 256-bit key");
        line(out, "    AES-256-GCM, the name bound in as associated data");
        line(out, "  THE SEALED ARCHIVE");
        line(out, "    shows the name Ana     "
                + new String(archive, StandardCharsets.ISO_8859_1).contains("Ana"));
        line(out, "    the right key          opens, " + BackupVault.open(key, "nightly-day-30", archive).size() + " files");
        String other;
        try {
            BackupVault.open(BackupVault.newKey(), "nightly-day-30", archive);
            other = "opens";
        } catch (BackupVault.WrongKeyException e) {
            other = "refused";
        }
        line(out, "    another key            " + other);
        String renamed;
        try {
            BackupVault.open(key, "nightly-day-31", archive);
            renamed = "opens";
        } catch (BackupVault.WrongKeyException e) {
            renamed = "refused, the name is sealed in";
        }
        line(out, "    named nightly-day-31   " + renamed);
        Path keyFile = dir.resolve("backup.key");
        BackupVault.writeKeyFile(keyFile, key);
        line(out, "    the key file           " + BackupVault.permissions(keyFile) + ", kept apart");
        line(out, "    <- A LOST KEY IS A LOST BACKUP: THE KEY NEEDS ITS OWN COPY");
    }
}
