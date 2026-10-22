package com.jk.explore.security.iammodel;

import java.util.List;
import java.util.Map;

public class IamModelDemo {

    static final String BANK = "111111111111";
    static final String AUDIT = "222222222222";
    static final String SAAS = "999999999999";
    static final String OTHER = "333333333333";

    public static void main(String[] args) {
        System.out.print(run());
    }

    static String s3(String bucket) {
        return "arn:aws:s3:::" + bucket;
    }

    static Policy json(String name, String text) {
        return Policy.parse(name, text);
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String ask(Principal p, String action, String resource, Evaluator.Guard guard) {
        return String.format("%-5s %-17s %-36s %s", p.name(), action, resource.replace("arn:aws:s3:::", "s3:"), Evaluator.decide(new Request(p, action, resource), guard));
    }

    public static String run() {
        StringBuilder out = new StringBuilder();
        act1(out);
        act2(out);
        act3(out);
        act4(out);
        act5(out);
        return out.toString();
    }

    private static void act1(StringBuilder out) {
        line(out, "ONE. Identity policies: default deny, allow, explicit deny.");
        Principal ana = Principal.user(BANK, "ana", json("reports-read", """
                {"Statement":[
                  {"Effect":"Allow","Action":["s3:GetObject","s3:ListBucket"],"Resource":["arn:aws:s3:::bank-reports","arn:aws:s3:::bank-reports/*"]},
                  {"Effect":"Deny","Action":"s3:GetObject","Resource":"arn:aws:s3:::bank-reports/secret/*"}]}"""));
        Evaluator.Guard none = Evaluator.Guard.none(BANK);
        line(out, "  ana's policy allows reading bank-reports and denies reading bank-reports/secret/*:");
        line(out, "    " + ask(ana, "s3:GetObject", s3("bank-reports/2026/q3.csv"), none));
        line(out, "    " + ask(ana, "S3:GETOBJECT", s3("bank-reports/2026/q3.csv"), none) + "  <- ACTION NAMES IGNORE CASE");
        line(out, "    " + ask(ana, "s3:GetObject", s3("bank-reports/secret/keys.txt"), none));
        line(out, "    " + ask(ana, "s3:PutObject", s3("bank-reports/2026/q3.csv"), none));
        line(out, "    " + ask(ana, "s3:GetObject", s3("bank-secrets/keys.txt"), none));
        line(out, "    " + ask(ana, "s3:GetObject", s3("Bank-Reports/2026/q3.csv"), none) + "  <- RESOURCE NAMES DO NOT");
        Principal carol = Principal.user(BANK, "carol", json("everything-but-iam", """
                {"Statement":[{"Effect":"Allow","NotAction":"iam:*","Resource":"*"}]}"""));
        line(out, "  carol's policy is Allow with NotAction iam:*:");
        line(out, "    " + ask(carol, "s3:DeleteBucket", s3("bank-reports"), none) + "  <- ALLOW + NOTACTION ALLOWS EVERYTHING ELSE");
        line(out, "    " + ask(carol, "iam:CreateUser", "arn:aws:iam::" + BANK + ":user/x", none));
        line(out, "  nothing is allowed until something allows it, and a deny anywhere beats every allow");
    }

    private static void act2(StringBuilder out) {
        line(out, "TWO. Accounts: who has to agree.");
        Policy allowRead = Policy.of("allow-read", true, "s3:GetObject", "*");
        Policy nothing = Policy.of("nothing", true, "s3:ListAllMyBuckets", "*");
        String bucketArn = s3("shared-data/file.csv");
        for (String where : new String[] {BANK, AUDIT}) {
            for (boolean identity : new boolean[] {true, false}) {
                for (boolean resource : new boolean[] {true, false}) {
                    if (!identity && !resource) {
                        continue;
                    }
                    Principal user = Principal.user(BANK, "ben", identity ? allowRead : nothing);
                    Policy bucketPolicy = resource ? json("bucket-policy", "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"arn:aws:iam::" + BANK + ":root\"},\"Action\":\"s3:GetObject\",\"Resource\":\"*\"}]}") : null;
                    Decision d = Evaluator.decide(new Request(user, "s3:GetObject", bucketArn), Evaluator.Guard.of(bucketPolicy, where));
                    line(out, String.format("  ben in the bank account, the bucket in the %s account; identity policy %-3s bucket policy %-3s -> %s", where.equals(BANK) ? "same" : "other", identity ? "yes" : "no", resource ? "yes" : "no", d));
                }
            }
        }
        line(out, "  in one account either side is enough; across accounts both must say yes, so one account cannot hand out another's data, or take it");
        Principal ben = Principal.user(BANK, "ben", allowRead);
        Policy publicBucket = json("public-bucket", "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":\"*\",\"Action\":\"s3:GetObject\",\"Resource\":\"*\"}]}");
        Principal stranger = Principal.user(OTHER, "stranger", nothing);
        line(out, "  a bucket policy with Principal *: someone with no account at all can read: " + Evaluator.decide(new Request(Principal.anonymous(), "s3:GetObject", bucketArn), Evaluator.Guard.of(publicBucket, BANK)).allowed() + "  <- EVERYONE ON THE INTERNET");
        line(out, "  a stranger from another account whose own policies allow nothing: " + Evaluator.decide(new Request(stranger, "s3:GetObject", bucketArn), Evaluator.Guard.of(publicBucket, BANK)).allowed() + ", and with a policy of their own that allows it: "
                + Evaluator.decide(new Request(Principal.user(OTHER, "stranger", allowRead), "s3:GetObject", bucketArn), Evaluator.Guard.of(publicBucket, BANK)).allowed());
        line(out, "  an organization policy that does not allow s3:GetObject stops even an administrator: " + Evaluator.decide(new Request(Principal.user(BANK, "root-user", Policy.of("admin", true, "*", "*")), "s3:GetObject", bucketArn),
                new Evaluator.Guard(null, BANK, List.of(Policy.of("only-ec2", true, "ec2:*", "*")))));
    }

    static Role developer() {
        Policy trust = json("trust", "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"arn:aws:iam::" + BANK + ":root\"},\"Action\":\"sts:AssumeRole\",\"Condition\":{\"Bool\":{\"aws:MultiFactorAuthPresent\":\"true\"}}}]}");
        Policy grants = json("developer", "{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":[\"s3:*\",\"dynamodb:*\",\"ec2:*\",\"iam:PassRole\"],\"Resource\":\"*\"}]}");
        return new Role(BANK, "Developer", trust, List.of(grants), null);
    }

    private static void act3(StringBuilder out) {
        line(out, "THREE. Roles and the layers that narrow them.");
        Principal ben = Principal.user(BANK, "ben", Policy.of("assume", true, "sts:AssumeRole", "*"));
        Role dev = developer();
        Role.Session noMfa = dev.assume(ben, Map.of(), null, "ben");
        Role.Session withMfa = dev.assume(ben, Map.of("aws:MultiFactorAuthPresent", "true"), null, "ben");
        line(out, "  ben assumes Developer, whose trust policy asks for a second factor: without it " + noMfa.decision() + "; with it " + withMfa.decision());
        List<String> actions = List.of("s3:GetObject", "s3:PutObject", "s3:DeleteObject", "dynamodb:Query", "ec2:RunInstances", "iam:CreateUser", "iam:PassRole", "kms:Decrypt");
        Evaluator.Guard none = Evaluator.Guard.none(BANK);
        String thing = "arn:aws:service:::thing";
        line(out, "  eight actions asked of the session; the role's own policy allows " + Evaluator.allowed(withMfa.principal(), actions, thing, none).size() + ": " + Evaluator.allowed(withMfa.principal(), actions, thing, none));
        Policy boundary = json("boundary", "{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":[\"s3:*\",\"dynamodb:*\"],\"Resource\":\"*\"}]}");
        Role bounded = new Role(BANK, "Developer", dev.trust(), dev.policies(), boundary);
        Role.Session s2 = bounded.assume(ben, Map.of("aws:MultiFactorAuthPresent", "true"), null, "ben");
        line(out, "  with a permission boundary of s3:* and dynamodb:*, only what both allow is left: " + Evaluator.allowed(s2.principal(), actions, thing, none).size() + ": " + Evaluator.allowed(s2.principal(), actions, thing, none));
        Policy session = json("session", "{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":[\"s3:GetObject\",\"dynamodb:Query\"],\"Resource\":\"*\"}]}");
        Role.Session s3 = bounded.assume(ben, Map.of("aws:MultiFactorAuthPresent", "true"), session, "ben");
        line(out, "  and a session policy passed at assumption, narrower again: " + Evaluator.allowed(s3.principal(), actions, thing, none).size() + ": " + Evaluator.allowed(s3.principal(), actions, thing, none));
        line(out, "  a session policy can only narrow: asking it for iam:CreateUser as well changes nothing: " + Evaluator.decide(new Request(bounded.assume(ben, Map.of("aws:MultiFactorAuthPresent", "true"), json("wide", "{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"*\",\"Resource\":\"*\"}]}"), "ben").principal(), "iam:CreateUser", thing), none));
        Policy named = json("bucket-names-the-session", "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"" + s3.principal().arn() + "\"},\"Action\":\"s3:DeleteObject\",\"Resource\":\"*\"}]}");
        line(out, "  a bucket policy that names the session itself is not narrowed by the boundary or the session policy: s3:DeleteObject " + Evaluator.decide(new Request(s3.principal(), "s3:DeleteObject", thing), Evaluator.Guard.of(named, BANK)) + "  <- NAMING A PRINCIPAL IS A STRONGER GRANT THAN NAMING AN ACCOUNT");
        Policy tagged = json("team-only", "{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"s3:GetObject\",\"Resource\":\"*\",\"Condition\":{\"StringEquals\":{\"aws:PrincipalTag/team\":\"payments\"},\"IpAddress\":{\"aws:SourceIp\":\"203.0.113.0/24\"}}}]}");
        Principal payments = Principal.user(BANK, "dev", tagged).withTags(Map.of("team", "payments"));
        line(out, "  conditions: the payments team from the office network " + Evaluator.decide(new Request(payments, "s3:GetObject", thing, Map.of("aws:SourceIp", "203.0.113.9")), none).allowed()
                + ", from elsewhere " + Evaluator.decide(new Request(payments, "s3:GetObject", thing, Map.of("aws:SourceIp", "198.51.100.9")), none).allowed()
                + ", another team from the office " + Evaluator.decide(new Request(payments.withTags(Map.of("team", "marketing")), "s3:GetObject", thing, Map.of("aws:SourceIp", "203.0.113.9")), none).allowed());
    }

    /** ReportCo, a service that reads its customers' buckets by assuming a role in each customer's account. */
    record ReportCo(Map<String, String> externalIds) {

        String externalIdFor(String customer) {
            return externalIds.get(customer);
        }

        /** The service reads the bucket that "customer" says is theirs: it assumes the role they name, presenting the external id it issued to that customer. */
        Decision readFor(String customer, Role role) {
            Principal worker = Principal.user(SAAS, "ReportCoWorker", Policy.of("assume-customer-roles", true, "sts:AssumeRole", "*"));
            Map<String, String> ctx = externalIdFor(customer) == null ? Map.of() : Map.of("sts:ExternalId", externalIdFor(customer));
            Role.Session s = role.assume(worker, ctx, null, "report");
            if (!s.decision().allowed()) {
                return s.decision();
            }
            return Evaluator.decide(new Request(s.principal(), "s3:GetObject", s3("bank-reports/2026/q3.csv")), Evaluator.Guard.none(BANK));
        }
    }

    private static Role customerRole(String externalIdCondition) {
        String condition = externalIdCondition == null ? "" : ",\"Condition\":{\"StringEquals\":{\"sts:ExternalId\":\"" + externalIdCondition + "\"}}";
        Policy trust = json("trust", "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"arn:aws:iam::" + SAAS + ":root\"},\"Action\":\"sts:AssumeRole\"" + condition + "}]}");
        return new Role(BANK, "ReportCoAccess", trust, List.of(Policy.parse("read-reports", "{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"s3:GetObject\",\"Resource\":\"arn:aws:s3:::bank-reports/*\"}]}")), null);
    }

    private static void act4(StringBuilder out) {
        line(out, "FOUR. The confused deputy.");
        ReportCo service = new ReportCo(Map.of("the-bank", "ext-bank-7f3a", "mallory-corp", "ext-mallory-91c2"));
        Role open = customerRole(null);
        line(out, "  ReportCo (account " + SAAS + ") reads its customers' buckets by assuming a role each customer made, whose trust policy names ReportCo's account");
        line(out, "  the bank's own request through ReportCo: " + service.readFor("the-bank", open));
        line(out, "  Mallory, another customer of ReportCo, types the bank's role into ReportCo's form: " + service.readFor("mallory-corp", open) + "  <- THE SERVICE IS THE DEPUTY, AND IT HAS BEEN FOOLED");
        Role guarded = customerRole("ext-bank-7f3a");
        line(out, "  the trust policy now also asks for the external id that ReportCo issued to the bank, ext-bank-7f3a");
        line(out, "  the bank's request: " + service.readFor("the-bank", guarded));
        line(out, "  Mallory's request, which carries the id ReportCo issued to Mallory: " + service.readFor("mallory-corp", guarded));
        line(out, "  an id that Mallory chooses is no use: it must be issued by the service, one for each customer, and not guessable");
        Policy topic = json("topic-policy", "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"" + SAAS + "\"},\"Action\":\"sns:Publish\",\"Resource\":\"*\"}]}");
        Principal service2 = Principal.user(SAAS, "notifier", Policy.of("publish", true, "sns:Publish", "*"));
        Policy topicGuarded = json("topic-policy", "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"" + SAAS + "\"},\"Action\":\"sns:Publish\",\"Resource\":\"*\",\"Condition\":{\"ArnLike\":{\"aws:SourceArn\":\"arn:aws:s3:::bank-*\"}}}]}");
        String topicArn = "arn:aws:sns:::bank-alerts";
        line(out, "  the same idea between services: a topic that trusts the service publishes for any bucket that asks: " + Evaluator.decide(new Request(Principal.user(SAAS, "notifier", Policy.of("publish", true, "sns:Publish", "*")), "sns:Publish", topicArn,
                Map.of("aws:SourceArn", "arn:aws:s3:::mallory-bucket")), Evaluator.Guard.of(topic, BANK)).allowed());
        line(out, "  with aws:SourceArn in the condition: " + Evaluator.decide(new Request(service2, "sns:Publish", topicArn, Map.of("aws:SourceArn", "arn:aws:s3:::mallory-bucket")), Evaluator.Guard.of(topicGuarded, BANK)).allowed()
                + " for Mallory's bucket, " + Evaluator.decide(new Request(service2, "sns:Publish", topicArn, Map.of("aws:SourceArn", "arn:aws:s3:::bank-statements")), Evaluator.Guard.of(topicGuarded, BANK)).allowed() + " for the bank's");
    }

    private static void act5(StringBuilder out) {
        line(out, "FIVE. The bill.");
        Policy devPolicy = json("DevPolicy", "{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":[\"s3:*\",\"iam:CreatePolicyVersion\"],\"Resource\":\"*\"}]}");
        Policy trust = json("trust", "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"arn:aws:iam::" + BANK + ":root\"},\"Action\":\"sts:AssumeRole\"}]}");
        Role devRole = new Role(BANK, "DevRole", trust, List.of(devPolicy), null);
        Role adminRole = new Role(BANK, "AdminRole", trust, List.of(Policy.of("AdministratorAccess", true, "*", "*")), null);
        Principal carol = Principal.user(BANK, "carol", Policy.of("assume-dev", true, "sts:AssumeRole", devRole.arn()));
        Principal ben = Principal.user(BANK, "ben", json("builder", "{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":[\"iam:PassRole\",\"lambda:CreateFunction\",\"lambda:InvokeFunction\"],\"Resource\":\"*\"}]}"));
        Principal ana = Principal.user(BANK, "ana", Policy.of("reports-read", true, "s3:GetObject", "arn:aws:s3:::bank-reports/*"));
        Escalation.World world = new Escalation.World(BANK, List.of(ana, ben, carol), List.of(devRole, adminRole), Map.of("DevPolicy", "DevRole"));
        line(out, "  who can make themselves an administrator, asking the evaluator at every step:");
        for (Principal p : world.users()) {
            List<String> paths = Escalation.paths(p, world);
            line(out, "    " + (paths.isEmpty() ? p.name() + ": no path found" : String.join("\n    ", paths)));
        }
        line(out, "  carol was given one harmless-looking permission, to assume a developer role, and the role may rewrite its own policy; ben may pass the admin role to a function he runs");
        long wildcards = List.of(devPolicy, adminRole.policies().get(0), world.users().get(1).policies().get(0)).stream().flatMap(pol -> pol.statements().stream())
                .filter(s -> s.actions().stream().anyMatch(a -> a.equals("*") || a.endsWith(":*")) || s.resources().contains("*")).count();
        line(out, "  statements with a wildcard action or resource in these policies: " + wildcards + " of 3  <- EVERY WILDCARD IS A DECISION NOBODY MADE");
        line(out, "  this evaluator follows the documented rules for a handful of services; the real service has more (service-linked roles, tag rules, session tags, more condition keys),");
        line(out, "  so a model like this finds the shape of a problem and the real service's policy simulator, or a real test account, confirms it");
    }
}
