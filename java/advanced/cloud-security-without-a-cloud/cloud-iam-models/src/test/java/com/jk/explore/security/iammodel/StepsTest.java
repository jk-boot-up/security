package com.jk.explore.security.iammodel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The seven questions, asked by hand, give the evaluator's exact answer for every combination of principal, guard and action. */
class StepsTest {

    private static final String A = "111111111111";
    private static final String B = "222222222222";
    private static final String OBJ = "arn:aws:s3:::bucket/key";

    private static Policy p(String json) {
        return Policy.parse("p", json);
    }

    @Test
    void theWalkAgreesWithTheEvaluatorEverywhere() {
        String session = "arn:aws:sts::" + A + ":assumed-role/R/s";
        List<Principal> who = List.of(
                Principal.user(A, "u", Policy.of("read", true, "s3:Get*", "*")),
                Principal.user(A, "d", Policy.of("all", true, "*", "*"), Policy.of("no-delete", false, "s3:Delete*", "*")),
                Principal.user(B, "other", Policy.of("read", true, "s3:GetObject", "*")),
                Principal.user(B, "nothing", Policy.of("n", true, "x:y", "*")),
                new Principal(A, session, List.of(Policy.of("all", true, "*", "*")), Policy.of("b", true, "s3:*", "*"), Policy.of("s", true, "s3:GetObject", "*"), Map.of()),
                Principal.anonymous());
        List<Evaluator.Guard> guards = new ArrayList<>();
        for (String account : List.of(A, B)) {
            guards.add(Evaluator.Guard.none(account));
            guards.add(Evaluator.Guard.of(p("{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":\"*\",\"Action\":\"s3:GetObject\",\"Resource\":\"*\"}]}"), account));
            guards.add(Evaluator.Guard.of(p("{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"" + session + "\"},\"Action\":\"s3:*\",\"Resource\":\"*\"}]}"), account));
            guards.add(Evaluator.Guard.of(p("{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"arn:aws:iam::" + B + ":root\"},\"Action\":\"s3:*\",\"Resource\":\"*\"}]}"), account));
            guards.add(Evaluator.Guard.of(p("{\"Statement\":[{\"Effect\":\"Deny\",\"Principal\":\"*\",\"Action\":\"s3:PutObject\",\"Resource\":\"*\"}]}"), account));
            guards.add(new Evaluator.Guard(null, account, List.of(Policy.of("ec2", true, "ec2:*", "*"))));
            guards.add(new Evaluator.Guard(null, account, List.of(Policy.of("all", true, "*", "*"), Policy.of("no-s3-put", false, "s3:PutObject", "*"))));
        }
        int compared = 0;
        for (Principal p : who) {
            for (Evaluator.Guard g : guards) {
                for (String action : List.of("s3:GetObject", "s3:PutObject", "s3:DeleteObject", "ec2:RunInstances", "iam:CreateUser")) {
                    Request r = new Request(p, action, OBJ);
                    assertEquals(Evaluator.decide(r, g).allowed(), Steps.of(r, g).allowed(), p.arn() + " " + action + " " + g);
                    compared++;
                }
            }
        }
        assertEquals(6 * 14 * 5, compared);
    }

    @Test
    void aDenyStopsTheWalkAndTheRestIsNotAsked() {
        Principal u = Principal.user(A, "u", Policy.of("all", true, "*", "*"), Policy.of("no-delete", false, "s3:Delete*", "*"));
        Steps.Walk w = Steps.of(new Request(u, "s3:DeleteObject", OBJ), Evaluator.Guard.none(A));
        assertEquals(List.of("yes: no-delete", "-", "-", "-", "-", "-", "-"), w.answers());
        assertEquals(false, w.allowed());
    }

    @Test
    void acrossAccountsBothSidesAreAsked() {
        Policy bucket = p("{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"arn:aws:iam::" + A + ":root\"},\"Action\":\"s3:GetObject\",\"Resource\":\"*\"}]}");
        Steps.Walk w = Steps.of(new Request(Principal.user(A, "u", Policy.of("n", true, "x:y", "*")), "s3:GetObject", OBJ), Evaluator.Guard.of(bucket, B));
        assertEquals(List.of("no", "none", "none", "none", "no", "yes", "no: both must allow"), w.answers());
        assertEquals(false, w.allowed());
    }
}
