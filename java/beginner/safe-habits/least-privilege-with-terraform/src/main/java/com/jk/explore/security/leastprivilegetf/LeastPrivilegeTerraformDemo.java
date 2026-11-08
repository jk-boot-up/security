package com.jk.explore.security.leastprivilegetf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the online store story in five numbered acts and prints exact, repeatable results. Each act prints the policy as written,
 * then what it allows, in separate labelled sections. It needs no tool: the policies are the JSON that OpenTofu emitted.
 */
public class LeastPrivilegeTerraformDemo {

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        act1PoliciesAsCode(out);
        act2Matching(out);
        act3DenyWins(out);
        act4Boundary(out);
        act5Linter(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static int total() {
        return Catalogue.all().size();
    }

    /** The part of an ARN after the account, such as shop-reports/q3.csv or table/orders; "*" stays "*". */
    static String shortName(String arn) {
        String[] parts = arn.split(":", 6);
        return parts.length == 6 ? parts[5] : arn;
    }

    /** One statement as "Allow action, action on resource", wrapped so that no line is longer than the screen. */
    private static void statement(StringBuilder out, PolicyDocument.Statement statement) {
        String head = (statement.allow() ? "Allow " : "Deny ") + String.join(", ", statement.actions());
        String on = "on " + String.join(", ", statement.resources().stream().map(LeastPrivilegeTerraformDemo::shortName).toList());
        if (head.length() + on.length() + 5 <= 62) {
            line(out, "    " + head + " " + on);
        } else {
            StringBuilder row = new StringBuilder(statement.allow() ? "Allow " : "Deny ");
            for (int i = 0; i < statement.actions().size(); i++) {
                String action = statement.actions().get(i) + (i < statement.actions().size() - 1 ? "," : "");
                if (row.length() + action.length() > 56) {
                    line(out, "    " + row.toString().stripTrailing());
                    row = new StringBuilder("  ");
                }
                row.append(action).append(' ');
            }
            line(out, "    " + row.toString().stripTrailing());
            line(out, "      " + on);
        }
    }

    private static void policy(StringBuilder out, PolicyDocument policy) {
        for (PolicyDocument.Statement statement : policy.statements()) {
            statement(out, statement);
        }
    }

    private static void request(StringBuilder out, Evaluator evaluator, String action, String resource) {
        line(out, String.format("    %-38s %s", action + " on " + shortName(resource), evaluator.decide(action, resource)));
    }

    private static String count(Evaluator evaluator) {
        return String.format("%2d of %d", Catalogue.blastRadius(evaluator), total());
    }

    private static void act1PoliciesAsCode(StringBuilder out) {
        line(out, "ONE. Policies written as code.");
        line(out, "  THE ACCOUNT (what a stolen key could be used for):");
        Map<String, Integer> services = new LinkedHashMap<>();
        for (Catalogue.Pair pair : Catalogue.all()) {
            services.merge(pair.action().substring(0, pair.action().indexOf(':')), 1, Integer::sum);
        }
        List<String> parts = new ArrayList<>();
        services.forEach((service, n) -> parts.add(service + " " + n));
        line(out, "    " + String.join(", ", parts));
        line(out, "    " + total() + " things, each an action on a resource");
        line(out, "    such as s3:GetObject on " + shortName(Catalogue.REPORT_FILE));
        PolicyDocument wildcard = Fixtures.policy("wildcard");
        line(out, "  BEFORE THE FIX (policy wildcard, " + wildcard.statements().size() + " statement):");
        policy(out, wildcard);
        line(out, "    a stolen key reaches " + count(Evaluator.of(wildcard)).strip() + "  <- EVERYTHING");
        PolicyDocument exporter = Fixtures.policy("exporter");
        line(out, "  AFTER THE FIX (policy exporter, " + exporter.statements().size() + " statements):");
        policy(out, exporter);
        line(out, "    a stolen key reaches " + count(Evaluator.of(exporter)).strip() + ": only the nightly export");
    }

    private static void act2Matching(StringBuilder out) {
        line(out, "TWO. How a request is matched.");
        PolicyDocument policy = Fixtures.policy("exporter");
        Evaluator exporter = Evaluator.of(policy);
        line(out, "  THE POLICY AS WRITTEN (exporter, the S3 part):");
        policy.statements().stream().filter(s -> s.actions().get(0).startsWith("s3:")).forEach(s -> statement(out, s));
        line(out, "  THE REQUESTS (action on resource, then the decision):");
        request(out, exporter, "s3:GetObject", Catalogue.REPORT_FILE);
        request(out, exporter, "s3:GetObject", Catalogue.SECRET_FILE);
        request(out, exporter, "s3:ListBucket", Catalogue.REPORTS);
        request(out, exporter, "s3:ListBucket", Catalogue.REPORT_FILE);
        request(out, exporter, "s3:GetObject", Catalogue.REPORTS);
        request(out, exporter, "s3:getobject", Catalogue.REPORT_FILE);
        line(out, "  THE PATTERN s3:Get* (a star matches any letters):");
        line(out, "    s3:GetObject  " + Glob.matches("s3:Get*", "s3:GetObject", true));
        line(out, "    s3:PutObject  " + Glob.matches("s3:Get*", "s3:PutObject", true));
        line(out, "  /* means the files in the bucket, not the bucket itself");
        line(out, "  no statement matches: denied by default (IMPLICIT_DENY)");
    }

    private static void act3DenyWins(StringBuilder out) {
        line(out, "THREE. A deny beats every allow.");
        PolicyDocument admin = Fixtures.policy("admin_with_deny");
        Evaluator evaluator = Evaluator.of(admin);
        line(out, "  THE POLICY AS WRITTEN (admin_with_deny):");
        policy(out, admin);
        line(out, "  THE REQUESTS (action on resource, then the decision):");
        request(out, evaluator, "s3:GetObject", Catalogue.SECRET_FILE);
        request(out, evaluator, "kms:Decrypt", Catalogue.KEY);
        request(out, evaluator, "s3:PutObject", Catalogue.REPORT_FILE);
        List<PolicyDocument.Statement> reversed = new ArrayList<>(admin.statements());
        Collections.reverse(reversed);
        Evaluator flipped = Evaluator.of(new PolicyDocument("reversed", reversed));
        boolean same = Catalogue.all().stream()
                .allMatch(p -> flipped.decide(p.action(), p.resource()) == evaluator.decide(p.action(), p.resource()));
        line(out, "  THE SAME STATEMENTS IN THE OPPOSITE ORDER:");
        line(out, "    the same answer for all " + total() + " things: " + same);
        line(out, "  THE COUNT (what a stolen key reaches):");
        line(out, "    without the deny  " + count(Evaluator.of(Fixtures.policy("wildcard"))));
        line(out, "    with the deny     " + count(evaluator) + "  <- STILL " + Catalogue.blastRadius(evaluator) + " OPEN");
    }

    private static void act4Boundary(StringBuilder out) {
        line(out, "FOUR. A permissions boundary.");
        PolicyDocument wildcard = Fixtures.policy("wildcard");
        PolicyDocument boundary = Fixtures.policy("boundary");
        line(out, "  THE BOUNDARY AS WRITTEN (a ceiling, not a grant):");
        policy(out, boundary);
        Evaluator bounded = Evaluator.of(wildcard).withBoundary(boundary);
        line(out, "  THE COUNT (what a stolen key reaches):");
        line(out, "    wildcard alone               " + count(Evaluator.of(wildcard)));
        line(out, "    wildcard under the boundary  " + count(bounded));
        line(out, "    the boundary alone           " + count(new Evaluator(List.of(), boundary)) + "  <- GRANTS NOTHING");
        line(out, "    admin_with_deny, bounded     " + count(Evaluator.of(Fixtures.policy("admin_with_deny")).withBoundary(boundary)));
        line(out, "  THE " + Catalogue.blastRadius(bounded) + " THINGS LEFT (wildcard under the boundary):");
        for (Catalogue.Pair pair : Catalogue.reachable(bounded)) {
            line(out, "    " + pair.action() + " on " + shortName(pair.resource()));
        }
        line(out, "  allowed = in a policy AND in the boundary, and never denied");
    }

    private static void act5Linter(StringBuilder out) {
        line(out, "FIVE. A linter, and the way to more power.");
        line(out, "  THE FINDINGS (the linter reads the text of each policy):");
        for (String name : PolicyNames.ALL) {
            line(out, String.format("    %-16s %s", name, summary(Linter.check(Fixtures.policy(name)))));
        }
        PolicyDocument deployer = Fixtures.policy("deployer");
        line(out, "  THE DEPLOYER AS WRITTEN:");
        policy(out, deployer);
        line(out, "    a stolen key reaches " + count(Evaluator.of(deployer)).strip() + ": it looks small");
        line(out, "  THE WAY TO MORE POWER (three steps):");
        line(out, "    1. iam:PassRole on *: hand any role to a function");
        line(out, "    2. lambda:CreateFunction: make checkout with that role");
        line(out, "    3. lambda:InvokeFunction: run code as that role");
        line(out, "  so read the policy as well as counting it");
        line(out, "  THE LIMITS OF THIS EVALUATOR:");
        line(out, "    it ignores resource policies and organisation rules");
        line(out, "    it refuses a statement with a condition: " + conditionRefused());
    }

    private static String summary(List<Linter.Finding> findings) {
        if (findings.isEmpty()) {
            return "none";
        }
        List<String> codes = findings.stream().map(Linter.Finding::code).distinct().toList();
        List<String> parts = new ArrayList<>();
        for (String code : codes) {
            parts.add(code + " " + Linter.count(findings, code));
        }
        return String.join(", ", parts);
    }

    private static boolean conditionRefused() {
        try {
            PolicyDocument.parse("c", "{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"s3:*\",\"Resource\":\"*\",\"Condition\":{\"Bool\":{\"aws:SecureTransport\":\"true\"}}}]}");
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }
}
