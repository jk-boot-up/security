package com.jk.explore.security.keystore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Runs the online bank story in five numbered acts and prints exact, repeatable results. */
public class KeystoreDemo {

    static final char[] PASSWORD = "correct horse battery staple".toCharArray();
    static final byte[] ACCOUNT_NOTE = Hex.bytes("ANA-1: balance 1200");

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Keys and salts are random, so only facts that do not depend on them are printed. */
    public static String run() throws IOException {
        StringBuilder out = new StringBuilder();
        Path dir = Files.createTempDirectory("keystore-demo");
        try {
            act1HardCoded(out);
            act2KeyFile(out, dir);
            act3Keystore(out, dir);
            act4Derivation(out);
            act5RotationAndBill(out, dir);
        } finally {
            try (var files = Files.list(dir)) {
                for (Path p : files.toList()) {
                    Files.deleteIfExists(p);
                }
            }
            Files.deleteIfExists(dir);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1HardCoded(StringBuilder out) throws IOException {
        line(out, "ONE. A key written in the source code.");
        byte[] classFile;
        try (InputStream in = HardCodedKey.class.getResourceAsStream("HardCodedKey.class")) {
            classFile = in.readAllBytes();
        }
        line(out, "  the bank compiles a program with its key inside it");
        line(out, "  the key's text is found inside the compiled class file: " + Hex.contains(classFile, HardCodedKey.key())
                + "  <- ANYONE WITH THE PROGRAM HAS THE KEY");
        line(out, "  it is also in the source repository, every copy of the program and every build, for good");
        line(out, "  changing the key means building and shipping the program again");
    }

    private static void act2KeyFile(StringBuilder out, Path dir) throws IOException {
        line(out, "TWO. A key in a file outside the program.");
        byte[] key = Hex.bytes("file-based-statement-key-32bytes");
        Path open = KeyFile.writeWithDefaultPermissions(dir.resolve("open.key"), key);
        line(out, "  the key is no longer in the program, and is in a file with permissions " + KeyFile.permissions(open));
        line(out, "  readable by others on the machine: " + KeyFile.readableByOthers(open) + "  <- ANYONE WITH A LOGIN CAN READ IT");
        Path locked = KeyFile.writeOwnerOnly(dir.resolve("locked.key"), key);
        line(out, "  with permissions " + KeyFile.permissions(locked) + ", readable by others: " + KeyFile.readableByOthers(locked));
        line(out, "  but the file holds the key in the clear, so anyone who can read it, or its backup, has the key");
    }

    private static void act3Keystore(StringBuilder out, Path dir) {
        line(out, "THREE. A PKCS12 keystore: an encrypted file of keys.");
        byte[] key = Hex.bytes("keystore-statement-key-32-bytes!");
        Path file = dir.resolve("bank.p12");
        KeystoreVault.create(PASSWORD).put("statements-v1", key).save(file);
        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(file);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        line(out, "  the key is saved in bank.p12 under the name statements-v1");
        line(out, "  the key's own bytes appear in the file: " + Hex.contains(fileBytes, key));
        line(out, "  the name of the key appears in the file: " + Hex.contains(fileBytes, Hex.bytes("statements-v1")));
        KeystoreVault reopened = KeystoreVault.open(file, PASSWORD);
        line(out, "  opened with the password, the key comes back: " + Arrays.equals(reopened.get("statements-v1").getEncoded(), key));
        line(out, "  opened with a wrong password: " + wrongPassword(file));
        line(out, "  the password makes the key that protects the file, and the file records the rounds: "
                + KeystoreVault.allRoundsRecordedIn(fileBytes));
        Path raised = dir.resolve("bank-raised.p12");
        KeystoreVault.create(PASSWORD).put("statements-v1", key).saveWithRounds(raised, Derivation.PBKDF2_ROUNDS);
        line(out, "  saved again with the setting raised to " + Derivation.PBKDF2_ROUNDS + ", the file records: "
                + KeystoreVault.allRoundsRecordedIn(readAll(raised)));
        line(out, "  the raised setting reached one place and not another: check what the file records, not only the settings");
    }

    private static void act4Derivation(StringBuilder out) {
        line(out, "FOUR. Making keys from other secrets.");
        byte[] salt = Hex.bytes("per-user-salt-1");
        byte[] a = Derivation.fromPassword(PASSWORD, salt, Derivation.PBKDF2_ROUNDS);
        byte[] b = Derivation.fromPassword(PASSWORD, salt, Derivation.PBKDF2_ROUNDS);
        byte[] c = Derivation.fromPassword(PASSWORD, Hex.bytes("per-user-salt-2"), Derivation.PBKDF2_ROUNDS);
        line(out, "  a key made from a password and a salt with PBKDF2, " + Derivation.PBKDF2_ROUNDS + " rounds: " + a.length + " bytes");
        line(out, "  the same password and salt give the same key: " + Arrays.equals(a, b));
        line(out, "  another salt gives another key: " + !Arrays.equals(a, c));
        byte[] master = Hex.bytes("one-strong-master-key-of-32-bytes");
        byte[] statements = Derivation.forPurpose(master, salt, "statements");
        byte[] tokens = Derivation.forPurpose(master, salt, "login tokens");
        line(out, "  from one master key, HKDF makes a separate key for each purpose: " + !Arrays.equals(statements, tokens));
        line(out, "  asking again for the same purpose gives the same key: "
                + Arrays.equals(statements, Derivation.forPurpose(master, salt, "statements")));
        line(out, "  a key made from a weak password is only as strong as the password");
    }

    private static void act5RotationAndBill(StringBuilder out, Path dir) {
        line(out, "FIVE. Replacing a key, and the bill.");
        KeystoreVault vault = KeystoreVault.create(PASSWORD);
        vault.put("statements-v1", Hex.bytes("keystore-statement-key-32-bytes!"));
        byte[] oldMessage = Messages.seal(vault, "statements-v1", ACCOUNT_NOTE);
        vault.put("statements-v2", Hex.bytes("a-newer-statement-key-32-bytes!!"));
        byte[] newMessage = Messages.seal(vault, "statements-v2", ACCOUNT_NOTE);
        line(out, "  a new key, statements-v2, is added; the old message names " + Messages.keyNameOf(oldMessage)
                + " and the new one " + Messages.keyNameOf(newMessage));
        line(out, "  both still open: " + Hex.text(Messages.open(vault, oldMessage)) + " and " + Hex.text(Messages.open(vault, newMessage)));
        vault.remove("statements-v1");
        line(out, "  after statements-v1 is removed, the old message: " + cannotOpen(vault, oldMessage));
        line(out, "  so old messages must be sealed again with the new key before the old key goes");
        line(out, "  the keystore's password has to be kept somewhere too, and a lost keystore file is lost data");
        line(out, "  the default rounds of the file are far below the " + Derivation.PBKDF2_ROUNDS
                + " now recommended, so raise them and choose a strong password");
    }

    private static byte[] readAll(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String wrongPassword(Path file) {
        try {
            KeystoreVault.open(file, "wrong".toCharArray());
            return "it opened";
        } catch (KeystoreVault.WrongPasswordException e) {
            return "refused";
        }
    }

    private static String cannotOpen(KeystoreVault vault, byte[] message) {
        try {
            Messages.open(vault, message);
            return "opened";
        } catch (Messages.CannotOpenException e) {
            return "refused, its key is gone";
        }
    }
}
