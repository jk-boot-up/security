package com.jk.explore.security.dragonmodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public class ThreatDragonDemo {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final List<String> BREAKAGES = List.of("no title", "short type", "no id", "tiny");
    private static final List<String> MUTATIONS = List.of("no mitigation", "stale flag", "lost end", "bad count");

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        try (Dragon dragon = new Dragon()) {
            act1(out);
            act2(out);
            act3(out, dragon);
            act4(out, dragon);
            act5(out, dragon);
            act6(out, dragon);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void line(StringBuilder out, String format, Object... args) {
        line(out, String.format(format, args));
    }

    /** The configurations after 0, 1, 2 ... defences of the plan: the same plan as in the plain project. */
    static List<BankConfig> steps() {
        List<BankConfig> configs = new ArrayList<>();
        BankConfig c = BankConfig.weak();
        configs.add(c);
        for (Planner.Step s : Planner.plan(BankTree.baseline(), Defence.all(), 5_000)) {
            c = c.with(s.defence().configChange());
            configs.add(c);
        }
        return configs;
    }

    private static String percent(double p) {
        return String.format("%.0f%%", p * 100);
    }

    private static String eur(double cost) {
        return String.format("%,.0f EUR", cost);
    }

    private static JsonNode cells(String model) {
        return JSON.readTree(model).get("detail").get("diagrams").get(0).get("cells");
    }

    /** The trust boundary whose box holds this cell's box. */
    private static String inside(JsonNode cells, JsonNode cell) {
        for (JsonNode b : cells) {
            if (b.get("shape").asString().equals("trust-boundary-box")) {
                int bx = b.get("position").get("x").asInt();
                int by = b.get("position").get("y").asInt();
                int x = cell.get("position").get("x").asInt();
                int y = cell.get("position").get("y").asInt();
                if (x >= bx && y >= by && x < bx + b.get("size").get("width").asInt() && y < by + b.get("size").get("height").asInt()) {
                    return b.get("data").get("name").asString();
                }
            }
        }
        return "-";
    }

    private static void act1(StringBuilder out) {
        line(out, "ONE. The bank as a Threat Dragon diagram.");
        String model = ModelBuilder.json(BankConfig.weak());
        JsonNode cells = cells(model);
        Map<String, String> names = new HashMap<>();
        cells.forEach(c -> names.put(c.get("id").asString(), c.get("data").get("name").asString()));
        line(out, "  THE DIAGRAM: %d CELLS, WRITTEN BY THE PROGRAM", cells.size());
        for (JsonNode c : cells) {
            String type = c.get("data").get("type").asString();
            String name = c.get("data").get("name").asString();
            String where = switch (type) {
                case "tm.Boundary" -> "trust boundary";
                case "tm.Flow" -> names.get(c.get("source").get("cell").asString()) + " -> " + names.get(c.get("target").get("cell").asString());
                default -> "in " + inside(cells, c);
            };
            line(out, "    %-11s %-17s %s", type, name, where);
        }
        line(out, "  THE INPUT: THE 8 ABUSE CASES OF THE PLAIN PROJECT");
        for (AbuseCase c : AbuseCase.all()) {
            line(out, "    %-4s %s", c.id(), c.story().replaceFirst("^As (?:a |an )?(.+?) I ", "$1: ").replace("Ana's", "her").replace(" from my own device", ""));
        }
        line(out, "  THE RESULT: ONE THREAT PER CASE, ON THE CELL IT HITS");
        for (ModelChecks.Row r : ModelChecks.threats(model)) {
            line(out, "    %-4s %-17s  %-22s  %s", r.id(), r.cell(), r.type(), r.status());
        }
        line(out, "  THE FILE: version %s, %d threats, threatTop %d", ModelBuilder.VERSION, ModelChecks.threats(model).size(),
                JSON.readTree(model).get("detail").get("threatTop").asInt());
    }

    private static void act2(StringBuilder out) {
        line(out, "TWO. From the tree's numbers to a severity.");
        Node tree = BankTree.baseline();
        Cases.Entry ac1 = Cases.all(tree).get(0);
        List<Node.Leaf> steps = Cases.steps(tree, ac1.id());
        line(out, "  THE INPUT: THE TREE'S STEPS FOR %s (AND: ALL ARE NEEDED)", ac1.id());
        for (Node.Leaf l : steps) {
            line(out, "    %-4s %9s  %3s  %s", l.id(), eur(l.cost()), percent(l.probability()), l.name());
        }
        line(out, "  HOW THE SCORE IS WORKED OUT:");
        line(out, "    cost      %s = %s   (costs add up)", steps.stream().map(l -> String.format("%,.0f", l.cost())).collect(Collectors.joining(" + ")), eur(ac1.cost()));
        line(out, "    chance    %s = %s   (chances multiply)", steps.stream().map(l -> percent(l.probability())).collect(Collectors.joining(" x ")), percent(ac1.chance()));
        line(out, "    severity  %s, because %s", ac1.severity(), ac1.because());
        line(out, "  THE SEVERITY RULE:");
        line(out, "    High      cost <= 500 EUR or chance >= 50%");
        line(out, "    Medium    cost <= 5,000 EUR");
        line(out, "    Low       cost > 5,000 EUR");
        line(out, "  THE RESULT: ALL 8 CASES, SCORED");
        for (Cases.Entry e : Cases.all(tree)) {
            line(out, "    %-4s %10s  %3s  %-6s  %s", e.id(), eur(e.cost()), percent(e.chance()), e.severity(), e.because());
        }
        line(out, "  THE RESULT FOR %s, AS THE FILE HOLDS IT:", ac1.id());
        String model = ModelBuilder.json(BankConfig.weak());
        String block = model.substring(model.indexOf("\"title\" : \"" + ac1.id() + ":"));
        for (String key : List.of("type", "severity", "status", "score")) {
            String l = block.lines().filter(x -> x.strip().startsWith("\"" + key + "\" :")).findFirst().orElseThrow();
            line(out, "    " + l.strip().replaceAll(",$", ""));
        }
    }

    static String broken(String model, String how) {
        ObjectNode root = (ObjectNode) JSON.readTree(model);
        JsonNode diagram = root.get("detail").get("diagrams").get(0);
        switch (how) {
            case "no title" -> ((ObjectNode) root.get("summary")).remove("title");
            case "short type" -> ((ObjectNode) diagram).put("diagramType", "x");
            case "no id" -> ((ObjectNode) diagram.get("cells").get(2)).remove("id");
            case "tiny" -> ((ObjectNode) diagram.get("cells").get(2).get("size")).put("width", 5);
            default -> throw new IllegalArgumentException(how);
        }
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private static String file(String how) {
        return how.replace(' ', '-') + ".json";
    }

    private static void act3(StringBuilder out, Dragon dragon) throws Exception {
        line(out, "THREE. Checked by Threat Dragon's own validator.");
        String model = ModelBuilder.json(BankConfig.weak());
        Map<String, String> files = new LinkedHashMap<>();
        files.put("bank.json", model);
        BREAKAGES.forEach(how -> files.put(file(how), broken(model, how)));
        Map<String, Dragon.Verdict> verdicts = dragon.validate(files);
        line(out, "  THE VALIDATOR: THE SCHEMA AND AJV INSIDE THE IMAGE");
        line(out, "    " + Dragon.IMAGE.replaceAll("-arm64$", ""));
        line(out, "  THE INPUT: FIVE FILES");
        line(out, "    bank.json        the bank's model, as written");
        line(out, "    no-title.json    the summary's title removed");
        line(out, "    short-type.json  the diagram type set to \"x\"");
        line(out, "    no-id.json       the id of cell 2 removed");
        line(out, "    tiny.json        the width of cell 2 set to 5");
        line(out, "  THE RESULT: WHAT THE VALIDATOR SAYS");
        for (Map.Entry<String, Dragon.Verdict> v : verdicts.entrySet()) {
            line(out, "    %-16s %s", v.getKey(), v.getValue().valid() ? "valid" : "refused");
            for (String e : v.getValue().errors()) {
                int space = e.indexOf(' ');
                line(out, "      at    " + e.substring(0, space));
                line(out, "      says  " + e.substring(space + 1));
            }
        }
    }

    private static void act4(StringBuilder out, Dragon dragon) throws Exception {
        line(out, "FOUR. The plan, step by step, in the file.");
        List<BankConfig> steps = steps();
        List<Planner.Step> plan = Planner.plan(BankTree.baseline(), Defence.all(), 5_000);
        line(out, "  THE INPUT: THE PLAN OF THE PLAIN PROJECT");
        for (int i = 0; i < plan.size(); i++) {
            line(out, "    %d  %-8s %s", i + 1, plan.get(i).defence().id(), plan.get(i).defence().name());
        }
        Map<String, String> files = new LinkedHashMap<>();
        List<String> models = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            String m = ModelBuilder.json(steps.get(i));
            models.add(m);
            files.put("step" + i + ".json", m);
        }
        Map<String, Dragon.Verdict> verdicts = dragon.validate(files);
        List<AbuseCase> cases = AbuseCase.all();
        line(out, "  THE RESULT, STEP BY STEP:");
        line(out, "    step  defence  open threats  abuse cases failing  validator");
        for (int i = 0; i < steps.size(); i++) {
            BankConfig config = steps.get(i);
            long failing = cases.stream().filter(c -> !c.passes(config)).count();
            line(out, "    %-4d  %-7s  %-12d  %-19d  %s", i, i == 0 ? "(none)" : plan.get(i - 1).defence().id(), ModelChecks.count(models.get(i), "Open"), failing,
                    verdicts.get("step" + i + ".json").valid() ? "valid" : "invalid");
        }
        line(out, "  BEFORE THE PLAN AND AFTER IT, THREAT BY THREAT:");
        List<ModelChecks.Row> before = ModelChecks.threats(models.get(0));
        List<ModelChecks.Row> after = ModelChecks.threats(models.get(models.size() - 1));
        for (int i = 0; i < before.size(); i++) {
            ModelChecks.Row a = after.get(i);
            line(out, String.format("    %-4s %s -> %-9s  %s", a.id(), before.get(i).status(), a.status(), a.mitigation()).stripTrailing());
        }
        Map<String, String> byChange = new HashMap<>();
        Defence.all().forEach(d -> byChange.put(d.configChange(), d.id()));
        line(out, "  STILL OPEN, AND THE DEFENCE THAT WOULD CLOSE EACH:");
        for (Cases.Entry e : Cases.all(BankTree.baseline())) {
            if (after.stream().anyMatch(r -> r.id().equals(e.id()) && r.status().equals("Open"))) {
                line(out, "    %-4s %s, not in the plan", e.id(), e.answers().stream().map(byChange::get).sorted().collect(Collectors.joining(", ")));
            }
        }
    }

    private static String mutate(String model, String how) {
        ObjectNode root = (ObjectNode) JSON.readTree(model);
        ArrayNode cells = (ArrayNode) root.get("detail").get("diagrams").get(0).get("cells");
        switch (how) {
            case "no mitigation" -> {
                for (JsonNode c : cells) {
                    if (c.get("data").has("threats")) {
                        for (JsonNode t : c.get("data").get("threats")) {
                            if (t.get("status").asString().equals("Mitigated")) {
                                ((ObjectNode) t).put("mitigation", "");
                                return write(root);
                            }
                        }
                    }
                }
            }
            case "stale flag" -> {
                for (JsonNode c : cells) {
                    if (c.get("data").get("hasOpenThreats").asBoolean()) {
                        ((ObjectNode) c.get("data")).put("hasOpenThreats", false);
                        return write(root);
                    }
                }
            }
            case "lost end" -> {
                for (JsonNode c : cells) {
                    if (c.get("shape").asString().equals("flow")) {
                        ((ObjectNode) c.get("target")).put("cell", "00000000-0000-0000-0000-000000000000");
                        return write(root);
                    }
                }
            }
            case "bad count" -> ((ObjectNode) root.get("detail")).put("threatTop", 3);
            default -> throw new IllegalArgumentException(how);
        }
        return write(root);
    }

    private static String write(JsonNode root) {
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private static void act5(StringBuilder out, Dragon dragon) throws Exception {
        line(out, "FIVE. Checks the schema cannot make.");
        List<BankConfig> steps = steps();
        String start = ModelBuilder.json(steps.get(0));
        String end = ModelBuilder.json(steps.get(steps.size() - 1));
        line(out, "  THE %d RULES OVER THE FILE:", ModelChecks.RULES.size());
        ModelChecks.RULES.forEach((rule, what) -> line(out, "    %-29s  %s", rule, what));
        line(out, "  THE PLAN'S FILES: %d problems before, %d after", ModelChecks.check(start).size(), ModelChecks.check(end).size());
        line(out, "  BEFORE THE CHECK: THE LAST FILE, BROKEN FOUR WAYS");
        line(out, "    no mitigation  a Mitigated threat's mitigation emptied");
        line(out, "    stale flag     an open cell's hasOpenThreats set false");
        line(out, "    lost end       a flow's target set to a missing cell");
        line(out, "    bad count      threatTop set to 3");
        Map<String, String> files = new LinkedHashMap<>();
        MUTATIONS.forEach(how -> files.put(file(how), mutate(end, how)));
        Map<String, Dragon.Verdict> verdicts = dragon.validate(files);
        line(out, "  AFTER THE CHECK: THE SCHEMA, THEN THE RULES");
        for (String how : MUTATIONS) {
            List<ModelChecks.Problem> problems = ModelChecks.check(files.get(file(how)));
            line(out, "    %-13s  schema %s, %d rule broken", how, verdicts.get(file(how)).valid() ? "valid" : "refused", problems.size());
            for (ModelChecks.Problem p : problems) {
                line(out, "      %s: %s", p.rule(), p.where());
            }
        }
    }

    /** How many gates of this kind the tree has. */
    static int gates(Node n, Node.Kind kind) {
        return switch (n) {
            case Node.Leaf l -> 0;
            case Node.Gate g -> (g.kind() == kind ? 1 : 0) + g.children().stream().mapToInt(c -> gates(c, kind)).sum();
        };
    }

    private static void act6(StringBuilder out, Dragon dragon) throws Exception {
        line(out, "SIX. The editor, and the bill.");
        dragon.startServer();
        line(out, "  THE REQUESTS: THE IMAGE'S SERVER, ON A LOOPBACK PORT");
        List<String> paths = List.of("/healthz", "/", "/api/config", "/api/threatmodel/organisation/models");
        paths.forEach(p -> line(out, "    GET " + p));
        line(out, "  THE RESPONSES:");
        Map<String, Dragon.Reply> replies = new LinkedHashMap<>();
        for (String p : paths) {
            replies.put(p, dragon.get(p));
        }
        line(out, "    /healthz     %d", replies.get("/healthz").status());
        line(out, "    /            %d, titled %s", replies.get("/").status(), replies.get("/").body().replaceAll("(?s).*<title>([^<]*)</title>.*", "$1"));
        line(out, "    /api/config  %d, the sign-in providers it offers:", replies.get("/api/config").status());
        JSON.readTree(replies.get("/api/config").body()).get("data").properties()
                .forEach(e -> line(out, "      %-17s %s", e.getKey(), e.getValue().asString()));
        line(out, "    /api/threatmodel/organisation/models  %d, not signed in", replies.get("/api/threatmodel/organisation/models").status());
        Node tree = BankTree.baseline();
        String model = ModelBuilder.json(BankConfig.weak());
        line(out, "  THE BILL: WHAT THE FILE CANNOT HOLD");
        line(out, "    the gates   the tree has %d AND and %d OR; the file has none", gates(tree, Node.Kind.AND), gates(tree, Node.Kind.OR));
        line(out, "    the sums    cost and chance are text: \"%s\"", ModelChecks.threats(model).get(4).score());
        line(out, "    the tests   none; Mitigated is a claim, the abuse cases prove it");
    }
}
