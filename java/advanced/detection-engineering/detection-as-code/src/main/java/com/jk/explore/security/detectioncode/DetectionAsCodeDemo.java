package com.jk.explore.security.detectioncode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class DetectionAsCodeDemo {

    static final long SEED = 20260302L;
    static final Path RULES = Path.of("rules");

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static String f2(double d) {
        return String.format(Locale.ROOT, "%.2f", d);
    }

    static String f1(double d) {
        return String.format(Locale.ROOT, "%.1f", d);
    }

    static String yes(boolean b) {
        return b ? "yes" : "no";
    }

    /** The rules that are alerts on their own: everything except the base rule that only the correlation uses. */
    static List<String> scored(Map<String, Rule> rules) {
        return rules.keySet().stream().filter(n -> !n.equals("auth-failure")).toList();
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        Map<String, Rule> rules = Sigma.load(RULES);
        Corpus corpus = Corpus.generate(SEED);
        corpus.write(Path.of("build", "logs", "events.jsonl"));
        Map<String, Integer> budgets = budgets();

        out.append("--- the recorded logs\n");
        out.append(logs(corpus));

        out.append("--- the rules\n");
        long correlations = rules.values().stream().filter(r -> r.correlation() != null).count();
        Set<String> techniques = new TreeSet<>();
        rules.values().forEach(r -> techniques.addAll(r.techniques()));
        out.append("rules: " + rules.size() + " in " + (rules.size() - 1) + " files (" + (rules.size() - correlations) + " detections, " + correlations + " correlation), ATT&CK techniques tagged: " + techniques.size() + "\n");

        out.append("--- each rule against the truth\n");
        Map<String, Score> scores = new LinkedHashMap<>();
        for (String name : scored(rules)) {
            Score s = Score.of(name, rules, corpus);
            scores.put(name, s);
            out.append(String.format(Locale.ROOT, "%-27s caught %2d of %2d, missed %d, false alarms %3d, precision %s, recall %s%n", name, s.detected(), s.attacks(), s.missed(), s.falsePositives(), f2(s.precision()), f2(s.recall())));
        }
        int attacks = scores.values().stream().mapToInt(Score::attacks).sum();
        int detected = scores.values().stream().mapToInt(Score::detected).sum();
        int falseAlarms = scores.values().stream().mapToInt(Score::falsePositives).sum();
        out.append("all rules: caught " + detected + " of " + attacks + ", false alarms " + falseAlarms + "\n");

        out.append("--- false-positive budgets, false alarms per simulated day\n");
        int over = 0;
        for (Map.Entry<String, Score> e : scores.entrySet()) {
            Score s = e.getValue();
            int budget = budgets.get(e.getKey());
            over += s.daysOver(budget) > 0 ? 1 : 0;
            out.append(String.format(Locale.ROOT, "%-27s budget %d, mean %s, worst day %2d, days over %d of %d: %s%n", e.getKey(), budget, f1(s.meanPerDay()), s.worstDay(), s.daysOver(budget), Corpus.DAYS, s.daysOver(budget) == 0 ? "within" : "OVER"));
        }
        out.append("rules over budget: " + over + " of " + scores.size() + "\n");

        out.append("--- tuning a rule on the same logs\n");
        out.append(tuning(rules, corpus, scores, budgets));

        out.append("--- the rule tests\n");
        out.append(tests(rules));

        out.append("--- ATT&CK coverage\n");
        out.append(coverage(rules, corpus, scores));

        out.append("--- the rules converted by pySigma\n");
        Pysigma pysigma = new Pysigma(Path.of("build"));
        Map<String, Pysigma.Conversion> converted = pysigma.convert(RULES);
        long ok = converted.values().stream().filter(c -> c.error() == null).count();
        out.append("pySigma " + Pysigma.PYSIGMA + " with the OpenSearch backend " + Pysigma.OPENSEARCH_BACKEND + ": converted " + ok + " of " + converted.size() + " rules to Lucene queries\n");
        converted.forEach((n, c) -> {
            if (c.error() != null) {
                out.append("not converted: " + n + ": " + c.error() + "\n");
            }
        });

        out.append("--- the queries run in OpenSearch\n");
        out.append(openSearch(rules, corpus, converted));

        out.append("--- the bill\n");
        out.append("a rule is a hypothesis: the attacks it was written for are caught, the ones spelled another way are missed, and only a labelled corpus says how many\n");
        out.append("a filter that removes a false alarm is also a hole an attacker can use, so each one needs a test that says why it is there\n");
        out.append("a budget turns noise into a number: a rule over budget gets a filter or a higher threshold, or it is read by nobody\n");
        out.append("a threshold or a filter chosen on one week of logs fits that week: the next week is the test\n");
        out.append("covering a technique with a tag is not detecting it: coverage is what the labelled attacks show\n");
        return out.toString();
    }

    static Map<String, Integer> budgets() throws IOException {
        Map<String, Integer> out = new LinkedHashMap<>();
        Sigma.documents(java.nio.file.Files.readString(RULES.resolve("budgets.yaml"))).getFirst().forEach((k, v) -> out.put(k, Integer.parseInt(String.valueOf(v))));
        return out;
    }

    static String logs(Corpus c) {
        long benign = c.truth().values().stream().filter(t -> t.kind().equals("benign")).count();
        long filtered = c.truth().values().stream().filter(t -> t.kind().equals("filtered")).count();
        long lookalike = c.truth().values().stream().filter(t -> t.kind().equals("lookalike")).count();
        Set<Integer> instances = new TreeSet<>();
        Set<Integer> evasions = new TreeSet<>();
        Set<Integer> uncovered = new TreeSet<>();
        for (Truth t : c.truth().values()) {
            if (t.attack()) {
                instances.add(t.instance());
                if (t.kind().equals("evasion")) {
                    evasions.add(t.instance());
                } else if (t.kind().equals("uncovered")) {
                    uncovered.add(t.instance());
                }
            }
        }
        long attackEvents = c.truth().values().stream().filter(Truth::attack).count();
        return "events: " + c.events().size() + " over " + Corpus.DAYS + " days, " + c.events().size() / Corpus.DAYS + " a day\n"
                + "benign: " + benign + ", look-alikes with a filter in the rule: " + filtered + ", look-alikes without one: " + lookalike + "\n"
                + "attack events: " + attackEvents + " in " + instances.size() + " attacks (" + (instances.size() - evasions.size() - uncovered.size()) + " that a rule is written for, "
                + evasions.size() + " evasions of those rules, " + uncovered.size() + " with no rule)\n";
    }

    static String tuning(Map<String, Rule> rules, Corpus c, Map<String, Score> base, Map<String, Integer> budgets) {
        StringBuilder out = new StringBuilder();
        List<Mutator.Mutant> mutants = Mutator.of(rules.get("web-server-shell"));
        Mutator.Mutant noFilter = mutants.stream().filter(m -> m.what().equals("drop filter_healthcheck")).findFirst().orElseThrow();
        Score loose = Score.of("web-server-shell", Mutator.with(rules, noFilter.rule()), c);
        Score now = base.get("web-server-shell");
        out.append("web-server-shell without its health-check filter: false alarms " + now.falsePositives() + " -> " + loose.falsePositives() + ", worst day " + now.worstDay() + " -> " + loose.worstDay()
                + ", budget " + budgets.get("web-server-shell") + ", days over " + loose.daysOver(budgets.get("web-server-shell")) + " of " + Corpus.DAYS + "\n");
        Rule burst = rules.get("failed-login-burst");
        Score before = base.get("failed-login-burst");
        for (int gte : new int[] {5, 15, 20}) {
            Map<String, Object> doc = Sigma.documents(Mutator.dump(burst)).getFirst();
            Sigma.map(Sigma.map(doc.get("correlation")).get("condition")).put("gte", gte);
            Score s = Score.of("failed-login-burst", Mutator.with(rules, new Rule(doc)), c);
            out.append("failed-login-burst at " + gte + " failures in 5 minutes (now " + burst.correlation().gte() + "): caught " + s.detected() + " of " + s.attacks() + " (now " + before.detected() + "), false alarms " + s.falsePositives()
                    + " (now " + before.falsePositives() + ")\n");
        }
        return out.toString();
    }

    static String tests(Map<String, Rule> rules) throws IOException {
        StringBuilder out = new StringBuilder();
        Map<String, List<RuleTests.Case>> tests = RuleTests.load(RULES.resolve("tests"));
        int cases = 0;
        int shouldMatch = 0;
        int failing = 0;
        for (Map.Entry<String, List<RuleTests.Case>> e : tests.entrySet()) {
            cases += e.getValue().size();
            shouldMatch += (int) e.getValue().stream().filter(RuleTests.Case::expect).count();
            failing += RuleTests.failures(e.getValue(), rules.get(e.getKey()), rules).size();
        }
        List<String> untested = rules.keySet().stream().filter(n -> !tests.containsKey(n)).toList();
        out.append("test files: " + tests.size() + ", cases: " + cases + " (" + shouldMatch + " should match, " + (cases - shouldMatch) + " should not), failing: " + failing + ", rules with no test file: " + untested.size() + "\n");

        int total = 0;
        int killed = 0;
        int loosen = 0;
        int tighten = 0;
        List<String> survivors = new ArrayList<>();
        for (Rule r : rules.values()) {
            for (Mutator.Mutant m : Mutator.of(r)) {
                total++;
                loosen += m.direction().equals("loosen") ? 1 : 0;
                tighten += m.direction().equals("tighten") ? 1 : 0;
                if (!RuleTests.failures(tests.get(r.name()), m.rule(), Mutator.with(rules, m.rule())).isEmpty()) {
                    killed++;
                } else {
                    survivors.add(m.label());
                }
            }
        }
        out.append("mutants (a filter dropped, a field or a value dropped, and to or, a threshold moved): " + total + " (" + loosen + " loosen, " + tighten + " tighten)\n");
        out.append("killed by the tests: " + killed + " of " + total + ", survived: " + survivors.size() + "\n");
        survivors.forEach(s -> out.append("  survivor: " + s + "\n"));
        Mutator.Mutant noFilter = Mutator.of(rules.get("web-server-shell")).stream().filter(m -> m.what().equals("drop filter_healthcheck")).findFirst().orElseThrow();
        List<String> failed = RuleTests.failures(tests.get("web-server-shell"), noFilter.rule(), Mutator.with(rules, noFilter.rule()));
        out.append("web-server-shell without its filter: " + failed.size() + " of " + tests.get("web-server-shell").size() + " cases fail (" + String.join("; ", failed) + ")\n");
        return out.toString();
    }

    static String coverage(Map<String, Rule> rules, Corpus c, Map<String, Score> scores) {
        StringBuilder out = new StringBuilder();
        Set<Integer> alerted = new TreeSet<>();
        for (String name : scored(rules)) {
            alerted.addAll(Engine.alerts(rules.get(name), rules, c.events()));
        }
        List<Coverage.Row> rows = Coverage.matrix(rules, c, alerted);
        for (Coverage.Row r : rows) {
            out.append(String.format(Locale.ROOT, "%-10s %-48s attacks %2d, caught %2d: %s%n", r.technique(), r.rules().isEmpty() ? "-" : String.join(",", r.rules()), r.attacks(), r.detected(), r.status()));
        }
        out.append("techniques with a rule and every labelled attack caught: " + rows.stream().filter(r -> r.status().equals("covered")).count() + " of " + rows.size() + "\n");
        out.append("gaps, attacked and no rule: " + String.join(", ", rows.stream().filter(r -> r.status().equals("GAP no rule")).map(r -> r.technique() + " (" + r.attacks() + ")").toList()) + "\n");
        out.append("partial, a rule that misses some of the attacks: " + String.join(", ", rows.stream().filter(r -> r.status().equals("partial")).map(r -> r.technique() + " (" + (r.attacks() - r.detected()) + " missed)").toList()) + "\n");
        out.append("tagged but never attacked in the logs: " + String.join(", ", rows.stream().filter(r -> r.status().startsWith("untested")).map(Coverage.Row::technique).toList()) + "\n");
        return out.toString();
    }

    static String openSearch(Map<String, Rule> rules, Corpus corpus, Map<String, Pysigma.Conversion> converted) throws Exception {
        if (!OpenSearch.dockerRuns()) {
            return "Docker does not run: the converted queries were not run in OpenSearch\n";
        }
        StringBuilder out = new StringBuilder();
        try (OpenSearch os = new OpenSearch()) {
            os.index(corpus);
            out.append("OpenSearch, one node, security plugin off; events indexed: " + corpus.events().size() + ", every text field a keyword compared with no regard to case\n");
            int same = 0;
            int queries = 0;
            int different = 0;
            for (Rule rule : rules.values()) {
                if (rule.correlation() != null) {
                    continue;
                }
                Set<Integer> java = new TreeSet<>(Engine.alerts(rule, rules, corpus.events()));
                Set<Integer> opensearch = os.hits(converted.get(rule.name()).queries().getFirst());
                queries++;
                Set<Integer> only = new TreeSet<>(java);
                only.addAll(opensearch);
                Set<Integer> both = new TreeSet<>(java);
                both.retainAll(opensearch);
                only.removeAll(both);
                different += only.size();
                boolean agree = java.equals(opensearch);
                same += agree ? 1 : 0;
                out.append(String.format(Locale.ROOT, "%-27s java %3d, opensearch %3d, the same events: %s%n", rule.name(), java.size(), opensearch.size(), yes(agree)));
                if (rule.name().equals("auth-failure")) {
                    Rule burst = rules.get("failed-login-burst");
                    List<Ev> hitEvents = corpus.events().stream().filter(e -> opensearch.contains(e.id())).toList();
                    List<Integer> fromOpenSearch = Engine.count(burst.correlation(), hitEvents);
                    List<Integer> fromJava = Engine.alerts(burst, rules, corpus.events());
                    out.append(String.format(Locale.ROOT, "%-27s the backend has no correlation; the count over OpenSearch's failed logins gives %d alerts, Java %d, the same: %s%n", "failed-login-burst",
                            fromOpenSearch.size(), fromJava.size(), yes(fromOpenSearch.equals(fromJava))));
                }
            }
            int events = corpus.events().size();
            out.append("queries run: " + queries + " over " + events + " events, " + same + " of " + queries + " return exactly the events of the Java evaluation, events decided differently: " + different + "\n");
        }
        return out.toString();
    }
}
