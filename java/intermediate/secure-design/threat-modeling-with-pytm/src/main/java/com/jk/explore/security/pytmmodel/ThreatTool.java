package com.jk.explore.security.pytmmodel;

import java.util.List;

/** What the demo asks of pytm: its library, the findings for a model, the diagram and a report. The real pytm answers, or a recording of it. */
public interface ThreatTool {

    /** One threat of pytm's library: the kinds of element it applies to, its severity, its title and its condition. */
    record Threat(String id, List<String> targets, String severity, String title, String condition) {
    }

    /** One finding: the threat, the element or flow it is about, its severity and title. */
    record Finding(String threat, String target, String severity, String title) {
    }

    /** The threats of the library that are not deprecated. */
    List<Threat> library() throws Exception;

    /** The findings for a model (python --json). */
    List<Finding> findings(String name, String python) throws Exception;

    /** The graph text of the data-flow diagram (python --dfd). */
    String dfd(String name, String python) throws Exception;

    /** A report filled from a template of our own (python --report). */
    String report(String name, String python, String template) throws Exception;
}
