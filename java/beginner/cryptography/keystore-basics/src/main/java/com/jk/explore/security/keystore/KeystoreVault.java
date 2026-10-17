package com.jk.explore.security.keystore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * A PKCS12 keystore holding AES keys under aliases. The file is encrypted and integrity-protected with a key made from a password, so
 * without the password it shows neither the keys nor their names.
 */
public class KeystoreVault {

    private final KeyStore store;
    private final char[] password;

    private KeystoreVault(KeyStore store, char[] password) {
        this.store = store;
        this.password = password.clone();
    }

    public static KeystoreVault create(char[] password) {
        try {
            KeyStore store = KeyStore.getInstance("PKCS12");
            store.load(null, password);
            return new KeystoreVault(store, password);
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Opens a keystore file, or throws {@link WrongPasswordException} if the password is wrong or the file was changed. */
    public static KeystoreVault open(Path file, char[] password) {
        try (InputStream in = Files.newInputStream(file)) {
            KeyStore store = KeyStore.getInstance("PKCS12");
            store.load(in, password);
            return new KeystoreVault(store, password);
        } catch (IOException e) {
            throw new WrongPasswordException("the keystore did not open: wrong password, or the file was changed");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public KeystoreVault put(String alias, byte[] keyBytes) {
        try {
            store.setEntry(alias, new KeyStore.SecretKeyEntry(new SecretKeySpec(keyBytes, "AES")),
                    new KeyStore.PasswordProtection(password));
            return this;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean has(String alias) {
        try {
            return store.containsAlias(alias);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The key stored under the alias, or null if there is none. */
    public SecretKey get(String alias) {
        try {
            return (SecretKey) store.getKey(alias, password);
        } catch (UnrecoverableKeyException e) {
            throw new WrongPasswordException("the key did not open with this password");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public void remove(String alias) {
        try {
            store.deleteEntry(alias);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public java.util.List<String> aliases() {
        try {
            return java.util.Collections.list(store.aliases());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public void save(Path file) {
        try (OutputStream out = Files.newOutputStream(file)) {
            store.store(out, password);
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Saves with a chosen number of rounds for the key that protects the file. The JDK reads these settings from security
     * properties, so they are set for the save and put back afterwards.
     */
    public void saveWithRounds(Path file, int rounds) {
        String[] names = {"keystore.pkcs12.keyPbeIterationCount", "keystore.pkcs12.certPbeIterationCount", "keystore.pkcs12.macIterationCount"};
        String[] before = new String[names.length];
        for (int i = 0; i < names.length; i++) {
            before[i] = Security.getProperty(names[i]);
            Security.setProperty(names[i], Integer.toString(rounds));
        }
        try {
            save(file);
        } finally {
            for (int i = 0; i < names.length; i++) {
                Security.setProperty(names[i], before[i] == null ? "" : before[i]);
            }
        }
    }

    /**
     * Reads, from the saved file itself, how many rounds of key derivation protect it: the file records the count as a DER
     * integer. Only a few common counts are looked for, which is enough to show what the file says.
     */
    public static java.util.List<Integer> allRoundsRecordedIn(byte[] file) {
        java.util.List<Integer> found = new java.util.ArrayList<>();
        int[] candidates = {10_000, 50_000, 100_000, 210_000, 600_000, 1_000_000};
        for (int rounds : candidates) {
            byte[] value = java.math.BigInteger.valueOf(rounds).toByteArray();
            byte[] der = new byte[2 + value.length];
            der[0] = 0x02;
            der[1] = (byte) value.length;
            System.arraycopy(value, 0, der, 2, value.length);
            if (Hex.contains(file, der)) {
                found.add(rounds);
            }
        }
        return found;
    }

    /** Thrown when a keystore or a key will not open with the password it was given. */
    public static class WrongPasswordException extends RuntimeException {
        public WrongPasswordException(String message) {
            super(message);
        }
    }
}
