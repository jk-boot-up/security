package com.jk.explore.security.pytmmodel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.TreeSet;

/** Writes the Python file that pytm reads: the design as pytm objects, plus the answers of a stage. */
public final class Writer {

    private Writer() {
    }

    public static String python(Design design, Answers.Stage stage, String title) {
        List<String> out = new ArrayList<>();
        out.add("from pytm import TM, Actor, ExternalEntity, Process, Server, Datastore, Dataflow, Boundary");
        out.add("");
        out.add("tm = TM(\"" + title + "\")");
        out.add("tm.description = \"The online bank: a mobile app, an API gateway, a payments service, two stores and a card processor\"");
        out.add("tm.isOrdered = True");
        out.add("");
        for (String boundary : design.boundaries()) {
            out.add(boundary.toLowerCase() + " = Boundary(\"" + boundary + "\")");
        }
        out.add("");
        for (Design.Element e : design.elements()) {
            out.add(e.id() + " = " + e.kind().pytm + "(\"" + e.name() + "\")");
            out.add(e.id() + ".inBoundary = " + e.boundary().toLowerCase());
            if (e.kind() == Design.Kind.STORE) {
                out.add(e.id() + ".isSQL = " + (e.id().equals("audit") ? "False" : "True"));
                out.add(e.id() + ".storesSensitiveData = " + (e.id().equals("audit") ? "False" : "True"));
                out.add(e.id() + ".storesLogData = " + (e.id().equals("audit") ? "True" : "False"));
            }
            out.addAll(Answers.forElement(e, stage));
        }
        out.add("");
        for (Design.Flow f : design.flows()) {
            out.add(f.id() + " = Dataflow(" + f.from() + ", " + f.to() + ", \"" + f.label() + "\")");
            out.add(f.id() + ".protocol = \"" + f.protocol() + "\"");
            out.add(f.id() + ".dstPort = " + f.port());
            out.add(f.id() + ".data = \"" + f.data() + "\"");
            out.addAll(Answers.forFlow(f, stage));
        }
        out.add("");
        out.add("tm.process()");
        return String.join("\n", out) + "\n";
    }

    /** The answers a model sets to True for one element (its variable name), read back from the Python: what pytm's conditions see. */
    public static Set<String> trueAnswers(String python, String id) {
        Set<String> out = new TreeSet<>();
        Matcher m = Pattern.compile("^" + id + "\\.(?:controls\\.)?(\\w+) = True$", Pattern.MULTILINE).matcher(python);
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }
}
