package com.jk.explore.security.hashingspring;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;

/** Runs the online store story in six numbered acts, with Spring Security's password encoders, and prints repeatable results. */
public class SpringHashingDemo {

    static final String ARGON2_ID = "argon2@SpringSecurity_v5_8";

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        act1WarningLabels(out);
        act2OneCall(out);
        act3FourAlgorithms(out);
        act4SameAttack(out);
        act5Migration(out);
        act6Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static Map<String, String> encodeAll(PasswordEncoder encoder) {
        Map<String, String> table = new LinkedHashMap<>();
        Customers.PASSWORDS.forEach((user, password) -> table.put(user, encoder.encode(password)));
        return table;
    }

    private static String names(Map<String, String> found) {
        return found.isEmpty() ? "nobody" : found.keySet().stream().collect(Collectors.joining(", "));
    }

    /** The text up to and including the closing brace, such as {bcrypt}. */
    static String prefix(String stored) {
        return stored.substring(0, stored.indexOf('}') + 1);
    }

    /** Builds an encoder that knows only the algorithms listed, so nothing else can be named in a stored value. */
    static DelegatingPasswordEncoder allowList(String idForEncode) {
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("bcrypt", new BCryptPasswordEncoder());
        encoders.put(ARGON2_ID, Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        return new DelegatingPasswordEncoder(idForEncode, encoders);
    }

    @SuppressWarnings({"deprecation", "removal"})
    private static void act1WarningLabels(StringBuilder out) {
        line(out, "ONE. Two the library marks as deprecated.");
        PasswordEncoder noop = NoOpPasswordEncoder.getInstance();
        line(out, "  NoOpPasswordEncoder stores ana's password as: " + noop.encode("welcome1") + "  <- THE PASSWORD ITSELF");
        PasswordEncoder sha = new MessageDigestPasswordEncoder("SHA-256");
        String ana = sha.encode("welcome1");
        String ben = sha.encode("welcome1");
        line(out, "  MessageDigestPasswordEncoder, SHA-256 with a salt, gives ana and ben the same value: " + ana.equals(ben));
        line(out, "  but it is one cheap step per guess: " + new Attacker(Customers.WORDLIST)
                .crack(encodeAll(sha), sha).guesses() + " guesses crack "
                + new Attacker(Customers.WORDLIST).crack(encodeAll(sha), sha).found().size() + " of "
                + Customers.PASSWORDS.size() + "  <- FAST");
    }

    private static void act2OneCall(StringBuilder out) {
        line(out, "TWO. One call: the delegating encoder.");
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        String ana = encoder.encode("welcome1");
        String ben = encoder.encode("welcome1");
        line(out, "  ana's stored text begins " + prefix(ana) + " and is " + ana.length() + " characters long");
        line(out, "  same password, same stored text: " + ana.equals(ben) + "  <- A SALT IS ADDED FOR YOU");
        line(out, "  ana logs in with the right password: " + encoder.matches("welcome1", ana));
        line(out, "  ana logs in with a wrong one: " + encoder.matches("guess", ana));
    }

    private static void act3FourAlgorithms(StringBuilder out) {
        line(out, "THREE. Four algorithms, one interface.");
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        Pbkdf2PasswordEncoder pbkdf2 = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        SCryptPasswordEncoder scrypt = SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8();
        Argon2PasswordEncoder argon2 = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        String b = bcrypt.encode("welcome1");
        line(out, "  bcrypt:  cost " + b.substring(4, 6) + " (" + (1 << Integer.parseInt(b.substring(4, 6)))
                + " rounds), text begins " + b.substring(0, 7));
        line(out, "  pbkdf2:  " + pbkdf2.encode("welcome1").length() + " characters, matches: "
                + pbkdf2.matches("welcome1", pbkdf2.encode("welcome1")));
        line(out, "  scrypt:  " + scrypt.encode("welcome1").length() + " characters, matches: "
                + scrypt.matches("welcome1", scrypt.encode("welcome1")));
        String a = argon2.encode("welcome1");
        String settings = a.split("\\$")[3];
        line(out, "  argon2:  settings " + settings + ", matches: " + argon2.matches("welcome1", a));
    }

    private static void act4SameAttack(StringBuilder out) {
        line(out, "FOUR. The same attack, at two costs.");
        Attacker attacker = new Attacker(Customers.WORDLIST);
        BCryptPasswordEncoder cheap = new BCryptPasswordEncoder(4);
        BCryptPasswordEncoder strong = new BCryptPasswordEncoder(10);
        Attacker.Cracked low = attacker.crack(encodeAll(cheap), cheap);
        Attacker.Cracked high = attacker.crack(encodeAll(strong), strong);
        line(out, "  bcrypt cost 4:  cracked " + low.found().size() + " of " + Customers.PASSWORDS.size() + " in "
                + low.guesses() + " guesses");
        line(out, "  bcrypt cost 10: cracked " + high.found().size() + " of " + Customers.PASSWORDS.size() + " in "
                + high.guesses() + " guesses");
        line(out, "  each guess at cost 10 is " + (1 << (10 - 4)) + " times the work of cost 4");
        line(out, "  cracked: " + names(high.found()) + "  <- COST HELPS, WEAK PASSWORDS STILL FALL");
    }

    private static void act5Migration(StringBuilder out) {
        line(out, "FIVE. Moving to a stronger algorithm without asking anyone.");
        DelegatingPasswordEncoder old = allowList("bcrypt");
        DelegatingPasswordEncoder current = allowList(ARGON2_ID);
        String erin = old.encode("letmein");
        line(out, "  erin's stored text begins " + prefix(erin));
        line(out, "  the shop switches its default to argon2; erin logs in: " + current.matches("letmein", erin));
        line(out, "  the encoder says the stored text should be upgraded: " + current.upgradeEncoding(erin));
        String upgraded = current.encode("letmein");
        line(out, "  after login it is stored again, beginning " + prefix(upgraded));
        line(out, "  upgrade needed now: " + current.upgradeEncoding(upgraded));
    }

    private static void act6Bill(StringBuilder out) {
        line(out, "SIX. The bill: an open list, and memory.");
        PasswordEncoder open = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        line(out, "  the default delegating encoder accepts {noop}letmein for letmein: "
                + open.matches("letmein", "{noop}letmein") + "  <- A WRITER TO THE TABLE CAN PLANT IT");
        DelegatingPasswordEncoder strict = allowList(ARGON2_ID);
        try {
            strict.matches("letmein", "{noop}letmein");
            line(out, "  an allow-list encoder accepts it");
        } catch (IllegalArgumentException e) {
            line(out, "  an allow-list encoder refuses {noop}: no encoder mapped for that id");
        }
        String stored = strict.encode("welcome1");
        String settings = stored.substring(stored.indexOf('$')).split("\\$")[3];
        int memoryKiB = Integer.parseInt(settings.split(",")[0].substring(2));
        line(out, "  one argon2 hash uses " + memoryKiB + " KiB, " + (memoryKiB / 1024) + " MiB, of memory");
        line(out, "  100 logins at once need " + (100 * memoryKiB / 1024) + " MiB  <- MEMORY IS THE PRICE");
    }
}
