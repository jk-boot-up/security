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

    private static void act1FullAndIncremental(StringBuilder out, Path dir) throws IOException {
        line(out, "ONE. Full and incremental backups, with a manifest.");
        Path live = shop(dir);
        Path root = dir.resolve("backups");
        BackupStore.full(live, root, "day1");
        line(out, "  day 1, a full backup: " + BackupStore.storedFiles(root, "day1") + " files stored");
        Files.writeString(live.resolve("orders.csv"), ORDERS + "1003,cy,99\n");
        Files.writeString(live.resolve("refunds.csv"), "order,amount\n1002,15\n");
        BackupStore.incremental(live, root, "day2", "day1");
        line(out, "  day 2, an incremental backup: " + BackupStore.storedFiles(root, "day2") + " files stored (orders changed, refunds new)");
        Files.writeString(live.resolve("customers.csv"), "id,name\nana,Ana\nben,Ben\ncy,Cy\n");
        BackupStore.incremental(live, root, "day3", "day2");
        line(out, "  day 3, an incremental backup: " + BackupStore.storedFiles(root, "day3") + " file stored");
        line(out, "  the manifest of day 3 lists " + BackupStore.state(root, "day3").entries().size() + " files, each with its SHA-256");
        Path restored = dir.resolve("restored");
        Manifest.Problems problems = BackupStore.restore(root, "day3", restored);
        line(out, "  restoring day 3 takes the newest copy of each file from the chain; problems found: " + problems.count());
        line(out, "  incremental backups are small and quick, and a restore needs the whole chain");
    }

    private static void act2Trust(StringBuilder out, Path dir) throws IOException {
        line(out, "TWO. Can the backup be trusted? Only a restore test says.");
        Path live = shop(dir);
        Path root = dir.resolve("backups");
        BackupStore.full(live, root, "day1");
        Files.writeString(live.resolve("orders.csv"), ORDERS + "1003,cy,99\n");
        BackupStore.incremental(live, root, "day2", "day1");
        Path stored = root.resolve("day2").resolve("data").resolve("orders.csv");
        byte[] bytes = Files.readAllBytes(stored);
        bytes[bytes.length - 3] ^= 0x01;
        Files.write(stored, bytes);
        line(out, "  one byte of a stored file is changed: files found changed " + BackupStore.verifyStored(root, "day2").changed().size() + ", and the manifest names it");
        Files.delete(root.resolve("day1").resolve("data").resolve("inventory.csv"));
        line(out, "  a stored file is deleted: files found missing " + BackupStore.verifyStored(root, "day2").missing().size());
        Path partialLive = dir.resolve("partial");
        Files.createDirectories(partialLive);
        Files.writeString(partialLive.resolve("customers.csv"), "id,name\nana,Ana\n");
        BackupStore.full(partialLive, root, "partial");
        RestoreTest.Result partial = RestoreTest.run(root, "partial", dir.resolve("scratch-partial"));
        line(out, "  a backup job skipped the orders file because it was in use; the manifest checks out: " + partial.manifestOk());
        line(out, "  the restore test asks for a usable orders file: " + partial.ordersUsable() + "  <- A BACKUP NOBODY RESTORED IS A HOPE");
        BackupStore.full(live, root, "good");
        RestoreTest.Result good = RestoreTest.run(root, "good", dir.resolve("scratch-good"));
        line(out, "  a good backup passes the restore test: " + good.passed() + ", with " + good.orders() + " orders");
    }

    private static void act3RulesAndRetention(StringBuilder out) {
        line(out, "THREE. The 3-2-1 rule, and how long to keep backups.");
        var careless = List.of(
                new ThreeTwoOne.Copy("shop server", "ssd", false, false),
                new ThreeTwoOne.Copy("same server, other folder", "ssd", false, false));
        var careful = List.of(
                new ThreeTwoOne.Copy("shop server", "ssd", false, false),
                new ThreeTwoOne.Copy("office NAS", "disk", false, false),
                new ThreeTwoOne.Copy("cloud bucket in another region", "object store", true, true));
        ThreeTwoOne.Result bad = ThreeTwoOne.check(careless);
        ThreeTwoOne.Result good = ThreeTwoOne.check(careful);
        line(out, "  a copy in another folder of the same server: three copies " + bad.threeCopies() + ", two media " + bad.twoMedia()
                + ", one off site " + bad.oneOffsite() + ", one unchangeable " + bad.oneImmutable() + ": " + bad.score() + " of 4");
        line(out, "  the server, a NAS and an off-site locked bucket: three copies " + good.threeCopies() + ", two media " + good.twoMedia()
                + ", one off site " + good.oneOffsite() + ", one unchangeable " + good.oneImmutable() + ": " + good.score() + " of 4");
        LocalDate today = LocalDate.of(2026, 2, 4);
        List<LocalDate> days = new ArrayList<>();
        for (int i = 399; i >= 0; i--) {
            days.add(today.minusDays(i));
        }
        var kept = Retention.keep(days, today, 7, 4, 12);
        line(out, "  400 daily snapshots, keeping 7 daily, 4 weekly and 12 monthly: " + kept.size() + " kept, " + (days.size() - kept.size()) + " pruned");
        line(out, "  the oldest kept is " + kept.first() + ": a year of history in " + kept.size() + " snapshots instead of 400");
        line(out, "  keeping less than the law or the business needs is a risk too, and so is keeping personal data for ever");
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
        Ransomware.Attack onLive = Ransomware.encryptInPlace(live);
        Ransomware.Attack onMounted = Ransomware.encryptInPlace(mounted);
        line(out, "  ransomware runs on the shop server and encrypts " + onLive.encrypted() + " files in place");
        line(out, "  the orders file still reads as before: " + java.util.Arrays.equals(originals.get("orders.csv"), Files.readAllBytes(live.resolve("orders.csv"))));
        line(out, "  the backup folder was mounted and writable, so it is encrypted too: " + onMounted.encrypted() + " files  <- THE BACKUP WENT WITH THE DATA");
        originals.forEach((name, data) -> store.append(name, "locked".getBytes(StandardCharsets.UTF_8), 200));
        boolean restored = originals.entrySet().stream().allMatch(e -> store.latestBefore(e.getKey(), 199).map(v -> java.util.Arrays.equals(v, e.getValue())).orElse(false));
        line(out, "  a store that only adds versions took the attacker's writes as new versions: " + store.versionCount("orders.csv") + " versions of the orders file");
        line(out, "  restoring the version from before the attack gives every original file back: " + restored);
        Path readOnly = dir.resolve("read-only-backup");
        Files.createDirectories(readOnly);
        for (String name : originals.keySet()) {
            Files.write(readOnly.resolve(name), originals.get(name));
            Files.setPosixFilePermissions(readOnly.resolve(name), PosixFilePermissions.fromString("r--------"));
        }
        Ransomware.Attack stopped = Ransomware.encryptInPlace(readOnly);
        line(out, "  files marked read-only: encrypted " + stopped.encrypted() + ", blocked " + stopped.blocked());
        for (String name : originals.keySet()) {
            Files.setPosixFilePermissions(readOnly.resolve(name), PosixFilePermissions.fromString("rw-------"));
        }
        Ransomware.Attack again = Ransomware.encryptInPlace(readOnly);
        line(out, "  but the same user can make them writable again: encrypted " + again.encrypted() + "  <- REAL PROTECTION COMES FROM A COPY THE SHOP'S OWN ACCOUNT CANNOT CHANGE");
    }

    private static void act5Objectives(StringBuilder out, Path dir) throws IOException, GeneralSecurityException, BackupVault.WrongKeyException {
        line(out, "FIVE. How much can be lost, how long it takes, and backups as secrets.");
        Objectives.Outcome nightly = Objectives.outcome(120, 930, 950, 95, 12);
        line(out, "  nightly backup at 02:00, failure at 15:30, noticed at 15:50, restore takes 95 minutes");
        line(out, "  recovery point: " + Objectives.clock(nightly.rpoMinutes()) + " of work lost, about " + nightly.ordersLost() + " orders at 12 an hour");
        line(out, "  recovery time: " + Objectives.clock(nightly.rtoMinutes()) + " until the shop is back");
        Objectives.Outcome hourly = Objectives.outcome(900, 930, 950, 95, 12);
        line(out, "  with an hourly backup the recovery point is " + Objectives.clock(hourly.rpoMinutes()) + " and about " + hourly.ordersLost() + " orders; the recovery time is the same");
        Path live = shop(dir);
        Map<String, byte[]> files = new TreeMap<>();
        for (String name : List.of("orders.csv", "customers.csv", "inventory.csv")) {
            files.put(name, Files.readAllBytes(live.resolve(name)));
        }
        byte[] key = BackupVault.newKey();
        byte[] archive = BackupVault.seal(key, "nightly-day-30", files);
        line(out, "  a backup holds every customer's data, so it is sealed with AES-256-GCM");
        line(out, "  the archive shows a customer name: " + new String(archive, StandardCharsets.ISO_8859_1).contains("Ana"));
        line(out, "  the right key opens it: " + (BackupVault.open(key, "nightly-day-30", archive).size() == files.size()));
        try {
            BackupVault.open(BackupVault.newKey(), "nightly-day-30", archive);
            line(out, "  another key opens it: true");
        } catch (BackupVault.WrongKeyException e) {
            line(out, "  another key: refused");
        }
        try {
            BackupVault.open(key, "nightly-day-31", archive);
            line(out, "  the same archive offered as the next night's: opens");
        } catch (BackupVault.WrongKeyException e) {
            line(out, "  the same archive offered as the next night's: refused, because its name is sealed in with it");
        }
        Path keyFile = dir.resolve("backup.key");
        BackupVault.writeKeyFile(keyFile, key);
        line(out, "  the key file has permissions " + BackupVault.permissions(keyFile) + ", and lives apart from the backups");
        line(out, "  a lost key is a lost backup, so the key needs its own safe copy");
    }
}
