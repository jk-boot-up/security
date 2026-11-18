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

    /** A resource ARN as the slides show it: s3 buckets without their arn:aws:s3::: prefix. */
    private static String shortArn(String resource) {
        return resource.replace("arn:aws:s3:::", "");
    }

    private static String word(Decision d) {
        return d.allowed() ? "ALLOW" : "DENY ";
    }

    private static String yes(boolean b) {
        return b ? "yes" : "no";
    }

    /** The evaluation order for several requests side by side: one column of answers per request. */
    private static void walk(StringBuilder out, List<String> names, List<Steps.Walk> walks) {
        StringBuilder head = new StringBuilder(String.format("    %-32s", "question"));
        names.forEach(n -> head.append(String.format("%-9s", n)));
        line(out, head.toString().stripTrailing());
        for (int q = 0; q < Steps.QUESTIONS.size(); q++) {
            StringBuilder row = new StringBuilder(String.format("    %-32s", Steps.QUESTIONS.get(q)));
            for (Steps.Walk w : walks) {
                String a = w.answers().get(q);
                row.append(String.format("%-9s", a.contains(":") ? a.substring(0, a.indexOf(':')) : a));
            }
            line(out, row.toString().stripTrailing());
        }
        StringBuilder last = new StringBuilder(String.format("    %-32s", "answer"));
        walks.forEach(w -> last.append(String.format("%-9s", w.allowed() ? "ALLOW" : "DENY")));
        line(out, last.toString().stripTrailing());
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
        line(out, "  THE POLICY reports-read (ana, bank account " + BANK + "):");
        line(out, "    Allow  s3:GetObject, s3:ListBucket");
        line(out, "           on bank-reports and bank-reports/*");
        line(out, "    Deny   s3:GetObject on bank-reports/secret/*");
        String[][] asks = {
            {"s3:GetObject", "bank-reports/2026/q3.csv"},
            {"S3:GETOBJECT", "bank-reports/2026/q3.csv"},
            {"s3:GetObject", "bank-reports/secret/keys.txt"},
            {"s3:PutObject", "bank-reports/2026/q3.csv"},
            {"s3:GetObject", "bank-secrets/keys.txt"},
            {"s3:GetObject", "Bank-Reports/2026/q3.csv"},
        };
        line(out, "  THE REQUESTS ana makes:");
        for (int i = 0; i < asks.length; i++) {
            line(out, String.format("    %d  %-13s %s", i + 1, asks[i][0], asks[i][1]));
        }
        line(out, "  THE ANSWERS:");
        List<Steps.Walk> walks = new java.util.ArrayList<>();
        for (int i = 0; i < asks.length; i++) {
            Request r = new Request(ana, asks[i][0], s3(asks[i][1]));
            walks.add(Steps.of(r, none));
            Decision d = Evaluator.decide(r, none);
            line(out, String.format("    %d  %s  %s", i + 1, word(d), d.why()));
        }
        line(out, "  WHY 2 AND 6 DIFFER:");
        line(out, "    action names ignore case:   S3:GETOBJECT = s3:GetObject");
        line(out, "    resource names keep it:     Bank-Reports is not bank-reports");
        line(out, "  HOW REQUESTS 1 AND 3 ARE DECIDED (top to bottom):");
        walk(out, List.of("1", "3"), List.of(walks.get(0), walks.get(2)));
        Principal carol = Principal.user(BANK, "carol", json("everything-but-iam", """
                {"Statement":[{"Effect":"Allow","NotAction":"iam:*","Resource":"*"}]}"""));
        line(out, "  THE POLICY everything-but-iam (carol):");
        line(out, "    Allow  NotAction iam:*  on *");
        line(out, "  THE REQUESTS carol makes, and the answers:");
        for (String[] a : new String[][] {{"s3:DeleteBucket", s3("bank-reports")}, {"iam:CreateUser", "arn:aws:iam::" + BANK + ":user/x"}}) {
            Decision d = Evaluator.decide(new Request(carol, a[0], a[1]), none);
            line(out, String.format("    %-16s %-14s %s", a[0], a[1].startsWith("arn:aws:iam") ? "user/x" : shortArn(a[1]), word(d).strip()));
        }
        line(out, "    NotAction iam:* allows every action of every other service");
    }

    private static void act2(StringBuilder out) {
        line(out, "TWO. Accounts: who has to agree.");
        Policy allowRead = Policy.of("allow-read", true, "s3:GetObject", "*");
        Policy nothing = Policy.of("nothing", true, "s3:ListAllMyBuckets", "*");
        String bucketArn = s3("shared-data/file.csv");
        Policy bucketPolicy = json("bucket-policy", "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"arn:aws:iam::" + BANK + ":root\"},\"Action\":\"s3:GetObject\",\"Resource\":\"*\"}]}");
        line(out, "  THE REQUEST: ben, of the bank account " + BANK + ",");
        line(out, "    asks s3:GetObject on shared-data/file.csv");
        line(out, "    identity policy  Allow s3:GetObject on *");
        line(out, "    bucket policy    Allow account " + BANK + " s3:GetObject");
        line(out, "  THE ANSWERS (which policies exist, where the bucket is):");
        line(out, "    bucket in      identity  bucket  answer");
        int[] allowed = new int[2];
        for (String where : new String[] {BANK, AUDIT}) {
            for (boolean identity : new boolean[] {true, false}) {
                for (boolean resource : new boolean[] {true, false}) {
                    if (!identity && !resource) {
                        continue;
                    }
                    Principal user = Principal.user(BANK, "ben", identity ? allowRead : nothing);
                    Decision d = Evaluator.decide(new Request(user, "s3:GetObject", bucketArn), Evaluator.Guard.of(resource ? bucketPolicy : null, where));
                    allowed[where.equals(BANK) ? 0 : 1] += d.allowed() ? 1 : 0;
                    line(out, String.format("    %-14s %-9s %-7s %s", where.equals(BANK) ? "same account" : "other account", yes(identity), yes(resource), word(d).strip()));
                }
            }
        }
        line(out, "  HOW THE RULE WORKS OUT:");
        line(out, "    same account   identity OR bucket   " + allowed[0] + " of 3 ALLOW");
        line(out, "    other account  identity AND bucket  " + allowed[1] + " of 3 ALLOW");
        Policy publicBucket = json("public-bucket", "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":\"*\",\"Action\":\"s3:GetObject\",\"Resource\":\"*\"}]}");
        Principal stranger = Principal.user(OTHER, "stranger", nothing);
        line(out, "  THE BUCKET POLICY public-bucket:");
        line(out, "    Allow  Principal *  s3:GetObject on *");
        line(out, "  THE ANSWERS for s3:GetObject:");
        line(out, "    nobody, no account at all              " + word(Evaluator.decide(new Request(Principal.anonymous(), "s3:GetObject", bucketArn), Evaluator.Guard.of(publicBucket, BANK))).strip() + "  <- ANYONE");
        line(out, "    a stranger, own policy allows nothing  " + word(Evaluator.decide(new Request(stranger, "s3:GetObject", bucketArn), Evaluator.Guard.of(publicBucket, BANK))).strip());
        line(out, "    a stranger, own policy allows it       " + word(Evaluator.decide(new Request(Principal.user(OTHER, "stranger", allowRead), "s3:GetObject", bucketArn), Evaluator.Guard.of(publicBucket, BANK))).strip());
        Decision org = Evaluator.decide(new Request(Principal.user(BANK, "root-user", Policy.of("admin", true, "*", "*")), "s3:GetObject", bucketArn),
                new Evaluator.Guard(null, BANK, List.of(Policy.of("only-ec2", true, "ec2:*", "*"))));
        line(out, "  THE ORGANIZATION POLICY only-ec2:");
        line(out, "    Allow  ec2:* on *");
        line(out, "  THE ANSWER for an administrator (Allow * on *):");
        line(out, "    s3:GetObject  " + word(org).strip() + "  " + org.why().replace("implicit deny: ", ""));
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
        Map<String, String> mfa = Map.of("aws:MultiFactorAuthPresent", "true");
        line(out, "  THE ROLE Developer (bank account " + BANK + "):");
        line(out, "    trust   Allow account " + BANK + " sts:AssumeRole");
        line(out, "            if aws:MultiFactorAuthPresent is true");
        line(out, "    policy  Allow s3:*, dynamodb:*, ec2:*, iam:PassRole");
        line(out, "  THE ASSUMPTION by ben:");
        Role.Session noMfa = dev.assume(ben, Map.of(), null, "ben");
        Role.Session withMfa = dev.assume(ben, mfa, null, "ben");
        line(out, "    no second factor    " + word(noMfa.decision()) + " " + noMfa.decision().why().replace("implicit deny: ", ""));
        line(out, "    a second factor     " + word(withMfa.decision()) + " a session of Developer");
        Policy boundary = json("boundary", "{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":[\"s3:*\",\"dynamodb:*\"],\"Resource\":\"*\"}]}");
        Policy session = json("session", "{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":[\"s3:GetObject\",\"dynamodb:Query\"],\"Resource\":\"*\"}]}");
        Role bounded = new Role(BANK, "Developer", dev.trust(), dev.policies(), boundary);
        Role.Session s2 = bounded.assume(ben, mfa, null, "ben");
        Role.Session s3 = bounded.assume(ben, mfa, session, "ben");
        line(out, "  THE TWO LAYERS that narrow the session:");
        line(out, "    boundary        Allow s3:*, dynamodb:*");
        line(out, "    session policy  Allow s3:GetObject, dynamodb:Query");
        List<String> actions = List.of("s3:GetObject", "s3:PutObject", "s3:DeleteObject", "dynamodb:Query", "ec2:RunInstances", "iam:CreateUser", "iam:PassRole", "kms:Decrypt");
        Evaluator.Guard none = Evaluator.Guard.none(BANK);
        String thing = "arn:aws:service:::thing";
        List<List<String>> layers = List.of(Evaluator.allowed(withMfa.principal(), actions, thing, none), Evaluator.allowed(s2.principal(), actions, thing, none),
                Evaluator.allowed(s3.principal(), actions, thing, none));
        line(out, "  THE EIGHT ACTIONS, LAYER BY LAYER:");
        line(out, "    action            role  +boundary  +session");
        for (String a : actions) {
            line(out, String.format("    %-17s %-5s %-10s %s", a, yes(layers.get(0).contains(a)), yes(layers.get(1).contains(a)), yes(layers.get(2).contains(a))));
        }
        line(out, String.format("    %-17s %-5d %-10d %d", "allowed", layers.get(0).size(), layers.get(1).size(), layers.get(2).size()));
        line(out, "    a session policy can only narrow: one that allows *");
        line(out, "    still gets iam:CreateUser " + word(Evaluator.decide(new Request(bounded.assume(ben, mfa, json("wide", "{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"*\",\"Resource\":\"*\"}]}"), "ben").principal(),
                "iam:CreateUser", thing), none)).strip());
        Policy named = json("bucket-names-the-session", "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"" + s3.principal().arn() + "\"},\"Action\":\"s3:DeleteObject\",\"Resource\":\"*\"}]}");
        line(out, "  THE BUCKET POLICY that names the session itself:");
        line(out, "    Allow  " + s3.principal().arn().replace("arn:aws:sts::" + BANK + ":", ""));
        line(out, "           s3:DeleteObject on *");
        Decision namedDecision = Evaluator.decide(new Request(s3.principal(), "s3:DeleteObject", thing), Evaluator.Guard.of(named, BANK));
        Steps.Walk namedWalk = Steps.of(new Request(s3.principal(), "s3:DeleteObject", thing), Evaluator.Guard.of(named, BANK));
        line(out, "  THE ANSWER for s3:DeleteObject:");
        line(out, "    " + word(namedDecision).strip() + "  " + namedDecision.why());
        line(out, "    the boundary and the session policy are " + namedWalk.answers().get(2));
        Policy tagged = json("team-only", "{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"s3:GetObject\",\"Resource\":\"*\",\"Condition\":{\"StringEquals\":{\"aws:PrincipalTag/team\":\"payments\"},\"IpAddress\":{\"aws:SourceIp\":\"203.0.113.0/24\"}}}]}");
        Principal payments = Principal.user(BANK, "dev", tagged).withTags(Map.of("team", "payments"));
        line(out, "  THE CONDITIONS of the policy team-only:");
        line(out, "    StringEquals  aws:PrincipalTag/team = payments");
        line(out, "    IpAddress     aws:SourceIp in 203.0.113.0/24");
        line(out, "  THE ANSWERS for s3:GetObject:");
        line(out, "    team payments,  from 203.0.113.9   " + word(Evaluator.decide(new Request(payments, "s3:GetObject", thing, Map.of("aws:SourceIp", "203.0.113.9")), none)).strip());
        line(out, "    team payments,  from 198.51.100.9  " + word(Evaluator.decide(new Request(payments, "s3:GetObject", thing, Map.of("aws:SourceIp", "198.51.100.9")), none)).strip());
        line(out, "    team marketing, from 203.0.113.9   "
                + word(Evaluator.decide(new Request(payments.withTags(Map.of("team", "marketing")), "s3:GetObject", thing, Map.of("aws:SourceIp", "203.0.113.9")), none)).strip());
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
        Map<String, String> ids = new java.util.LinkedHashMap<>();
        ids.put("the-bank", "ext-bank-7f3a");
        ids.put("mallory-corp", "ext-mallory-91c2");
        ReportCo service = new ReportCo(ids);
        line(out, "  THE SETUP:");
        line(out, "    ReportCo   a reporting service, account " + SAAS);
        line(out, "    customers  the bank and Mallory each made a role");
        line(out, "               that ReportCo assumes to read their files");
        line(out, "    the bank's role  ReportCoAccess, may read bank-reports/*");
        line(out, "  BEFORE THE FIX, the trust policy of ReportCoAccess:");
        line(out, "    Allow account " + SAAS + " sts:AssumeRole");
        Role open = customerRole(null);
        line(out, "  THE ANSWERS before the fix:");
        line(out, "    the bank asks for its reports       " + word(service.readFor("the-bank", open)).strip());
        line(out, "    Mallory names the bank's role       " + word(service.readFor("mallory-corp", open)).strip() + "  <- FOOLED");
        line(out, "    ReportCo is the deputy: it acts with its own rights,");
        line(out, "    for whoever asked");
        line(out, "  THE EXTERNAL IDS that ReportCo issued, one per customer:");
        ids.forEach((customer, id) -> line(out, String.format("    %-13s %s", customer, id)));
        Role guarded = customerRole("ext-bank-7f3a");
        line(out, "  AFTER THE FIX, the trust policy also asks:");
        line(out, "    StringEquals sts:ExternalId = ext-bank-7f3a");
        line(out, "  THE ANSWERS after the fix:");
        for (String customer : List.of("the-bank", "mallory-corp")) {
            String id = service.externalIdFor(customer);
            line(out, String.format("    %-13s sends %-17s %-3s  %s", customer, id, id.equals("ext-bank-7f3a") ? "=" : "!=", word(service.readFor(customer, guarded)).strip()));
        }
        line(out, "    the id is issued by ReportCo, so Mallory cannot choose it");
        Policy topic = json("topic-policy", "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"" + SAAS + "\"},\"Action\":\"sns:Publish\",\"Resource\":\"*\"}]}");
        Principal notifier = Principal.user(SAAS, "notifier", Policy.of("publish", true, "sns:Publish", "*"));
        Policy topicGuarded = json("topic-policy", "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"" + SAAS + "\"},\"Action\":\"sns:Publish\",\"Resource\":\"*\",\"Condition\":{\"ArnLike\":{\"aws:SourceArn\":\"arn:aws:s3:::bank-*\"}}}]}");
        String topicArn = "arn:aws:sns:::bank-alerts";
        line(out, "  BETWEEN SERVICES: the topic bank-alerts trusts the notifier");
        line(out, "    before  Allow account " + SAAS + " sns:Publish");
        line(out, "    after   ... if aws:SourceArn is like arn:aws:s3:::bank-*");
        line(out, "  THE ANSWERS (the bucket that asked the notifier to publish):");
        for (String bucket : List.of("mallory-bucket", "bank-statements")) {
            Map<String, String> ctx = Map.of("aws:SourceArn", "arn:aws:s3:::" + bucket);
            line(out, String.format("    %-16s before %-6s after %s", bucket, word(Evaluator.decide(new Request(notifier, "sns:Publish", topicArn, ctx), Evaluator.Guard.of(topic, BANK))).strip(),
                    word(Evaluator.decide(new Request(notifier, "sns:Publish", topicArn, ctx), Evaluator.Guard.of(topicGuarded, BANK))).strip()));
        }
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
        line(out, "  THE POLICIES (all in the bank account):");
        line(out, "    ana        Allow s3:GetObject on bank-reports/*");
        line(out, "    ben        Allow iam:PassRole, lambda:CreateFunction,");
        line(out, "               lambda:InvokeFunction on *");
        line(out, "    carol      Allow sts:AssumeRole on role/DevRole");
        line(out, "    DevRole    DevPolicy: Allow s3:*,");
        line(out, "               iam:CreatePolicyVersion on *");
        line(out, "    AdminRole  AdministratorAccess: Allow * on *");
        line(out, "  THE PATHS TO ADMINISTRATOR (each step asked of the evaluator):");
        for (Principal p : world.users()) {
            List<String> paths = Escalation.paths(p, world);
            if (paths.isEmpty()) {
                line(out, String.format("    %-6s no path found", p.name()));
            }
            for (String path : paths) {
                String[] steps = path.substring(p.name().length() + " -> ".length()).replace(" => ", " -> => ").replace(" + ", " -> + ").split(" -> ");
                for (int i = 0; i < steps.length; i++) {
                    line(out, String.format("    %-6s %s", i == 0 ? p.name() : "", (i == 0 || steps[i].startsWith("+") || steps[i].startsWith("=") ? "" : "-> ") + steps[i]));
                }
            }
        }
        Role.Session devSession = devRole.assume(carol, Map.of(), null, "carol");
        String policyArn = "arn:aws:iam::" + BANK + ":policy/DevPolicy";
        Principal rewritten = new Principal(BANK, devSession.principal().arn(), List.of(Policy.of("DevPolicy-v2", true, "*", "*")), null, null, Map.of());
        line(out, "  HOW CAROL'S PATH IS FOUND, step by step:");
        line(out, "    carol    sts:AssumeRole on role/DevRole       " + word(devSession.decision()).strip());
        line(out, "    session  iam:CreatePolicyVersion on DevPolicy " + word(Evaluator.decide(new Request(devSession.principal(), "iam:CreatePolicyVersion", policyArn), Evaluator.Guard.none(BANK))).strip());
        line(out, "    DevPolicy v2 says Allow * on *, and DevRole has it");
        line(out, "    session  is an administrator now              " + yes(Escalation.isAdmin(rewritten, BANK)));
        List<Policy> policies = List.of(devPolicy, adminRole.policies().get(0), world.users().get(1).policies().get(0));
        line(out, "  THE WILDCARDS in these policies (action or resource *):");
        for (Policy pol : policies) {
            boolean wild = pol.statements().stream().anyMatch(st -> st.actions().stream().anyMatch(a -> a.equals("*") || a.endsWith(":*")) || st.resources().contains("*"));
            line(out, String.format("    %-20s %s", pol.name(), wild ? "has a wildcard" : "none"));
        }
        long wildcards = policies.stream().flatMap(pol -> pol.statements().stream())
                .filter(st -> st.actions().stream().anyMatch(a -> a.equals("*") || a.endsWith(":*")) || st.resources().contains("*")).count();
        long statements = policies.stream().mapToLong(pol -> pol.statements().size()).sum();
        line(out, "    " + wildcards + " of " + statements + " statements: each wildcard is a decision nobody made");
    }
}
