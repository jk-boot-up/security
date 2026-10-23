package bank;

/** Input checks shared by the controllers. */
final class Validate {

    private Validate() {
    }

    static String fileName(String name) {
        if (!name.matches("[A-Za-z0-9._-]+") || name.contains("..")) {
            throw new IllegalArgumentException("bad file name");
        }
        return name;
    }
}
