package com.jk.explore.security.attacksnginx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** The hand-made rule gives the same counts the real nginx gave in the golden output, with no Docker. */
class LeakyBucketTest {

    private static String act(String number) throws IOException {
        String out = Files.readString(Path.of("docs", "expected-output.txt"));
        Matcher m = Pattern.compile("(?ms)^" + number + "\\. .*?(?=^[A-Z]+\\. |\\z)").matcher(out);
        assertTrue(m.find(), number);
        return m.group();
    }

    private static String counts(String act, int line) {
        Matcher m = Pattern.compile("(\\d+ answered, \\d+ refused)").matcher(act);
        for (int i = 0; i <= line; i++) {
            assertTrue(m.find());
        }
        return m.group(1);
    }

    private static String twenty(LeakyBucket bucket, IntFunction<String> key) {
        int answered = 0;
        for (int i = 0; i < 20; i++) {
            if (bucket.allow(key.apply(i), 0)) {
                answered++;
            }
        }
        return answered + " answered, " + (20 - answered) + " refused";
    }

    @Test
    void oneKeyLetsThroughOnePlusTheBurstLikeNginx() throws IOException {
        assertEquals(counts(act("TWO"), 0), twenty(new LeakyBucket(1, 5), i -> "one address"));
    }

    @Test
    void aNewHeaderValueEveryTimeIsANewEmptyBucket() throws IOException {
        assertEquals(counts(act("THREE"), 0), twenty(new LeakyBucket(1, 5), i -> "10.0.0." + i));
    }

    @Test
    void aSprayOfUsernamesFillsNoBucket() throws IOException {
        assertEquals(counts(act("FIVE"), 1), twenty(new LeakyBucket(1, 5), i -> "customer" + i));
    }

    @Test
    void theExcessGrowsByOneUntilTheBurstThenStays() {
        LeakyBucket bucket = new LeakyBucket(1, 5);
        for (int i = 0; i < 6; i++) {
            assertTrue(bucket.allow("dana", 0));
            assertEquals(i, bucket.excess("dana"));
        }
        assertFalse(bucket.allow("dana", 0));
        assertEquals(5, bucket.excess("dana"));
    }

    @Test
    void oneMinuteDrainsOneRequest() {
        LeakyBucket bucket = new LeakyBucket(1, 5);
        for (int i = 0; i < 6; i++) {
            bucket.allow("dana", 0);
        }
        assertFalse(bucket.allow("dana", 59_000));
        assertTrue(bucket.allow("dana", 60_000));
        assertFalse(bucket.allow("dana", 60_001));
    }
}
