package com.jk.explore.security.k8snetpol;

import java.util.ArrayList;
import java.util.List;

public class KubernetesNetworkPoliciesDemo {

    private final StringBuilder out = new StringBuilder();

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    private void act(String heading) {
        out.append(heading).append('\n');
    }

    /** A labelled section of an act: its header, then its lines. */
    private void section(String header, List<String> lines) {
        out.append("  ").append(header).append('\n');
        lines.forEach(l -> out.append("    ").append(l).append('\n'));
    }

    private void section(String header, String... lines) {
        section(header, List.of(lines));
    }

    /** "networkpolicy.networking.k8s.io/x created" -> "created". */
    static String verdict(List<String> answers) {
        return answers.stream().map(a -> a.substring(a.lastIndexOf(' ') + 1)).distinct().reduce((a, b) -> a + ", " + b).orElse("");
    }

    /** The pods of a namespace that a selector does not pick. */
    static List<String> leftOut(String namespace, List<String> selected) {
        return Lab.PODS.stream().filter(w -> w.namespace().equals(namespace) && !selected.contains(w.name())).map(Workload::name).toList();
    }

    private static String probe(String what, int exit) {
        return String.format("%-24s %s", what, Report.exit(exit));
    }

    public static String run() throws Exception {
        return new KubernetesNetworkPoliciesDemo().demo();
    }

    private String demo() throws Exception {
        try (Lab lab = new Lab()) {
            flat(lab);
            defaultDeny(lab);
            mistakes(lab);
            egress(lab);
            label(lab);
        }
        return out.toString();
    }

    private void flat(Lab lab) throws Exception {
        act("ONE. A flat network: no policy at all.");
        section("THE PODS (a policy selects a pod by its label):", Report.pods(Lab.PODS));
        section("THE DESIGN (" + Lab.INTENT.allowed().size() + " pairs may talk):", Report.design(Lab.INTENT, Lab.PODS));
        section("THE POLICIES:", "none");
        var reachable = lab.measure();
        section("THE MEASUREMENT (curl, row to column; o answered):", Report.matrix(Lab.PODS, reachable));
        section("HOW MANY PAIRS (n pods make n x (n - 1)):", Report.pairCount(Lab.PODS.size()));
        section("AGAINST THE DESIGN:", Report.against(Lab.INTENT.compare(Lab.PODS, reachable)));
    }

    private void defaultDeny(Lab lab) throws Exception {
        act("TWO. Default deny, then one allow for each line of the design.");
        lab.apply(Lab.DENY_ALL);
        section("STEP 1, THE POLICIES (a default deny in each namespace):", Report.policies(Lab.DENY_ALL));
        section("STEP 1, THE MEASUREMENT:", Report.against(Lab.INTENT.compare(Lab.PODS, lab.measure())));
        lab.apply(Lab.ALLOWS);
        section("STEP 2, THE POLICIES ADDED (one for each line of the design):", Report.policies(Lab.ALLOWS));
        var reachable = lab.measure();
        section("STEP 2, THE MEASUREMENT (o answered):", Report.matrix(Lab.PODS, reachable));
        Intent.Diff d = Lab.INTENT.compare(Lab.PODS, reachable);
        List<String> against = new ArrayList<>(Report.against(d));
        against.add("equals the design: " + (d.matches() ? "yes" : "no"));
        section("AGAINST THE DESIGN:", against);
        Policy first = Lab.ALLOWS.getFirst();
        section("ONE ALLOW, LINE BY LINE (" + first.name() + "):", first.manifest().lines().toList());
    }

    private void mistakes(Lab lab) throws Exception {
        act("THREE. Three mistakes that the API server accepts.");
        Policy wide = Policies.allowIngress("pay", "api", "shop", null);
        mistake(lab, 1, Lab.ALLOWS.getFirst(), wide, "app=api");
        Policy typo = Policies.allowIngress("pay", "apii", "shop", "web");
        mistake(lab, 2, Lab.ALLOWS.getFirst(), typo, "app=apii");
        Policy narrow = Policies.denyIngressFor("shop", "web");
        mistake(lab, 3, Lab.DENY_ALL.getFirst(), narrow, "app=web");
    }

    /** The design's policies with one replaced by a mistake: what the API server says, what it selects, what the pods can reach. */
    private void mistake(Lab lab, int n, Policy replaced, Policy wrong, String selector) throws Exception {
        lab.reset();
        List<Policy> policies = new ArrayList<>(Lab.DENY_ALL);
        policies.addAll(Lab.ALLOWS);
        List<String> answers = lab.apply(policies.stream().map(p -> p == replaced ? wrong : p).toList());
        List<String> selected = lab.cluster.podsLabelled(wrong.namespace(), selector);
        List<String> lines = new ArrayList<>(Report.policies(List.of(wrong)));
        lines.add(String.format("%-10s %s", "selects", selected.isEmpty() ? "no pod (" + selector + " in " + wrong.namespace() + ")" : String.join(", ", selected)));
        List<String> left = leftOut(wrong.namespace(), selected);
        if (n == 3) {
            lines.add(String.format("%-10s %s, now with no policy at all", "leaves", String.join(", ", left)));
        }
        lines.add(String.format("%-10s %s", "accepted", "yes, the API server says " + verdict(answers)));
        section("MISTAKE " + n + ", THE POLICY (replaces " + replaced.name() + " in " + replaced.namespace() + "):", lines);
        section("MISTAKE " + n + ", THE MEASUREMENT:", Report.against(Lab.INTENT.compare(Lab.PODS, lab.measure())));
    }

    private void egress(Lab lab) throws Exception {
        act("FOUR. Egress, and the name lookup that stops with it.");
        lab.reset();
        lab.applyIntended();
        section("BEFORE (the design's policies, nothing on egress):",
                probe("api -> outside server", lab.toOutside(Lab.API)),
                probe("api -> db by name", lab.dbByName(Lab.API)));
        List<Policy> egress = List.of(Policies.denyAllEgress("pay"), Policies.allowEgress("pay", "api", "pay", "db"));
        lab.apply(egress);
        section("THE POLICIES ADDED (on what leaves pay):", Report.policies(egress));
        List<String> after = new ArrayList<>(Report.against(Lab.INTENT.compare(Lab.PODS, lab.measure())).subList(0, 1));
        after.add(probe("api -> outside server", lab.toOutside(Lab.API)));
        after.add(probe("api -> db by address", lab.dbByAddress(Lab.API)));
        after.add(probe("api -> db by name", lab.dbByName(Lab.API)));
        section("AFTER:", after);
        List<String> dns = lab.cluster.podsLabelled("kube-system", "k8s-app=kube-dns");
        section("WHY THE NAME FAILS (a name is looked up first):",
                "db.pay.svc.cluster.local  asked of kube-dns, port 53",
                "kube-dns  " + dns.size() + " pods in kube-system, k8s-app=kube-dns",
                "no egress allow names them, so the question is dropped");
        Policy allowDns = Policies.allowDns("pay");
        lab.apply(allowDns);
        section("THE POLICY ADDED:", Report.policies(List.of(allowDns)));
        section("AFTER ALLOWING DNS:",
                probe("api -> db by name", lab.dbByName(Lab.API)),
                probe("api -> outside server", lab.toOutside(Lab.API)));
    }

    private void label(Lab lab) throws Exception {
        act("FIVE. The label is the credential.");
        lab.reset();
        lab.applyIntended();
        List<String> before = new ArrayList<>();
        before.add(String.format("%-10s app=%s", Lab.JOBS.id(), lab.cluster.appLabel(Lab.JOBS)));
        before.addAll(Report.against(Lab.INTENT.compare(Lab.PODS, lab.measure())));
        section("BEFORE (the design's policies):", before);
        section("THE CHANGE (by anyone who may edit pods in shop):", "kubectl -n shop label pod jobs app=web --overwrite");
        lab.cluster.must("-n", "shop", "label", "pod", "jobs", "app=web", "--overwrite");
        Thread.sleep(6_000);
        List<String> after = new ArrayList<>();
        after.add(String.format("%-10s app=%s", Lab.JOBS.id(), lab.cluster.appLabel(Lab.JOBS)));
        after.addAll(Report.against(Lab.INTENT.compare(Lab.PODS, lab.measure())));
        section("AFTER:", after);
        Policy allow = Lab.ALLOWS.getFirst();
        boolean inShop = Lab.JOBS.namespace().equals("shop");
        boolean isWeb = lab.cluster.podsLabelled("shop", "app=web").contains(Lab.JOBS.name());
        section("WHY (" + allow.name() + " asks two questions):",
                String.format("%-24s jobs: %s", "namespace is shop?", inShop ? "yes" : "no"),
                String.format("%-24s jobs: %s", "label app=web?", isWeb ? "yes" : "no"),
                "both yes, so jobs gets the store front's access");
    }
}
