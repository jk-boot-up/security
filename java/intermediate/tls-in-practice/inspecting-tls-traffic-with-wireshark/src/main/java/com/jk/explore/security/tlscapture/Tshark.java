package com.jk.explore.security.tlscapture;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/** The tshark command reading a capture file, with or without the secrets of the connection. */
final class Tshark {

    private Tshark() {
    }

    /** The fields of every packet that matches {@code filter}, one list per packet. {@code options} are extra -o settings such as tls.keylog_file:path. */
    static List<List<String>> fields(Path capture, String filter, List<String> options, String... fields) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("tshark", "-r", capture.toString()));
        for (String option : options) {
            command.addAll(List.of("-o", option));
        }
        command.addAll(List.of("-Y", filter, "-T", "fields", "-E", "separator=\t"));
        for (String field : fields) {
            command.addAll(List.of("-e", field));
        }
        List<List<String>> rows = new ArrayList<>();
        for (String line : stdout(command).split("\n")) {
            if (!line.isBlank()) {
                rows.add(List.of(line.split("\t", -1)));
            }
        }
        return rows;
    }

    /** Only what tshark prints on its standard output; its warnings are not data. */
    private static String stdout(List<String> command) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        String out = new String(p.getInputStream().readAllBytes());
        if (p.waitFor() != 0) {
            throw new IllegalStateException(String.join(" ", command) + " failed");
        }
        return out;
    }

    static List<String> keyLog(Path keys) {
        return List.of("tls.keylog_file:" + keys);
    }

    /** The server's private key (an RSA key file with no password); tshark tries it on every TLS connection in the capture. */
    static List<String> serverKey(Path key) {
        return List.of("uat:rsa_keys:\"" + key + "\",\"\"");
    }

    /** tshark prints http.file_data as hex. */
    static String text(String hex) {
        return hex.isEmpty() ? "" : new String(HexFormat.of().parseHex(hex.replace(":", "")));
    }

    static int count(Path capture, String filter, List<String> options) throws IOException, InterruptedException {
        return fields(capture, filter, options, "frame.number").size();
    }
}
