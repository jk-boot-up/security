package com.jk.explore.security.pytmmodel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A teaching class: redoes by hand what pytm does with a threat's condition. pytm evaluates the condition as Python with the element as {@code target}; most
 * conditions only ask whether an answer is True or False, joined by and, or, not and brackets. Every answer that was never given is False.
 * A test checks that this gives pytm's exact findings for every such threat, element and stage.
 */
public final class Condition {

    private static final Pattern TERM = Pattern.compile("target\\.(?:controls\\.)?(\\w+) is (True|False)");
    private static final Pattern TOKEN = Pattern.compile("\\s*(target\\.(?:controls\\.)?\\w+ is (?:True|False)|\\(|\\)|and\\b|or\\b|not\\b)");

    /** One test inside a condition: an answer's name and the value that makes the test true. */
    public record Term(String answer, boolean value) {
        public boolean holds(Set<String> trueAnswers) {
            return trueAnswers.contains(answer) == value;
        }
    }

    private final List<String> tokens;
    private int at;

    private Condition(List<String> tokens) {
        this.tokens = tokens;
    }

    /** Whether the condition only asks about answers being True or False. */
    public static boolean simple(String condition) {
        return tokenize(condition) != null;
    }

    /** The tests inside a condition, left to right. */
    public static List<Term> terms(String condition) {
        List<Term> out = new ArrayList<>();
        Matcher m = TERM.matcher(condition);
        while (m.find()) {
            out.add(new Term(m.group(1), m.group(2).equals("True")));
        }
        return out;
    }

    /** The condition, evaluated for an element whose true answers are given: true means pytm reports the threat. */
    public static boolean holds(String condition, Set<String> trueAnswers) {
        List<String> tokens = tokenize(condition);
        if (tokens == null) {
            throw new IllegalArgumentException("not a simple condition: " + condition);
        }
        Condition c = new Condition(tokens);
        boolean result = c.or(trueAnswers);
        if (c.at != tokens.size()) {
            throw new IllegalArgumentException("unexpected " + tokens.get(c.at) + " in " + condition);
        }
        return result;
    }

    private static List<String> tokenize(String condition) {
        List<String> out = new ArrayList<>();
        Matcher m = TOKEN.matcher(condition);
        int end = 0;
        while (m.lookingAt()) {
            out.add(m.group(1));
            end = m.end();
            m.region(end, condition.length());
        }
        return condition.substring(end).isBlank() ? out : null;
    }

    // Python's order: not binds tightest, then and, then or.
    private boolean or(Set<String> answers) {
        boolean value = and(answers);
        while (at < tokens.size() && tokens.get(at).equals("or")) {
            at++;
            value = and(answers) | value;
        }
        return value;
    }

    private boolean and(Set<String> answers) {
        boolean value = not(answers);
        while (at < tokens.size() && tokens.get(at).equals("and")) {
            at++;
            value = not(answers) & value;
        }
        return value;
    }

    private boolean not(Set<String> answers) {
        if (tokens.get(at).equals("not")) {
            at++;
            return !not(answers);
        }
        if (tokens.get(at).equals("(")) {
            at++;
            boolean value = or(answers);
            at++; // ")"
            return value;
        }
        Matcher m = TERM.matcher(tokens.get(at++));
        m.matches();
        return new Term(m.group(1), m.group(2).equals("True")).holds(answers);
    }
}
