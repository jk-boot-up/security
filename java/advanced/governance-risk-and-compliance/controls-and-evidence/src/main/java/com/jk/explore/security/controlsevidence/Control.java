package com.jk.explore.security.controlsevidence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** A control of the bank: who owns it, which NIST CSF 2.0 subcategory and ISO/IEC 27001 Annex A controls it serves, and where its evidence is. */
public record Control(String id, String name, String owner, String csf, List<String> iso, String evidence, String kind) {

    /** The CSF function: the part of the subcategory before the dot (GV, ID, PR, DE, RS, RC). */
    public String function() {
        return csf.substring(0, csf.indexOf('.'));
    }

    public boolean automated() {
        return kind.equals("auto");
    }

    public static List<Control> load(Path csv) throws IOException {
        List<Control> controls = new ArrayList<>();
        List<String> lines = Files.readAllLines(csv);
        for (String line : lines.subList(1, lines.size())) {
            String[] f = line.split(",", -1);
            controls.add(new Control(f[0], f[1], f[2], f[3], List.of(f[4].split(";")), f[5], f[6]));
        }
        return controls;
    }

    /** The Annex A controls that the bank has chosen, one for each line; lines that start with # are comments. */
    public static List<String> applicable(Path file) throws IOException {
        return Files.readAllLines(file).stream().filter(l -> !l.isBlank() && !l.startsWith("#")).toList();
    }
}
