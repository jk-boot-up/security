package com.jk.explore.security.tcpipdump;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Independent checks: the real tcpdump and an independent Python script read the files this project writes. Run with ./gradlew integrationTest. */
@Tag("tools")
class ToolsInteropTest {

    @TempDir
    Path dir;

    private static String run(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        assertTrue(process.waitFor(60, TimeUnit.SECONDS));
        assertEquals(0, process.exitValue(), output);
        return output;
    }

    private Path capture() throws IOException {
        Path file = dir.resolve("capture.pcap");
        Files.write(file, PcapFile.write(Traffic.everything()));
        return file;
    }

    @Test
    void tcpdumpPrintsTheSameLinesAsTheJavaFormatter() throws Exception {
        Path file = capture();
        String output = run("tcpdump", "-nn", "-t", "-S", "-r", file.toString());
        List<String> fromTcpdump = output.lines().filter(l -> l.startsWith("IP ")).toList();
        assertEquals(Capture.lines(Files.readAllBytes(file)), fromTcpdump);
    }

    @Test
    void tcpdumpReportsTheBadChecksumOnlyWithVerbose() throws Exception {
        Path file = capture();
        String plain = run("tcpdump", "-nn", "-t", "-S", "-r", file.toString());
        String verbose = run("tcpdump", "-nn", "-t", "-S", "-v", "-r", file.toString());
        assertEquals(0, plain.split("incorrect", -1).length - 1);
        assertEquals(1, verbose.split("incorrect", -1).length - 1);
    }

    @Test
    void pythonScriptFindsTheSameUnfinishedHandshakes() throws Exception {
        Path file = capture();
        String output = run("python3", "-I", "python/unfinished_handshakes.py", file.toString());
        List<String> fromPython = Arrays.asList(output.strip().split("\n"));
        assertEquals(List.copyOf(Capture.unfinishedHandshakes(Files.readAllBytes(file))).stream()
                .sorted(java.util.Comparator.comparingLong(a -> Address.parse(a) & 0xffffffffL)).toList(), fromPython);
    }
}
