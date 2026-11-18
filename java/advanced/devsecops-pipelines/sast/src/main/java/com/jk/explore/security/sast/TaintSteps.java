package com.jk.explore.security.sast;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A teaching class: follows input through a few lines of code the way a taint rule does, by hand.
 * A variable assigned from the source, or from a tainted variable, is tainted; a sink called with a tainted value is a finding.
 * It knows only plain assignments inside one method, which is also where Semgrep's taint mode stops by default.
 */
final class TaintSteps {

    /** One step: the line number, the variable or call, and why it is tainted. */
    record Step(int line, String what, String why) {
    }

    private static final Pattern ASSIGN = Pattern.compile("^\\s*(?:[\\w<>\\[\\]]+\\s+)?(\\w+)\\s*=\\s*(.+);");

    private TaintSteps() {
    }

    /** {@code first} is the line number of the first line given; source and sink are method names, such as getParameter. */
    static List<Step> follow(List<String> code, int first, String source, String sink) {
        List<String> tainted = new ArrayList<>();
        List<Step> steps = new ArrayList<>();
        for (int i = 0; i < code.size(); i++) {
            String line = code.get(i);
            Matcher a = ASSIGN.matcher(line);
            String from = line.contains("." + source + "(") ? "the source" : tainted.stream().filter(v -> line.matches(".*\\b" + v + "\\b.*")).findFirst().orElse(null);
            if (a.find() && from != null) {
                tainted.add(a.group(1));
                steps.add(new Step(first + i, a.group(1), "tainted: from " + from));
            }
            Matcher s = Pattern.compile("\\." + sink + "\\(([^)]*)\\)").matcher(line);
            if (s.find() && tainted.stream().anyMatch(v -> s.group(1).matches(".*\\b" + v + "\\b.*"))) {
                steps.add(new Step(first + i, sink + "(" + s.group(1) + ")", "a tainted value reaches the sink"));
            }
        }
        return steps;
    }
}
