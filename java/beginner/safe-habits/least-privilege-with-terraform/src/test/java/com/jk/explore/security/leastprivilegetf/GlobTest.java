package com.jk.explore.security.leastprivilegetf;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GlobTest {

    @Test
    void starMatchesAnyRunIncludingNone() {
        assertTrue(Glob.matches("*", "anything:at:all", true));
        assertTrue(Glob.matches("s3:*", "s3:", true));
        assertTrue(Glob.matches("s3:Get*", "s3:GetObject", true));
        assertTrue(Glob.matches("a*b*c", "aXXbYYc", true));
    }

    @Test
    void literalAndQuestionMark() {
        assertTrue(Glob.matches("s3:GetObjec?", "s3:GetObject", true));
        assertFalse(Glob.matches("s3:GetObjec?", "s3:GetObjec", true));
        assertFalse(Glob.matches("s3:Get*", "s3:PutObject", true));
        assertFalse(Glob.matches("abc", "abcd", true));
    }

    @Test
    void caseRuleIsChosenByTheCaller() {
        assertTrue(Glob.matches("S3:GETOBJECT", "s3:getobject", true));
        assertFalse(Glob.matches("arn:aws:s3:::Shop", "arn:aws:s3:::shop", false));
    }

    @Test
    void backtrackingFindsTheLaterMatch() {
        assertTrue(Glob.matches("*ab", "aab", true));
        assertTrue(Glob.matches("*a*b", "xaxab", true));
        assertFalse(Glob.matches("*ab", "aba", true));
    }
}
