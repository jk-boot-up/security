package com.jk.explore.security.hashingspring;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder.BCryptVersion;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
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
        act1WeakEncoders(out);
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

    private static void act1WeakEncoders(StringBuilder out) {
        line(out, "ONE. Two weak encoders, written out here.");
        line(out, "  BEFORE HASHING");
        line(out, "    ana's password  welcome1");
        PasswordEncoder plain = new PlainTextEncoder();
        line(out, "  AFTER HASHING: PLAIN TEXT");
        line(out, "    stored          " + plain.encode("welcome1") + "  <- THE PASSWORD ITSELF");
        PasswordEncoder fast = new FastSaltedSha256Encoder(new RepeatableSalts());
        String ana = fast.encode("welcome1");
        String ben = fast.encode("welcome1");
        line(out, "  AFTER HASHING: FAST SHA-256 WITH A SALT");
        line(out, "    salt            " + ana.substring(0, ana.indexOf('$')));
        line(out, "    hash            " + ana.substring(ana.indexOf('$') + 1, ana.indexOf('$') + 33) + "...");
        line(out, "    ana and ben the same: " + ana.equals(ben));
        Attacker.Cracked cracked = new Attacker(Customers.WORDLIST).crack(encodeAll(fast), fast);
        line(out, "  THE ATTACK");
        line(out, "    " + cracked.guesses() + " guesses crack " + cracked.found().size() + " of " + Customers.PASSWORDS.size()
                + ", one cheap step each  <- FAST");
        line(out, "  the library keeps these only for old systems: deprecated");
    }

    private static void act2OneCall(StringBuilder out) {
        line(out, "TWO. One call: the delegating encoder.");
        PasswordEncoder factory = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        // The factory's default, bcrypt at cost 10, with repeatable salts so the values shown never change.
        PasswordEncoder encoder = new DelegatingPasswordEncoder("bcrypt",
                Map.of("bcrypt", new BCryptPasswordEncoder(BCryptVersion.$2A, 10, new RepeatableSalts())));
        String ana = encoder.encode("welcome1");
        line(out, "  BEFORE HASHING");
        line(out, "    ana's password  welcome1");
        line(out, "  AFTER HASHING: ANA'S STORED TEXT, " + ana.length() + " CHARACTERS");
        BcryptSteps.Parts parts = BcryptSteps.parts(ana);
        line(out, "    " + parts.id() + parts.version() + parts.cost() + "$");
        line(out, "    " + parts.salt() + parts.hash());
        line(out, "  THE PARTS");
        line(out, "    id       " + parts.id() + "  picks the algorithm");
        line(out, "    version  " + parts.version() + "      bcrypt's version");
        line(out, "    cost     " + parts.cost() + "        2^" + parts.cost() + " = " + parts.rounds() + " rounds");
        line(out, "    salt     " + parts.salt());
        line(out, "    hash     " + parts.hash());
        String ben = encoder.encode("welcome1");
        line(out, "  BEN, THE SAME PASSWORD");
        line(out, "    salt     " + BcryptSteps.parts(ben).salt());
        line(out, "    same stored text as ana: " + ana.equals(ben) + "  <- A SALT IS ADDED FOR YOU");
        for (String typed : new String[] {"welcome1", "guess"}) {
            line(out, "  LOGIN: ANA TYPES " + typed);
            if (typed.equals("welcome1")) {
                line(out, "    STEP 1. read cost " + parts.cost() + " and the salt from the stored text");
            }
            String again = BcryptSteps.hashAgain(typed, parts);
            line(out, "    STEP 2. hash it again  " + again);
            line(out, "    STEP 3. same as the stored hash: " + BcryptSteps.sameHash(again, parts));
        }
        line(out, "  the library's matches says the same: " + factory.matches("welcome1", ana) + ", "
                + factory.matches("guess", ana));
    }

    private static void act3FourAlgorithms(StringBuilder out) {
        line(out, "THREE. Four algorithms, one interface.");
        PasswordEncoder[] encoders = {
            new BCryptPasswordEncoder(),
            Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8(),
            SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8(),
            Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
        };
        String[] names = {"bcrypt", "pbkdf2", "scrypt", "argon2"};
        line(out, "  BEFORE HASHING");
        line(out, "    the password  welcome1");
        line(out, "  AFTER HASHING");
        StringBuilder checks = new StringBuilder();
        for (int i = 0; i < encoders.length; i++) {
            String stored = encoders[i].encode("welcome1");
            line(out, "    " + names[i] + "  " + stored.length() + " characters  " + settings(stored));
            checks.append(i == 0 ? "" : ", ").append(encoders[i].matches("welcome1", stored));
        }
        line(out, "  THE LOGIN CHECK");
        line(out, "    each one matches welcome1: " + checks);
    }

    /** The settings an algorithm writes at the front of its stored text: everything before the salt. */
    static String settings(String stored) {
        if (stored.startsWith("$2")) {
            return stored.substring(0, 7);                  // bcrypt: $2a$10$, then salt and hash
        }
        if (!stored.startsWith("$")) {
            return "salt + hash in hex";                                // pbkdf2 writes no settings
        }
        return stored.substring(0, stored.lastIndexOf('$', stored.lastIndexOf('$') - 1) + 1);
    }

    private static void act4SameAttack(StringBuilder out) {
        line(out, "FOUR. The same attack, at two costs.");
        Attacker attacker = new Attacker(Customers.WORDLIST);
        BCryptPasswordEncoder cheap = new BCryptPasswordEncoder(4);
        BCryptPasswordEncoder strong = new BCryptPasswordEncoder(10);
        Attacker.Cracked low = attacker.crack(encodeAll(cheap), cheap);
        Attacker.Cracked high = attacker.crack(encodeAll(strong), strong);
        line(out, "  THE ATTACK");
        line(out, "    " + Customers.WORDLIST.size() + " common passwords, tried in turn on each of "
                + Customers.PASSWORDS.size() + " accounts");
        line(out, "    guesses: " + high.perAccount().entrySet().stream()
                .map(e -> e.getKey() + " " + e.getValue()).collect(Collectors.joining(", ")));
        line(out, "  THE RESULT");
        line(out, "    cost 4:  cracked " + low.found().size() + " of " + Customers.PASSWORDS.size() + " in "
                + low.guesses() + " guesses, " + (1 << 4) + " rounds each");
        line(out, "    cost 10: cracked " + high.found().size() + " of " + Customers.PASSWORDS.size() + " in "
                + high.guesses() + " guesses, " + (1 << 10) + " rounds each");
        line(out, "    each guess at cost 10 is " + (1 << (10 - 4)) + " times the work of cost 4");
        line(out, "    cracked: " + names(high.found()) + "  <- WEAK ONES STILL FALL");
    }

    private static void act5Migration(StringBuilder out) {
        line(out, "FIVE. Moving to a stronger algorithm without asking anyone.");
        DelegatingPasswordEncoder old = allowList("bcrypt");
        DelegatingPasswordEncoder current = allowList(ARGON2_ID);
        String erin = old.encode("letmein");
        line(out, "  BEFORE THE LOGIN");
        line(out, "    erin's stored text  " + erin.substring(0, 15) + "...");
        line(out, "    the shop's default  now argon2");
        line(out, "  THE LOGIN: ERIN TYPES letmein");
        line(out, "    the id " + prefix(erin) + " picks bcrypt; matches: " + current.matches("letmein", erin));
        line(out, "    upgradeEncoding: " + current.upgradeEncoding(erin) + ", " + prefix(erin) + " is not the default");
        String upgraded = current.encode("letmein");
        String rest = upgraded.substring(prefix(upgraded).length());
        line(out, "  AFTER THE LOGIN: STORED AGAIN");
        line(out, "    " + prefix(upgraded));
        line(out, "    " + rest.substring(0, rest.lastIndexOf('$', rest.lastIndexOf('$') - 1) + 1) + "...");
        line(out, "    upgradeEncoding now: " + current.upgradeEncoding(upgraded));
    }

    private static void act6Bill(StringBuilder out) {
        line(out, "SIX. The bill: an open list, and memory.");
        PasswordEncoder open = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        line(out, "  THE ATTACK: A VALUE WRITTEN INTO THE TABLE");
        line(out, "    erin's stored text  {noop}letmein");
        line(out, "  THE DEFAULT ENCODER");
        line(out, "    letmein matches: " + open.matches("letmein", "{noop}letmein") + "  <- NO HASHING AT ALL");
        DelegatingPasswordEncoder strict = allowList(ARGON2_ID);
        line(out, "  THE ALLOW-LIST ENCODER: bcrypt and argon2 only");
        try {
            strict.matches("letmein", "{noop}letmein");
            line(out, "    accepted");
        } catch (IllegalArgumentException e) {
            line(out, "    refused: no encoder mapped for the id noop");
        }
        String stored = strict.encode("welcome1");
        String settings = stored.substring(stored.indexOf('$')).split("\\$")[3];
        int memoryKiB = Integer.parseInt(settings.split(",")[0].substring(2));
        line(out, "  THE MEMORY");
        line(out, "    one argon2 hash     " + memoryKiB + " KiB = " + (memoryKiB / 1024) + " MiB");
        line(out, "    100 logins at once  " + (100 * memoryKiB / 1024) + " MiB  <- MEMORY IS THE PRICE");
    }
}
