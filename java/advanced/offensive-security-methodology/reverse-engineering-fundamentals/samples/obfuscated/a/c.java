package a;

final class c {

    private c() {
    }

    static String d(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) (Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16) ^ 0x5A);
        }
        return new String(out, java.nio.charset.StandardCharsets.UTF_8);
    }
}
