package bank;

/** Starts the client: with a valid key it shows a statement line, otherwise it refuses. */
public final class Main {

    private Main() {
    }

    public static String run(String key) {
        if (Licence.check(key)) {
            return "licensed: " + Statement.line("Corner Bakery", 1250);
        }
        return "denied";
    }

    public static void main(String[] args) {
        System.out.println(run(args.length > 0 ? args[0] : null));
    }
}
