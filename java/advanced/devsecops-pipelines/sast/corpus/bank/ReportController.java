package bank;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.servlet.http.HttpServletRequest;

/** Statements as files, produced by an external converter. */
public class ReportController {

    private static final File DIR = new File("/var/reports");

    void convert(HttpServletRequest request) throws IOException {
        String file = request.getParameter("file");
        Runtime.getRuntime().exec("convert " + file + " out.pdf"); // VULN command
    }

    void convertSafely(HttpServletRequest request) throws IOException {
        String file = Validate.fileName(request.getParameter("file"));
        Runtime.getRuntime().exec(new String[] {"convert", file, "out.pdf"}); // SAFE command-array
    }

    byte[] download(HttpServletRequest request) throws IOException {
        File f = new File(DIR, request.getParameter("name")); // VULN path
        return Files.readAllBytes(f.toPath());
    }

    byte[] downloadSafely(HttpServletRequest request) throws IOException {
        File f = new File(DIR, Validate.fileName(request.getParameter("name"))); // SAFE path-validated
        return Files.readAllBytes(f.toPath());
    }
}
