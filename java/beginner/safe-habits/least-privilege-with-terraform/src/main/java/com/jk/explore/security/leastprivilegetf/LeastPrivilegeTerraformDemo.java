package com.jk.explore.security.leastprivilegetf;

import java.util.ArrayList;
import java.util.List;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. It needs no tool: the policies are the JSON that OpenTofu emitted. */
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

    private static void act1PoliciesAsCode(StringBuilder out) {
        line(out, "ONE. Policies written as code.");
        line(out, "  the store's account offers " + total() + " things to do: an action on a resource, such as reading one report or querying one table");
        for (String name : List.of("wildcard", "exporter", "deployer")) {
            PolicyDocument policy = Fixtures.policy(name);
            line(out, "  policy " + name + ": " + policy.statements().size() + " statement(s), so an identity wearing it can do "
                    + Catalogue.blastRadius(Evaluator.of(policy)) + " of " + total());
        }
        line(out, "  the number of things an identity can do if its key is stolen is its blast radius");
        line(out, "  the nightly export needs 3 of them, and the shortcut policy gives " + total());
    }

    private static void act2Matching(StringBuilder out) {
        line(out, "TWO. How a request is matched.");
        Evaluator exporter = Evaluator.of(Fixtures.policy("exporter"));
        line(out, "  exporter reads a report file: " + exporter.decide("s3:GetObject", Catalogue.REPORT_FILE));
        line(out, "  exporter reads a secret file: " + exporter.decide("s3:GetObject", Catalogue.SECRET_FILE));
        line(out, "  exporter lists the reports bucket: " + exporter.decide("s3:ListBucket", Catalogue.REPORTS));
        line(out, "  exporter lists using a file's name instead of the bucket's: " + exporter.decide("s3:ListBucket", Catalogue.REPORT_FILE));
        line(out, "  exporter reads the bucket itself as if it were a file: " + exporter.decide("s3:GetObject", Catalogue.REPORTS)
                + "  <- /* means the contents, not the bucket");
        line(out, "  action names ignore case, s3:getobject: " + exporter.decide("s3:getobject", Catalogue.REPORT_FILE));
        line(out, "  the pattern s3:Get* matches s3:GetObject: " + Glob.matches("s3:Get*", "s3:GetObject", true)
                + ", and s3:PutObject: " + Glob.matches("s3:Get*", "s3:PutObject", true));
        line(out, "  anything not allowed is denied by default");
    }

    private static void act3DenyWins(StringBuilder out) {
        line(out, "THREE. A deny beats every allow.");
        PolicyDocument admin = Fixtures.policy("admin_with_deny");
        Evaluator evaluator = Evaluator.of(admin);
        line(out, "  admin with a deny on secrets can do " + Catalogue.blastRadius(evaluator) + " of " + total() + ", against " + total() + " without the deny");
        line(out, "  reading the secret file: " + evaluator.decide("s3:GetObject", Catalogue.SECRET_FILE));
        line(out, "  writing a report: " + evaluator.decide("s3:PutObject", Catalogue.REPORT_FILE));
        List<PolicyDocument.Statement> reversed = new ArrayList<>(admin.statements());
        java.util.Collections.reverse(reversed);
        Evaluator flipped = Evaluator.of(new PolicyDocument("reversed", reversed));
        line(out, "  with the statements in the opposite order the answer is the same: "
                + (flipped.decide("s3:GetObject", Catalogue.SECRET_FILE) == evaluator.decide("s3:GetObject", Catalogue.SECRET_FILE)));
        line(out, "  the deny is a safety net, and it still leaves " + Catalogue.blastRadius(evaluator) + " things open");
    }

    private static void act4Boundary(StringBuilder out) {
        line(out, "FOUR. A permissions boundary.");
        PolicyDocument wildcard = Fixtures.policy("wildcard");
        PolicyDocument boundary = Fixtures.policy("boundary");
        line(out, "  the wildcard policy alone: " + Catalogue.blastRadius(Evaluator.of(wildcard)) + " of " + total());
        line(out, "  the same policy under the boundary: " + Catalogue.blastRadius(Evaluator.of(wildcard).withBoundary(boundary)) + " of " + total());
        line(out, "  the boundary alone, with nothing attached to the identity: " + Catalogue.blastRadius(new Evaluator(List.of(), boundary)) + " of " + total()
                + "  <- A BOUNDARY GRANTS NOTHING");
        line(out, "  admin with a deny, under the boundary: " + Catalogue.blastRadius(Evaluator.of(Fixtures.policy("admin_with_deny")).withBoundary(boundary)) + " of " + total());
        line(out, "  an identity gets only what both its policies and its boundary allow");
    }

    private static void act5Linter(StringBuilder out) {
        line(out, "FIVE. A linter, and the way to more power.");
        for (String name : PolicyNames.ALL) {
            List<Linter.Finding> findings = Linter.check(Fixtures.policy(name));
            line(out, "  " + name + ": " + findings.size() + " finding(s)" + summary(findings));
        }
        Evaluator deployer = Evaluator.of(Fixtures.policy("deployer"));
        line(out, "  deployer can do only " + Catalogue.blastRadius(deployer) + " of " + total() + ", and is still dangerous: "
                + "iam:PassRole on every role plus lambda:CreateFunction runs code as any role in the account");
        line(out, "  the count hides escalation, so read the policy as well as counting it");
        line(out, "  this evaluator ignores conditions, resource policies and organisation rules, and refuses a statement with a condition: "
                + conditionRefused());
    }

    private static String summary(List<Linter.Finding> findings) {
        if (findings.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder(" (");
        List<String> codes = findings.stream().map(Linter.Finding::code).distinct().toList();
        for (int i = 0; i < codes.size(); i++) {
            text.append(i == 0 ? "" : ", ").append(codes.get(i)).append(' ').append(Linter.count(findings, codes.get(i)));
        }
        return text.append(')').toString();
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
