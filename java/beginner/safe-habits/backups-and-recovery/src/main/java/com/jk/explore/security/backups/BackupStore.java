package com.jk.explore.security.backups;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A folder of snapshots. Each snapshot holds a manifest of the whole state at that moment and the files it stored: a full
 * snapshot stores every file, an incremental one only the files that are new or changed since its parent.
 */
final class BackupStore {

    private BackupStore() {
    }

    private static Path snapshot(Path root, String name) {
        return root.resolve(name);
    }

    static void full(Path source, Path root, String name) throws IOException {
        write(source, root, name, "", Manifest.of(source).entries().keySet().stream().toList());
    }

    static void incremental(Path source, Path root, String name, String parent) throws IOException {
        Manifest before = state(root, parent);
        Manifest now = Manifest.of(source);
        List<String> changed = new ArrayList<>();
        now.entries().forEach((file, hash) -> {
            if (!hash.equals(before.entries().get(file))) {
                changed.add(file);
            }
        });
        write(source, root, name, parent, changed);
    }

    private static void write(Path source, Path root, String name, String parent, List<String> files) throws IOException {
        Path snapshot = snapshot(root, name);
        for (String file : files) {
            Path target = snapshot.resolve("data").resolve(file);
            Files.createDirectories(target.getParent());
            Files.copy(source.resolve(file), target, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.createDirectories(snapshot);
        Files.writeString(snapshot.resolve("PARENT.txt"), parent);
        Files.writeString(snapshot.resolve(Manifest.FILE_NAME), Manifest.of(source).toText());
    }

    static Manifest state(Path root, String name) throws IOException {
        return Manifest.parse(Files.readString(snapshot(root, name).resolve(Manifest.FILE_NAME)));
    }

    static int storedFiles(Path root, String name) throws IOException {
        Path data = snapshot(root, name).resolve("data");
        if (!Files.isDirectory(data)) {
            return 0;
        }
        try (Stream<Path> walk = Files.walk(data)) {
            return (int) walk.filter(Files::isRegularFile).count();
        }
    }

    /** The newest copy of a file in the chain that ends at this snapshot, if any snapshot stored it. */
    private static Path locate(Path root, String name, String file) throws IOException {
        String current = name;
        while (!current.isEmpty()) {
            Path candidate = snapshot(root, current).resolve("data").resolve(file);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = Files.readString(snapshot(root, current).resolve("PARENT.txt"));
        }
        return null;
    }

    /** Checks the stored files against the manifest without restoring anything: tampering and loss show up here. */
    static Manifest.Problems verifyStored(Path root, String name) throws IOException {
        List<String> missing = new ArrayList<>();
        List<String> changed = new ArrayList<>();
        for (Map.Entry<String, String> entry : state(root, name).entries().entrySet()) {
            Path found = locate(root, name, entry.getKey());
            if (found == null) {
                missing.add(entry.getKey());
            } else if (!Sha256.of(found).equals(entry.getValue())) {
                changed.add(entry.getKey());
            }
        }
        return new Manifest.Problems(missing, changed, List.of());
    }

    /** Restores the state of a snapshot into a folder and returns what is wrong with it, compared with the manifest. */
    static Manifest.Problems restore(Path root, String name, Path target) throws IOException {
        Manifest state = state(root, name);
        for (String file : state.entries().keySet()) {
            Path found = locate(root, name, file);
            if (found != null) {
                Path to = target.resolve(file);
                Files.createDirectories(to.getParent());
                Files.copy(found, to, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return state.check(target);
    }
}
