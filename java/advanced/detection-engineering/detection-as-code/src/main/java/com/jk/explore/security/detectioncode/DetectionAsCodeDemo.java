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

    /** Three process starts on a bank host: a web shell, the application server's health check, and a web shell under another server name. */
    static final List<Map<String, String>> THREE_EVENTS = List.of(
            Map.of("ParentImage", "/usr/sbin/nginx", "Image", "/bin/sh", "CommandLine", "sh -c id"),
            Map.of("ParentImage", "/usr/lib/jvm/java-21/bin/java", "Image", "/bin/sh", "CommandLine", "/bin/sh -c /opt/bank/bin/healthcheck --deep"),
            Map.of("ParentImage", "/usr/sbin/apache2", "Image", "/bin/sh", "CommandLine", "sh -c id"));
    static final List<String> THREE_TRUTHS = List.of("attack (a web shell runs id)", "benign (the health check)",
            "attack (the same, under apache2)");

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

    /** The whole demo: acts ONE to FIVE need no tools; act SIX runs pySigma and OpenSearch in Docker. */
    public static String run() throws Exception {
        Map<String, Rule> rules = Sigma.load(RULES);
        Corpus corpus = Corpus.generate(SEED);
        corpus.write(Path.of("build", "logs", "events.jsonl"));
        return offline(rules, corpus) + tools(rules, corpus);
    }

    /** Acts ONE to FIVE: Java only, the same on every machine. */
    static String offline(Map<String, Rule> rules, Corpus corpus) throws IOException {
        StringBuilder out = new StringBuilder();
        Map<String, Integer> budgets = budgets();
        Map<String, Score> scores = new LinkedHashMap<>();
        for (String name : scored(rules)) {
            scores.put(name, Score.of(name, rules, corpus));
        }
        out.append("ONE. One Sigma rule against three events.\n");
        out.append(oneRule(rules.get("web-server-shell")));
        out.append("TWO. A week of labelled logs, and each rule scored.\n");
        out.append(logs(corpus));
        out.append(scoring(scores));
        out.append("THREE. False-alarm budgets, and tuning a rule on the same logs.\n");
        out.append(budgetCheck(scores, budgets));
        out.append(tuning(rules, corpus, scores, budgets));
        out.append("FOUR. Tests beside every rule, and mutants that check the tests.\n");
        out.append(tests(rules));
        out.append("FIVE. ATT&CK coverage: what the tags say, and what the attacks show.\n");
        out.append(coverage(rules, corpus));
        return out.toString();
    }

    static String oneRule(Rule rule) {
        StringBuilder out = new StringBuilder();
        out.append("  THE RULE " + rule.name() + " (level " + rule.doc().get("level") + "):\n");
        out.append("    tags: " + String.join(", ", rule.techniques()) + "\n");
        ConditionSteps.describe(rule).forEach(l -> out.append("    " + l + "\n"));
        String condition = String.valueOf(Sigma.map(rule.doc().get("detection")).get("condition"));
        int cut = condition.indexOf(" and not ");
        out.append("    condition: " + condition.substring(0, cut) + "\n");
        out.append("      " + condition.substring(cut + 1) + "\n");
        out.append("  THE EVENTS (three process starts on a bank host):\n");
        for (int i = 0; i < THREE_EVENTS.size(); i++) {
            Map<String, String> e = THREE_EVENTS.get(i);
            out.append("    " + (i + 1) + "  " + e.get("ParentImage") + " starts " + e.get("Image") + "\n");
            out.append("       " + e.get("CommandLine") + "\n");
            out.append("       truth  " + THREE_TRUTHS.get(i) + "\n");
        }
        out.append("  HOW THE CONDITION IS DECIDED:\n");
        out.append("    event  parent  child  filter  alert  verdict\n");
        for (int i = 0; i < THREE_EVENTS.size(); i++) {
            Map<String, Boolean> s = ConditionSteps.selections(rule, THREE_EVENTS.get(i));
            boolean alert = rule.matches(THREE_EVENTS.get(i));
            boolean attack = THREE_TRUTHS.get(i).startsWith("attack");
            String verdict = alert == attack ? (alert ? "caught" : "rightly quiet") : (alert ? "false alarm" : "missed");
            out.append(String.format(Locale.ROOT, "    %-6s %-7s %-6s %-7s %-6s %s%n", i + 1, yes(s.get("selection_parent")), yes(s.get("selection_child")),
                    yes(s.get("filter_healthcheck")), yes(alert), verdict));
        }
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
        return "  THE RECORDED LOGS (a seeded week of a bank's events):\n"
                + "    events       " + c.events().size() + " over " + Corpus.DAYS + " days, " + c.events().size() / Corpus.DAYS + " a day\n"
                + "    benign       " + benign + "\n"
                + "    look-alikes  " + filtered + " that a filter in the rule removes\n"
                + "                 " + lookalike + " that no filter removes\n"
                + "    attacks      " + instances.size() + ", in " + attackEvents + " events, each labelled\n"
                + "                 " + (instances.size() - evasions.size() - uncovered.size()) + " that a rule is written for\n"
                + "                 " + evasions.size() + " evasions: the same attack spelled another way\n"
                + "                 " + uncovered.size() + " that no rule is written for\n";
    }

    static String scoring(Map<String, Score> scores) {
        StringBuilder out = new StringBuilder("  EACH RULE AGAINST THE TRUTH (false: false alarms):\n");
        out.append("    rule                        caught    false  P     R\n");
        for (Score s : scores.values()) {
            out.append(String.format(Locale.ROOT, "    %-27s %2d of %2d  %5d  %s  %s%n", s.rule(), s.detected(), s.attacks(), s.falsePositives(), f2(s.precision()), f2(s.recall())));
        }
        int attacks = scores.values().stream().mapToInt(Score::attacks).sum();
        int detected = scores.values().stream().mapToInt(Score::detected).sum();
        int falseAlarms = scores.values().stream().mapToInt(Score::falsePositives).sum();
        out.append(String.format(Locale.ROOT, "    %-27s %2d of %2d  %5d%n", "all rules", detected, attacks, falseAlarms));
        Score s = scores.get("log-clearing");
        out.append("  HOW PRECISION (P) AND RECALL (R) ARE MADE (" + s.rule() + "):\n");
        out.append("    R = attacks caught / attacks = " + s.detected() + " / " + s.attacks() + " = " + f2(s.recall()) + "\n");
        out.append("    P = alerts on its attacks / (those + false alarms)\n");
        out.append("      = " + s.tpAlerts() + " / (" + s.tpAlerts() + " + " + s.falsePositives() + ") = " + f2(s.precision()) + "\n");
        return out.toString();
    }

    static String budgetCheck(Map<String, Score> scores, Map<String, Integer> budgets) {
        StringBuilder out = new StringBuilder("  THE BUDGETS (false alarms a day, set with the analysts):\n");
        out.append("    rule                        budget  mean  worst  days over\n");
        int over = 0;
        for (Score s : scores.values()) {
            int budget = budgets.get(s.rule());
            over += s.daysOver(budget) > 0 ? 1 : 0;
            out.append(String.format(Locale.ROOT, "    %-27s %6d  %4s  %5d  %d of %d %s%n", s.rule(), budget, f1(s.meanPerDay()), s.worstDay(), s.daysOver(budget), Corpus.DAYS,
                    s.daysOver(budget) == 0 ? "within" : "OVER"));
        }
        out.append("    rules over budget: " + over + " of " + scores.size() + "\n");
        Score s = scores.get("log-clearing");
        int budget = budgets.get(s.rule());
        StringBuilder days = new StringBuilder();
        StringBuilder overDays = new StringBuilder();
        for (int d = 0; d < Corpus.DAYS; d++) {
            days.append(" ").append(s.falsePerDay()[d]);
            if (s.falsePerDay()[d] > budget) {
                overDays.append(" ").append(d + 1);
            }
        }
        out.append("  HOW A BUDGET IS CHECKED (" + s.rule() + ", budget " + budget + "):\n");
        out.append("    false alarms on days 1 to " + Corpus.DAYS + ":" + days + "\n");
        out.append("    mean " + s.falsePositives() + " / " + Corpus.DAYS + " = " + f1(s.meanPerDay()) + ", worst day " + s.worstDay() + "\n");
        out.append("    over " + budget + " on days" + overDays + ": " + s.daysOver(budget) + " of " + Corpus.DAYS + ", OVER\n");
        return out.toString();
    }

    static String tuning(Map<String, Rule> rules, Corpus c, Map<String, Score> base, Map<String, Integer> budgets) {
        StringBuilder out = new StringBuilder();
        Mutator.Mutant noFilter = Mutator.of(rules.get("web-server-shell")).stream().filter(m -> m.what().equals("drop filter_healthcheck")).findFirst().orElseThrow();
        Score loose = Score.of("web-server-shell", Mutator.with(rules, noFilter.rule()), c);
        Score now = base.get("web-server-shell");
        int budget = budgets.get("web-server-shell");
        out.append("  BEFORE THE FILTER IS DROPPED (web-server-shell, budget " + budget + "):\n");
        out.append("    false alarms " + now.falsePositives() + ", worst day " + now.worstDay() + ", days over " + now.daysOver(budget) + " of " + Corpus.DAYS + "\n");
        out.append("  AFTER THE FILTER IS DROPPED (the health check alerts):\n");
        out.append("    false alarms " + loose.falsePositives() + ", worst day " + loose.worstDay() + ", days over " + loose.daysOver(budget) + " of " + Corpus.DAYS + "\n");
        Rule burst = rules.get("failed-login-burst");
        out.append("  A THRESHOLD MOVED (failed-login-burst: N failed logins\n");
        out.append("  from one address in 5 minutes, now N = " + burst.correlation().gte() + "):\n");
        out.append("    N         caught    false alarms\n");
        for (int gte : new int[] {5, burst.correlation().gte(), 15, 20}) {
            Map<String, Object> doc = Sigma.documents(Mutator.dump(burst)).getFirst();
            Sigma.map(Sigma.map(doc.get("correlation")).get("condition")).put("gte", gte);
            Score s = Score.of("failed-login-burst", Mutator.with(rules, new Rule(doc)), c);
            out.append(String.format(Locale.ROOT, "    %-9s %2d of %2d  %d%n", gte == burst.correlation().gte() ? gte + " (now)" : String.valueOf(gte), s.detected(), s.attacks(), s.falsePositives()));
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
        List<RuleTests.Case> shell = tests.get("web-server-shell");
        out.append("  THE CASES OF web-server-shell (rules/tests):\n");
        for (RuleTests.Case c : shell) {
            out.append(String.format(Locale.ROOT, "    %-9s %s%n", c.expect() ? "match" : "no match", c.name()));
        }
        out.append("  EVERY RULE'S CASES, RUN:\n");
        out.append("    files " + tests.size() + ", cases " + cases + ": " + shouldMatch + " should match, " + (cases - shouldMatch) + " should not\n");
        out.append("    failing " + failing + ", rules with no test file " + untested.size() + "\n");

        out.append("  THE MUTANTS OF web-server-shell (one small change each):\n");
        for (Mutator.Mutant m : Mutator.of(rules.get("web-server-shell"))) {
            int failed = RuleTests.failures(shell, m.rule(), Mutator.with(rules, m.rule())).size();
            out.append(String.format(Locale.ROOT, "    %-8s %-27s %d of %d fail%n", m.direction(), m.what().replaceFirst(" from .*", "").replace("the value ", ""), failed, shell.size()));
        }

        int total = 0;
        int killed = 0;
        int loosen = 0;
        List<String> survivors = new ArrayList<>();
        for (Rule r : rules.values()) {
            for (Mutator.Mutant m : Mutator.of(r)) {
                total++;
                loosen += m.direction().equals("loosen") ? 1 : 0;
                if (!RuleTests.failures(tests.get(r.name()), m.rule(), Mutator.with(rules, m.rule())).isEmpty()) {
                    killed++;
                } else {
                    survivors.add(m.label());
                }
            }
        }
        out.append("  EVERY RULE'S MUTANTS, RUN AGAINST ITS CASES:\n");
        out.append("    mutants " + total + ": " + loosen + " loosen, " + (total - loosen) + " tighten\n");
        out.append("    killed by a failing case " + killed + " of " + total + ", survived " + survivors.size() + "\n");
        survivors.forEach(s -> out.append("    survivor: " + s + "\n"));
        return out.toString();
    }

    static String coverage(Map<String, Rule> rules, Corpus c) {
        StringBuilder out = new StringBuilder();
        Set<Integer> alerted = new TreeSet<>();
        for (String name : scored(rules)) {
            alerted.addAll(Engine.alerts(rules.get(name), rules, c.events()));
        }
        List<Coverage.Row> rows = Coverage.matrix(rules, c, alerted);
        long tagged = rows.stream().filter(r -> !r.rules().isEmpty()).count();
        long attacked = rows.stream().filter(r -> r.attacks() > 0).count();
        out.append("  THE TAGS AND THE ATTACKS:\n");
        out.append("    techniques the rules are tagged with  " + tagged + "\n");
        out.append("    techniques the labelled attacks used  " + attacked + "\n");
        out.append("  THE MATRIX (one row a technique):\n");
        out.append("    technique  attacks  caught  status    rule\n");
        for (Coverage.Row r : rows) {
            String rule = r.rules().isEmpty() ? "-" : r.rules().getFirst() + (r.rules().size() > 1 ? " +" + (r.rules().size() - 1) : "");
            String status = r.status().startsWith("untested") ? "untested" : r.status().equals("GAP no rule") ? "GAP" : r.status();
            out.append(String.format(Locale.ROOT, "    %-10s %7d %7d  %-9s %s%n", r.technique(), r.attacks(), r.detected(), status, rule));
        }
        out.append("  HOW THE STATUS IS DECIDED:\n");
        out.append("    GAP       attacked, no rule tagged       " + count(rows, "GAP no rule") + "\n");
        out.append("    partial   a rule, some attacks missed    " + count(rows, "partial") + "\n");
        out.append("    covered   a rule, every attack caught    " + count(rows, "covered") + "\n");
        out.append("    untested  a rule, no labelled attack     " + rows.stream().filter(r -> r.status().startsWith("untested")).count() + "\n");
        out.append("    covered " + count(rows, "covered") + " of " + rows.size() + ": a tag alone is not a detection\n");
        return out.toString();
    }

    private static long count(List<Coverage.Row> rows, String status) {
        return rows.stream().filter(r -> r.status().equals(status)).count();
    }

    /** Act SIX: the rules converted by pySigma, and the queries run in OpenSearch in Docker. */
    static String tools(Map<String, Rule> rules, Corpus corpus) throws Exception {
        StringBuilder out = new StringBuilder("SIX. pySigma turns the rules into queries; OpenSearch runs them.\n");
        Pysigma pysigma = new Pysigma(Path.of("build"));
        Map<String, Pysigma.Conversion> converted = pysigma.convert(RULES);
        long ok = converted.values().stream().filter(c -> c.error() == null).count();
        out.append("  THE QUERY pySigma MADE (Lucene, for OpenSearch):\n");
        wrap(converted.get("web-server-shell").queries().getFirst(), 62).forEach(l -> out.append("    " + l + "\n"));
        out.append("  THE CONVERSION OF EVERY RULE:\n");
        out.append("    pySigma " + Pysigma.PYSIGMA + ", OpenSearch backend " + Pysigma.OPENSEARCH_BACKEND + ": " + ok + " of " + converted.size() + " converted\n");
        converted.forEach((n, c) -> {
            if (c.error() != null) {
                out.append("    not converted: " + n + "\n");
                out.append("      " + c.error() + "\n");
            }
        });
        out.append(openSearch(rules, corpus, converted));
        return out.toString();
    }

    /** A query cut into lines at the spaces that are not escaped, each line at most width characters where it can be. */
    static List<String> wrap(String query, int width) {
        List<String> words = new ArrayList<>();
        StringBuilder w = new StringBuilder();
        for (int i = 0; i < query.length(); i++) {
            char ch = query.charAt(i);
            if (ch == ' ' && (i == 0 || query.charAt(i - 1) != '\\')) {
                words.add(w.toString());
                w.setLength(0);
            } else {
                w.append(ch);
            }
        }
        words.add(w.toString());
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            boolean breakHere = word.equals("AND") && !line.isEmpty();
            if (!line.isEmpty() && (breakHere || line.length() + 1 + word.length() > width)) {
                lines.add(line.toString());
                line.setLength(0);
            }
            line.append(line.isEmpty() ? "" : " ").append(word);
        }
        lines.add(line.toString());
        return lines;
    }

    static String openSearch(Map<String, Rule> rules, Corpus corpus, Map<String, Pysigma.Conversion> converted) throws Exception {
        if (!OpenSearch.dockerRuns()) {
            return "  Docker does not run: the converted queries were not run in OpenSearch\n";
        }
        StringBuilder out = new StringBuilder();
        try (OpenSearch os = new OpenSearch()) {
            os.index(corpus);
            out.append("  THE QUERIES RUN IN OPENSEARCH " + OpenSearch.IMAGE.substring(OpenSearch.IMAGE.indexOf(':') + 1) + " (" + corpus.events().size() + " events):\n");
            out.append("    rule                        java  opensearch  same\n");
            int same = 0;
            int queries = 0;
            int different = 0;
            String burstLine = "";
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
                out.append(String.format(Locale.ROOT, "    %-27s %4d  %10d  %s%n", rule.name(), java.size(), opensearch.size(), yes(agree)));
                if (rule.name().equals("auth-failure")) {
                    Rule burst = rules.get("failed-login-burst");
                    List<Ev> hitEvents = corpus.events().stream().filter(e -> opensearch.contains(e.id())).toList();
                    List<Integer> fromOpenSearch = Engine.count(burst.correlation(), hitEvents);
                    List<Integer> fromJava = Engine.alerts(burst, rules, corpus.events());
                    burstLine = String.format(Locale.ROOT, "    %-27s %4d  %10d  %s%n", "failed-login-burst *", fromJava.size(), fromOpenSearch.size(), yes(fromOpenSearch.equals(fromJava)));
                }
            }
            out.append(burstLine);
            out.append("    * counted in Java over the failed logins OpenSearch found\n");
            out.append("  THE RESULT: " + same + " of " + queries + " queries return the Java events, " + different + " differ\n");
        }
        return out.toString();
    }
}
