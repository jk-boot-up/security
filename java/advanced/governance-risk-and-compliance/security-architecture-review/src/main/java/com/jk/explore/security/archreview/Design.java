package com.jk.explore.security.archreview;

import com.structurizr.Workspace;
import com.structurizr.dsl.StructurizrDslParser;
import com.structurizr.dsl.StructurizrDslParserException;
import com.structurizr.model.Container;
import com.structurizr.model.Element;
import com.structurizr.model.Person;
import com.structurizr.model.Relationship;
import com.structurizr.model.SoftwareSystem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** A design read from a Structurizr DSL file: the parts with their zone and properties, and the flows between them. */
public final class Design {

    /** Kinds of part, which decide the STRIDE questions asked about them. */
    public enum Kind {
        PERSON, EXTERNAL, PROCESS, DATASTORE
    }

    /** A part of the design. */
    public record Part(String id, String name, Kind kind, String zone, Map<String, String> props) {

        public String prop(String key) {
            return props.getOrDefault(key, "");
        }
    }

    /** A flow from one part to another. */
    public record Flow(Part from, Part to, String technology, Map<String, String> props) {

        public String id() {
            return from.id() + " -> " + to.id();
        }

        public boolean crossesZones() {
            return !from.zone().equals(to.zone());
        }

        public String prop(String key) {
            return props.getOrDefault(key, "");
        }
    }

    private final List<Part> parts;
    private final List<Flow> flows;

    private Design(List<Part> parts, List<Flow> flows) {
        this.parts = parts;
        this.flows = flows;
    }

    public List<Part> parts() {
        return parts;
    }

    public List<Flow> flows() {
        return flows;
    }

    public static Design load(Path dsl) throws IOException {
        try {
            StructurizrDslParser parser = new StructurizrDslParser();
            parser.parse(Files.readString(dsl));
            Workspace workspace = parser.getWorkspace();
            Map<String, Part> byName = new java.util.LinkedHashMap<>();
            for (Element e : workspace.getModel().getElements()) {
                Kind kind;
                if (e instanceof Person) {
                    kind = Kind.PERSON;
                } else if (e instanceof SoftwareSystem s && !s.getContainers().isEmpty()) {
                    continue;
                } else if (e instanceof SoftwareSystem) {
                    kind = Kind.EXTERNAL;
                } else if (e instanceof Container c) {
                    kind = c.getTags().contains("Datastore") ? Kind.DATASTORE : Kind.PROCESS;
                } else {
                    continue;
                }
                Map<String, String> props = e.getProperties();
                byName.put(e.getId(), new Part(props.getOrDefault("id", e.getName()), e.getName(), kind, props.getOrDefault("zone", ""), props));
            }
            List<Flow> flows = new java.util.ArrayList<>();
            for (Relationship r : workspace.getModel().getRelationships()) {
                Part from = byName.get(r.getSourceId());
                Part to = byName.get(r.getDestinationId());
                if (from != null && to != null) {
                    flows.add(new Flow(from, to, r.getTechnology(), r.getProperties()));
                }
            }
            flows.sort(Comparator.comparing(Flow::id));
            return new Design(List.copyOf(byName.values()), List.copyOf(flows));
        } catch (StructurizrDslParserException e) {
            throw new IOException(e.getMessage(), e);
        }
    }
}
