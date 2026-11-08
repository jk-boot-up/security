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
        line(out, "  BEFORE COMPILING (the source code):");
        line(out, "    KEY_TEXT = \"" + HardCodedKey.KEY_TEXT + "\"");
        line(out, "  AFTER COMPILING (HardCodedKey.class, the program):");
        line(out, "    key bytes  " + Hex.first16(HardCodedKey.key()));
        line(out, "    in the class file: " + Hex.contains(classFile, HardCodedKey.key()) + "  <- EVERYONE HAS THE KEY");
        line(out, "  also in the repository and every build, for good");
        line(out, "  a new key means building and shipping the program again");
    }

    private static void act2KeyFile(StringBuilder out, Path dir) throws IOException {
        line(out, "TWO. A key in a file outside the program.");
        byte[] key = Hex.bytes("file-based-statement-key-32bytes");
        Path open = KeyFile.writeWithDefaultPermissions(dir.resolve("open.key"), key);
        line(out, "  BEFORE THE FIX (a new file, default permissions):");
        permissions(out, open);
        line(out, "    readable by others: " + KeyFile.readableByOthers(open) + "  <- ANYONE WITH A LOGIN");
        Path locked = KeyFile.writeOwnerOnly(dir.resolve("locked.key"), key);
        line(out, "  AFTER THE FIX (owner only):");
        permissions(out, locked);
        line(out, "    readable by others: " + KeyFile.readableByOthers(locked));
        line(out, "  WHAT THE FILE HOLDS (in the clear):");
        line(out, "    " + Hex.text(KeyFile.read(locked)));
        line(out, "  whoever reads the file, or its backup, has the key");
    }

    private static void permissions(StringBuilder out, Path file) throws IOException {
        String p = KeyFile.permissions(file);
        line(out, "    " + file.getFileName() + "  " + p);
        line(out, "    owner " + p.substring(0, 3) + "   group " + p.substring(3, 6) + "   others " + p.substring(6));
    }

    private static void act3Keystore(StringBuilder out, Path dir) {
        line(out, "THREE. A PKCS12 keystore: an encrypted file of keys.");
        byte[] key = Hex.bytes("keystore-statement-key-32-bytes!");
        Path file = dir.resolve("bank.p12");
        KeystoreVault.create(PASSWORD).put("statements-v1", key).save(file);
        byte[] fileBytes = readAll(file);
        line(out, "  BEFORE SAVING (what goes in):");
        line(out, "    name      statements-v1");
        line(out, "    key       " + Hex.first16(key));
        line(out, "    password  " + new String(PASSWORD));
        line(out, "  AFTER SAVING (bank.p12):");
        line(out, "    the key's bytes are in the file: " + Hex.contains(fileBytes, key) + "  <- HIDDEN");
        line(out, "    the name, as UTF-16, is in the file: " + Hex.contains(fileBytes, Hex.utf16("statements-v1"))
                + "  <- A LABEL");
        KeystoreVault reopened = KeystoreVault.open(file, PASSWORD);
        line(out, "  OPENING IT:");
        line(out, "    with the password, the key comes back: "
                + Arrays.equals(reopened.get("statements-v1").getEncoded(), key));
        line(out, "    with a wrong password: " + wrongPassword(file));
        line(out, "  BEFORE THE FIX (rounds in the file: key lock, check):");
        line(out, "    default          " + KeystoreVault.allRoundsRecordedIn(fileBytes));
        Path raised = dir.resolve("bank-raised.p12");
        KeystoreVault.create(PASSWORD).put("statements-v1", key).saveWithRounds(raised, Derivation.PBKDF2_ROUNDS);
        line(out, "    raised at save   " + KeystoreVault.allRoundsRecordedIn(readAll(raised))
                + "  <- LOCK UNCHANGED");
        Path fixed = dir.resolve("bank-fixed.p12");
        KeystoreVault.create(PASSWORD).put("statements-v1", key, Derivation.PBKDF2_ROUNDS)
                .saveWithRounds(fixed, Derivation.PBKDF2_ROUNDS);
        line(out, "  AFTER THE FIX (rounds set when the key is put, too):");
        line(out, "    put and save     " + KeystoreVault.allRoundsRecordedIn(readAll(fixed)));
        line(out, "    still opens with the password: "
                + Arrays.equals(KeystoreVault.open(fixed, PASSWORD).get("statements-v1").getEncoded(), key));
    }

    private static void act4Derivation(StringBuilder out) {
        line(out, "FOUR. Making keys from other secrets.");
        byte[] salt = Hex.bytes("per-user-salt-1");
        int rounds = Derivation.PBKDF2_ROUNDS;
        byte[] a = Derivation.fromPassword(PASSWORD, salt, rounds);
        Pbkdf2Steps.Steps steps = Pbkdf2Steps.derive(new String(PASSWORD), salt, rounds);
        line(out, "  BEFORE DERIVING (the inputs of PBKDF2):");
        line(out, "    password  " + new String(PASSWORD));
        line(out, "    salt      " + Hex.text(salt));
        line(out, "    rounds    " + rounds);
        line(out, "  STEP 1. U1 = HMAC-SHA-256(password, salt + 00000001)");
        line(out, "    U1        " + Hex.first16(steps.u1()));
        line(out, "  STEP 2. EACH ROUND: U(n) = HMAC-SHA-256(password, U(n-1))");
        line(out, "    U2        " + Hex.first16(steps.u2()));
        line(out, "  STEP 3. KEY = U1 xor U2 xor ... xor U" + rounds);
        line(out, "  AFTER DERIVING (the key, " + a.length + " bytes):");
        line(out, "    key       " + Hex.first16(steps.key()));
        line(out, "    the same key the JDK's PBKDF2 gave: " + Arrays.equals(a, steps.key()));
        byte[] c = Derivation.fromPassword(PASSWORD, Hex.bytes("per-user-salt-2"), rounds);
        line(out, "  ANOTHER SALT (per-user-salt-2): ANOTHER KEY");
        line(out, "    key       " + Hex.first16(c));
        byte[] master = Hex.bytes("one-strong-master-key-of-32-bytes");
        byte[] statements = Derivation.forPurpose(master, salt, "statements");
        byte[] tokens = Derivation.forPurpose(master, salt, "login tokens");
        byte[] again = Derivation.forPurpose(master, salt, "statements");
        line(out, "  HKDF: ONE MASTER KEY, A KEY FOR EACH PURPOSE:");
        line(out, "    statements    " + Hex.first16(statements));
        line(out, "    login tokens  " + Hex.first16(tokens));
        line(out, "    asked again for statements, the same key: " + Arrays.equals(statements, again));
        line(out, "  a weak password makes a weak key, whatever the rounds");
    }

    private static void act5RotationAndBill(StringBuilder out, Path dir) {
        line(out, "FIVE. Replacing a key, and the bill.");
        KeystoreVault vault = KeystoreVault.create(PASSWORD);
        vault.put("statements-v1", Hex.bytes("keystore-statement-key-32-bytes!"));
        byte[] oldMessage = Messages.seal(vault, "statements-v1", ACCOUNT_NOTE);
        int nameLength = oldMessage[0];
        int sealedLength = oldMessage.length - 1 - nameLength - 12;
        line(out, "  A STORED MESSAGE (" + oldMessage.length + " bytes), PART BY PART:");
        line(out, "    1 byte    " + Hex.of(new byte[] {oldMessage[0]}) + "             the name's length, " + nameLength);
        line(out, "    " + nameLength + " bytes  " + Messages.keyNameOf(oldMessage) + "  the key's name");
        line(out, "    12 bytes  nonce          new for every message");
        line(out, "    " + sealedLength + " bytes  cipher text    " + ACCOUNT_NOTE.length + ", and a 16-byte tag");
        vault.put("statements-v2", Hex.bytes("a-newer-statement-key-32-bytes!!"));
        byte[] newMessage = Messages.seal(vault, "statements-v2", ACCOUNT_NOTE);
        line(out, "  BEFORE REMOVING statements-v1 (statements-v2 added):");
        line(out, "    old, names " + Messages.keyNameOf(oldMessage) + ": " + Hex.text(Messages.open(vault, oldMessage)));
        line(out, "    new, names " + Messages.keyNameOf(newMessage) + ": " + Hex.text(Messages.open(vault, newMessage)));
        vault.remove("statements-v1");
        line(out, "  AFTER REMOVING statements-v1:");
        line(out, "    old, names statements-v1: " + cannotOpen(vault, oldMessage) + "  <- LOST");
        line(out, "    new, names statements-v2: " + Hex.text(Messages.open(vault, newMessage)));
        line(out, "  so re-seal old messages before the old key goes");
        line(out, "  THE BILL:");
        line(out, "    the keystore's password needs a home too");
        line(out, "    a lost keystore file is lost data: back it up");
        line(out, "    default rounds 10000, far below " + Derivation.PBKDF2_ROUNDS + ": raise them");
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
            return "refused";
        }
    }
}
