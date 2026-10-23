package com.jk.explore.security.detectioncode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * The part of Sigma this project needs, evaluated in Java: selections of fields with the modifiers contains, startswith, endswith and all,
 * wildcards, matching with no regard to case, a missing field that matches nothing, and the conditions and, or, not, parentheses,
 * "1 of selection_*", "all of them". Regular expressions, keyword lists and aggregations are not implemented, and the log source is not checked.
 */
final class Sigma {

    private static final ObjectMapper YAML = YAMLMapper.builder().build();

    private Sigma() {
    }

    /** Every rule in the folder, in file name order, by name. */
    static Map<String, Rule> load(Path dir) throws IOException {
        Map<String, Rule> rules = new LinkedHashMap<>();
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(p -> p.toString().endsWith(".yml")).sorted().toList();
        }
        for (Path file : files) {
            for (Map<String, Object> doc : documents(Files.readString(file))) {
                Rule r = new Rule(doc);
                rules.put(r.name(), r);
            }
        }
        return rules;
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> documents(String text) {
        List<Map<String, Object>> out = new ArrayList<>();
        try (MappingIterator<JsonNode> it = YAML.readerFor(JsonNode.class).<JsonNode>readValues(text)) {
            while (it.hasNext()) {
                out.add(YAML.convertValue(it.next(), Map.class));
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Object o) {
        return (Map<String, Object>) o;
    }

    static List<String> strings(Object o) {
        if (o == null) {
            return List.of();
        }
        if (o instanceof List<?> l) {
            return l.stream().map(String::valueOf).toList();
        }
        return List.of(String.valueOf(o));
    }

    static long seconds(String span) {
        long n = Long.parseLong(span.substring(0, span.length() - 1));
        return switch (span.charAt(span.length() - 1)) {
            case 's' -> n;
            case 'm' -> n * 60;
            case 'h' -> n * 3600;
            case 'd' -> n * 86400;
            default -> throw new IllegalArgumentException("timespan " + span);
        };
    }

    /** One field test: all values (with the all modifier) or any value must match. */
    private record Test(String field, boolean all, List<Pattern> patterns) {

        boolean test(Map<String, String> fields) {
            String v = fields.get(field);
            if (v == null) {
                return false;
            }
            String lower = v.toLowerCase(Locale.ROOT);
            return all ? patterns.stream().allMatch(p -> p.matcher(lower).matches()) : patterns.stream().anyMatch(p -> p.matcher(lower).matches());
        }
    }

    private static Test test(String key, Object value) {
        String[] parts = key.split("\\|");
        String op = "equals";
        boolean all = false;
        for (int i = 1; i < parts.length; i++) {
            switch (parts[i]) {
                case "contains", "startswith", "endswith" -> op = parts[i];
                case "all" -> all = true;
                default -> throw new IllegalArgumentException("modifier not implemented: " + parts[i]);
            }
        }
        List<Pattern> patterns = new ArrayList<>();
        for (String v : strings(value)) {
            StringBuilder re = new StringBuilder(op.equals("contains") || op.equals("endswith") ? ".*" : "");
            StringBuilder literal = new StringBuilder();
            for (char c : v.toLowerCase(Locale.ROOT).toCharArray()) {
                if (c == '*' || c == '?') {
                    if (!literal.isEmpty()) {
                        re.append(Pattern.quote(literal.toString()));
                        literal.setLength(0);
                    }
                    re.append(c == '*' ? ".*" : ".");
                } else {
                    literal.append(c);
                }
            }
            if (!literal.isEmpty()) {
                re.append(Pattern.quote(literal.toString()));
            }
            if (op.equals("contains") || op.equals("startswith")) {
                re.append(".*");
            }
            patterns.add(Pattern.compile(re.toString(), Pattern.DOTALL));
        }
        return new Test(parts[0], all, patterns);
    }

    /** A selection is a map of fields that all must match, or a list of such maps of which one must. */
    private static Predicate<Map<String, String>> selection(Object body) {
        List<Object> alternatives = body instanceof List<?> l ? new ArrayList<>(l) : List.of(body);
        List<List<Test>> any = new ArrayList<>();
        for (Object alt : alternatives) {
            if (!(alt instanceof Map<?, ?>)) {
                throw new IllegalArgumentException("keyword selections are not implemented");
            }
            List<Test> tests = new ArrayList<>();
            map(alt).forEach((k, v) -> tests.add(test(k, v)));
            any.add(tests);
        }
        return f -> any.stream().anyMatch(tests -> tests.stream().allMatch(t -> t.test(f)));
    }

    static Predicate<Map<String, String>> compile(Map<String, Object> detection) {
        Map<String, Predicate<Map<String, String>>> selections = new LinkedHashMap<>();
        detection.forEach((k, v) -> {
            if (!k.equals("condition") && !k.equals("timeframe")) {
                selections.put(k, selection(v));
            }
        });
        return new Condition(String.valueOf(detection.get("condition")), selections).parse();
    }

    /** A recursive descent parser: or, then and, then not, then a name, a group or "N of pattern". */
    private static final class Condition {
        private final List<String> tokens = new ArrayList<>();
        private final Map<String, Predicate<Map<String, String>>> selections;
        private int pos;

        Condition(String text, Map<String, Predicate<Map<String, String>>> selections) {
            this.selections = selections;
            java.util.regex.Matcher m = Pattern.compile("\\(|\\)|[^\\s()]+").matcher(text);
            while (m.find()) {
                tokens.add(m.group());
            }
        }

        Predicate<Map<String, String>> parse() {
            Predicate<Map<String, String>> p = or();
            if (pos != tokens.size()) {
                throw new IllegalArgumentException("unexpected " + tokens.get(pos));
            }
            return p;
        }

        private boolean peek(String word) {
            return pos < tokens.size() && tokens.get(pos).equals(word);
        }

        private Predicate<Map<String, String>> or() {
            Predicate<Map<String, String>> p = and();
            while (peek("or")) {
                pos++;
                p = p.or(and());
            }
            return p;
        }

        private Predicate<Map<String, String>> and() {
            Predicate<Map<String, String>> p = not();
            while (peek("and")) {
                pos++;
                p = p.and(not());
            }
            return p;
        }

        private Predicate<Map<String, String>> not() {
            if (peek("not")) {
                pos++;
                return not().negate();
            }
            return primary();
        }

        private Predicate<Map<String, String>> primary() {
            String t = tokens.get(pos++);
            if (t.equals("(")) {
                Predicate<Map<String, String>> p = or();
                pos++;
                return p;
            }
            if ((t.equals("1") || t.equals("all")) && peek("of")) {
                pos++;
                String pattern = tokens.get(pos++);
                Pattern glob = Pattern.compile(pattern.equals("them") ? ".*" : pattern.replace("*", ".*"));
                List<Predicate<Map<String, String>>> chosen = selections.entrySet().stream().filter(e -> glob.matcher(e.getKey()).matches()).map(Map.Entry::getValue).toList();
                if (chosen.isEmpty()) {
                    throw new IllegalArgumentException("no selection matches " + pattern);
                }
                boolean all = t.equals("all");
                return f -> all ? chosen.stream().allMatch(c -> c.test(f)) : chosen.stream().anyMatch(c -> c.test(f));
            }
            Predicate<Map<String, String>> p = selections.get(t);
            if (p == null) {
                throw new IllegalArgumentException("unknown selection " + t);
            }
            return p;
        }
    }
}
