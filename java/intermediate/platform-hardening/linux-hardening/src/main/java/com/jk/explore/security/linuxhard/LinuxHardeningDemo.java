package com.jk.explore.security.linuxhard;

import java.io.IOException;
import java.util.List;

/** Runs the host-checking story in four numbered acts and prints exact, repeatable results. The file tree is a temporary directory whose name is never printed. */
public class LinuxHardeningDemo {

    public static void main(String[] args) throws IOException {
        System.out.print(run());
    }

    public static String run() throws IOException {
        StringBuilder out = new StringBuilder();
        try (Fixture host = Fixture.insecure()) {
            Scanner scanner = new Scanner(host.root(), Policy.standard());

            out.append("== 1. What the checker finds on a typical image ==\n");
            List<Finding> first = scanner.scan();
            first.forEach(f -> out.append(f).append('\n'));
            out.append(first.size()).append(" findings: ").append(count(first, Severity.CRITICAL)).append(" critical, ").append(count(first, Severity.HIGH)).append(" high, ")
                    .append(count(first, Severity.MEDIUM)).append(" medium, ").append(count(first, Severity.LOW)).append(" low; ")
                    .append(first.stream().filter(Finding::fixable).count()).append(" can be fixed mechanically\n");

            out.append("\n== 2. The fixes a machine can make ==\n");
            int applied = scanner.applyPermissionFixes(first);
            List<Finding> second = scanner.scan();
            out.append("permission changes applied: ").append(applied).append('\n');
            out.append("findings left: ").append(second.size()).append(" (none of them a permission)\n");
            second.forEach(f -> out.append("  ").append(f.rule()).append(" - ").append(f.subject()).append('\n'));

            out.append("\n== 3. The decisions a person makes ==\n");
            host.hardenByHand();
            List<Finding> third = scanner.scan();
            out.append("after removing the second root account and the test account, switching off passwords and root login for ssh,\n");
            out.append("setting the kernel options and closing the extra ports -> ").append(third.size()).append(" findings\n");
            third.forEach(f -> out.append("  ").append(f).append('\n'));

            out.append("\n== 4. Running it again ==\n");
            out.append("a second scan of the hardened tree -> ").append(scanner.scan().size()).append(" findings; the same scan is part of the build, so a regression fails it\n");
        }
        return out.toString();
    }

    private static long count(List<Finding> f, Severity s) {
        return f.stream().filter(x -> x.severity() == s).count();
    }
}
