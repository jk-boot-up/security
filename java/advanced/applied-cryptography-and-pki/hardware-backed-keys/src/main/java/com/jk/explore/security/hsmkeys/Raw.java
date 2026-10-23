package com.jk.explore.security.hsmkeys;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The PKCS#11 calls the JDK provider does not offer (reading attributes, wrapping a key, unwrapping it again), made straight on the library
 * through the foreign function API. The library is the one SunPKCS11 already loaded, so the login and the objects are shared.
 */
final class Raw implements AutoCloseable {

    static final long CKA_CLASS = 0x0, CKA_TOKEN = 0x1, CKA_PRIVATE = 0x2, CKA_LABEL = 0x3, CKA_VALUE = 0x11, CKA_KEY_TYPE = 0x100,
            CKA_SENSITIVE = 0x103, CKA_WRAP = 0x106, CKA_UNWRAP = 0x107, CKA_SIGN = 0x108, CKA_VERIFY = 0x10A, CKA_EC_PARAMS = 0x180,
            CKA_EC_POINT = 0x181, CKA_VALUE_LEN = 0x161, CKA_EXTRACTABLE = 0x162, CKA_NEVER_EXTRACTABLE = 0x164,
            CKA_ALWAYS_SENSITIVE = 0x165, CKA_PRIVATE_EXPONENT = 0x123,
            CKA_ID = 0x102, CKA_MODULUS = 0x120, CKA_MODULUS_BITS = 0x121, CKA_PUBLIC_EXPONENT = 0x122, CKA_SUBJECT = 0x101, CKA_CERTIFICATE_TYPE = 0x80;
    static final long CKO_CERTIFICATE = 1, CKO_PUBLIC_KEY = 2, CKO_PRIVATE_KEY = 3, CKO_SECRET_KEY = 4;
    static final long CKK_RSA = 0, CKK_EC = 3, CKK_AES = 0x1F;
    static final long CKM_RSA_PKCS_KEY_PAIR_GEN = 0x0, CKM_SHA256_RSA_PKCS = 0x40, CKM_EC_KEY_PAIR_GEN = 0x1040, CKM_ECDSA = 0x1041, CKM_AES_KEY_GEN = 0x1080, CKM_AES_KEY_WRAP_PAD = 0x210A;
    static final byte[] P256 = {0x06, 0x08, 0x2A, (byte) 0x86, 0x48, (byte) 0xCE, 0x3D, 0x03, 0x01, 0x07};

    private static final Map<Long, String> RV = Map.ofEntries(Map.entry(0x0L, "CKR_OK"), Map.entry(0x11L, "CKR_ATTRIBUTE_SENSITIVE"),
            Map.entry(0x63L, "CKR_KEY_NOT_WRAPPABLE"), Map.entry(0x6AL, "CKR_KEY_UNEXTRACTABLE"), Map.entry(0xA0L, "CKR_PIN_INCORRECT"),
            Map.entry(0xA4L, "CKR_PIN_LOCKED"), Map.entry(0x101L, "CKR_USER_NOT_LOGGED_IN"), Map.entry(0x191L, "CKR_CRYPTOKI_ALREADY_INITIALIZED"),
            Map.entry(0x70L, "CKR_MECHANISM_INVALID"), Map.entry(0x82L, "CKR_OBJECT_HANDLE_INVALID"),
            Map.entry(0xF0L, "CKR_UNWRAPPING_KEY_HANDLE_INVALID"), Map.entry(0x100L, "CKR_USER_ALREADY_LOGGED_IN"));

    /** The answer of a call: the return value and any bytes. */
    record Answer(long rv, byte[] bytes) {

        boolean ok() {
            return rv == 0;
        }

        String name() {
            return RV.getOrDefault(rv, "CKR_0x" + Long.toHexString(rv).toUpperCase());
        }
    }

    private final Arena library = Arena.ofShared();
    private final Linker linker = Linker.nativeLinker();
    private final MemorySegment functions;

    Raw(Path libraryPath) {
        try {
            SymbolLookup lookup = SymbolLookup.libraryLookup(libraryPath, library);
            MethodHandle list = linker.downcallHandle(lookup.find("C_GetFunctionList").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
            try (Arena a = Arena.ofConfined()) {
                MemorySegment out = a.allocate(8);
                long rv = (long) list.invokeExact(out);
                if (rv != 0) {
                    throw new IllegalStateException("C_GetFunctionList " + rv);
                }
                functions = out.get(ValueLayout.ADDRESS, 0).reinterpret(8 + 8 * 70);
            }
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
        long rv = call(0, MemorySegment.NULL);
        if (rv != 0 && rv != 0x191) {
            throw new IllegalStateException("C_Initialize " + rv);
        }
    }

    // index of each function in CK_FUNCTION_LIST (the version field takes the first 8 bytes)
    private static final int GET_SLOT_LIST = 4, GET_TOKEN_INFO = 6, OPEN_SESSION = 12, CLOSE_SESSION = 13, LOGIN = 18, LOGOUT = 19, DESTROY = 22,
            GET_ATTRIBUTE = 24, FIND_INIT = 26, FIND = 27, FIND_FINAL = 28, SIGN_INIT = 42, SIGN = 43, GENERATE_KEY = 58, GENERATE_PAIR = 59, CREATE_OBJECT = 20,
            WRAP = 60, UNWRAP = 61;

    private long call(int index, Object... args) {
        MemoryLayout[] layouts = new MemoryLayout[args.length];
        for (int i = 0; i < args.length; i++) {
            layouts[i] = switch (args[i]) {
                case Long l -> ValueLayout.JAVA_LONG;
                case Byte b -> ValueLayout.JAVA_BYTE;
                default -> ValueLayout.ADDRESS;
            };
        }
        try {
            MemorySegment fn = functions.get(ValueLayout.ADDRESS, 8L + 8L * index);
            MethodHandle h = linker.downcallHandle(fn, FunctionDescriptor.of(ValueLayout.JAVA_LONG, layouts));
            return (long) h.invokeWithArguments(args);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /** One attribute of a template: a type and a value. */
    record Attr(long type, byte[] value) {

        static Attr bool(long type, boolean v) {
            return new Attr(type, new byte[] {(byte) (v ? 1 : 0)});
        }

        static Attr ulong(long type, long v) {
            return new Attr(type, java.nio.ByteBuffer.allocate(8).order(java.nio.ByteOrder.nativeOrder()).putLong(v).array());
        }

        static Attr text(long type, String v) {
            return new Attr(type, v.getBytes(StandardCharsets.UTF_8));
        }
    }

    private MemorySegment template(Arena a, List<Attr> attrs) {
        MemorySegment t = a.allocate(24L * Math.max(1, attrs.size()));
        for (int i = 0; i < attrs.size(); i++) {
            Attr at = attrs.get(i);
            MemorySegment v = a.allocate(Math.max(1, at.value().length));
            v.copyFrom(MemorySegment.ofArray(at.value()));
            t.set(ValueLayout.JAVA_LONG, 24L * i, at.type());
            t.set(ValueLayout.ADDRESS, 24L * i + 8, v);
            t.set(ValueLayout.JAVA_LONG, 24L * i + 16, at.value().length);
        }
        return t;
    }

    private MemorySegment mechanism(Arena a, long mech) {
        MemorySegment m = a.allocate(24);
        m.set(ValueLayout.JAVA_LONG, 0, mech);
        return m;
    }

    /** The slot whose token carries the label, or -1. */
    long slotWithLabel(String label) {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment count = a.allocate(8);
            call(GET_SLOT_LIST, (byte) 1, MemorySegment.NULL, count);
            int n = (int) count.get(ValueLayout.JAVA_LONG, 0);
            MemorySegment slots = a.allocate(8L * n);
            call(GET_SLOT_LIST, (byte) 1, slots, count);
            for (int i = 0; i < n; i++) {
                long slot = slots.get(ValueLayout.JAVA_LONG, 8L * i);
                MemorySegment info = a.allocate(256);
                if (call(GET_TOKEN_INFO, slot, info) == 0
                        && new String(info.asSlice(0, 32).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_8).trim().equals(label)) {
                    return slot;
                }
            }
        }
        return -1;
    }

    long openSession(long slot) {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment out = a.allocate(8);
            long rv = call(OPEN_SESSION, slot, 6L, MemorySegment.NULL, MemorySegment.NULL, out);
            if (rv != 0) {
                throw new IllegalStateException("C_OpenSession " + rv);
            }
            return out.get(ValueLayout.JAVA_LONG, 0);
        }
    }

    void closeSession(long session) {
        call(CLOSE_SESSION, session);
    }

    Answer login(long session, String pin) {
        try (Arena a = Arena.ofConfined()) {
            byte[] p = pin.getBytes(StandardCharsets.UTF_8);
            MemorySegment seg = a.allocate(p.length);
            seg.copyFrom(MemorySegment.ofArray(p));
            return new Answer(call(LOGIN, session, 1L, seg, (long) p.length), new byte[0]);
        }
    }

    void logout(long session) {
        call(LOGOUT, session);
    }

    /** The object handles that match the attributes. */
    List<Long> find(long session, List<Attr> attrs) {
        List<Long> found = new ArrayList<>();
        try (Arena a = Arena.ofConfined()) {
            if (call(FIND_INIT, session, template(a, attrs), (long) attrs.size()) != 0) {
                return found;
            }
            MemorySegment handles = a.allocate(8L * 16);
            MemorySegment count = a.allocate(8);
            call(FIND, session, handles, 16L, count);
            for (int i = 0; i < count.get(ValueLayout.JAVA_LONG, 0); i++) {
                found.add(handles.get(ValueLayout.JAVA_LONG, 8L * i));
            }
            call(FIND_FINAL, session);
        }
        return found;
    }

    /** Asks the token for one attribute value; a sensitive one is refused. */
    Answer attribute(long session, long object, long type) {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment t = a.allocate(24);
            MemorySegment buf = a.allocate(1024);
            t.set(ValueLayout.JAVA_LONG, 0, type);
            t.set(ValueLayout.ADDRESS, 8, buf);
            t.set(ValueLayout.JAVA_LONG, 16, 1024);
            long rv = call(GET_ATTRIBUTE, session, object, t, 1L);
            long len = t.get(ValueLayout.JAVA_LONG, 16);
            return new Answer(rv, rv == 0 ? buf.asSlice(0, len).toArray(ValueLayout.JAVA_BYTE) : new byte[0]);
        }
    }

    boolean flag(long session, long object, long type) {
        Answer a = attribute(session, object, type);
        return a.ok() && a.bytes().length > 0 && a.bytes()[0] != 0;
    }

    /** An AES-256 key on the token. A token key that is sensitive and not extractable is the bank's wrapping key; a session key that is neither is an attacker's. */
    long generateAes(long session, String label, boolean onToken, boolean extractable) {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment out = a.allocate(8);
            List<Attr> attrs = List.of(Attr.bool(CKA_TOKEN, onToken), Attr.bool(CKA_PRIVATE, true), Attr.bool(CKA_SENSITIVE, !extractable),
                    Attr.bool(CKA_EXTRACTABLE, extractable), Attr.bool(CKA_WRAP, true), Attr.bool(CKA_UNWRAP, true),
                    Attr.ulong(CKA_VALUE_LEN, 32), Attr.text(CKA_LABEL, label));
            long rv = call(GENERATE_KEY, session, mechanism(a, CKM_AES_KEY_GEN), template(a, attrs), (long) attrs.size(), out);
            if (rv != 0) {
                throw new IllegalStateException("C_GenerateKey " + rv);
            }
            return out.get(ValueLayout.JAVA_LONG, 0);
        }
    }

    /** Generates a P-256 pair on the token; the private half is sensitive and, as asked, extractable (wrappable) or not. Returns {public, private}. */
    long[] generateEcPair(long session, String label, boolean extractable) {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment pub = a.allocate(8);
            MemorySegment priv = a.allocate(8);
            List<Attr> pubT = List.of(Attr.bool(CKA_TOKEN, true), Attr.bool(CKA_VERIFY, true), new Attr(CKA_EC_PARAMS, P256), Attr.text(CKA_LABEL, label));
            List<Attr> privT = List.of(Attr.bool(CKA_TOKEN, true), Attr.bool(CKA_PRIVATE, true), Attr.bool(CKA_SENSITIVE, true),
                    Attr.bool(CKA_EXTRACTABLE, extractable), Attr.bool(CKA_SIGN, true), Attr.text(CKA_LABEL, label));
            long rv = call(GENERATE_PAIR, session, mechanism(a, CKM_EC_KEY_PAIR_GEN), template(a, pubT), (long) pubT.size(), template(a, privT),
                    (long) privT.size(), pub, priv);
            if (rv != 0) {
                throw new IllegalStateException("C_GenerateKeyPair " + rv);
            }
            return new long[] {pub.get(ValueLayout.JAVA_LONG, 0), priv.get(ValueLayout.JAVA_LONG, 0)};
        }
    }

    /** Generates an RSA 2048 pair on the token. The private half is born sensitive and not extractable, so it can never be wrapped or read. Returns {public, private}. */
    long[] generateRsaPair(long session, String label, byte[] id) {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment pub = a.allocate(8);
            MemorySegment priv = a.allocate(8);
            List<Attr> pubT = List.of(Attr.bool(CKA_TOKEN, true), Attr.bool(CKA_VERIFY, true), Attr.ulong(CKA_MODULUS_BITS, 2048),
                    new Attr(CKA_PUBLIC_EXPONENT, new byte[] {1, 0, 1}), new Attr(CKA_ID, id), Attr.text(CKA_LABEL, label));
            List<Attr> privT = List.of(Attr.bool(CKA_TOKEN, true), Attr.bool(CKA_PRIVATE, true), Attr.bool(CKA_SENSITIVE, true),
                    Attr.bool(CKA_EXTRACTABLE, false), Attr.bool(CKA_SIGN, true), new Attr(CKA_ID, id), Attr.text(CKA_LABEL, label));
            long rv = call(GENERATE_PAIR, session, mechanism(a, CKM_RSA_PKCS_KEY_PAIR_GEN), template(a, pubT), (long) pubT.size(), template(a, privT),
                    (long) privT.size(), pub, priv);
            if (rv != 0) {
                throw new IllegalStateException("C_GenerateKeyPair " + rv);
            }
            return new long[] {pub.get(ValueLayout.JAVA_LONG, 0), priv.get(ValueLayout.JAVA_LONG, 0)};
        }
    }

    /** Stores an X.509 certificate on the token next to the key (same id and label), which is what makes the KeyStore list the key. */
    void createCertificate(long session, byte[] der, byte[] subjectDer, byte[] id, String label) {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment out = a.allocate(8);
            List<Attr> t = List.of(Attr.ulong(CKA_CLASS, CKO_CERTIFICATE), Attr.ulong(CKA_CERTIFICATE_TYPE, 0), Attr.bool(CKA_TOKEN, true),
                    new Attr(CKA_VALUE, der), new Attr(CKA_SUBJECT, subjectDer), new Attr(CKA_ID, id), Attr.text(CKA_LABEL, label));
            long rv = call(CREATE_OBJECT, session, template(a, t), (long) t.size(), out);
            if (rv != 0) {
                throw new IllegalStateException("C_CreateObject " + rv);
            }
        }
    }

    /** Signs with the given mechanism (for example CKM_SHA256_RSA_PKCS: the token hashes, pads and signs). */
    Answer sign(long session, long mech, long key, byte[] data) {
        try (Arena a = Arena.ofConfined()) {
            long rv = call(SIGN_INIT, session, mechanism(a, mech), key);
            if (rv != 0) {
                return new Answer(rv, new byte[0]);
            }
            MemorySegment d = a.allocate(Math.max(1, data.length));
            d.copyFrom(MemorySegment.ofArray(data));
            MemorySegment sig = a.allocate(512);
            MemorySegment len = a.allocate(8);
            len.set(ValueLayout.JAVA_LONG, 0, 512);
            rv = call(SIGN, session, d, (long) data.length, sig, len);
            return new Answer(rv, rv == 0 ? sig.asSlice(0, len.get(ValueLayout.JAVA_LONG, 0)).toArray(ValueLayout.JAVA_BYTE) : new byte[0]);
        }
    }

    /** C_WrapKey: the token encrypts the key under the wrapping key, or refuses. */
    Answer wrap(long session, long mech, long wrapKey, long key) {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment len = a.allocate(8);
            MemorySegment m = mechanism(a, mech);
            long rv = call(WRAP, session, m, wrapKey, key, MemorySegment.NULL, len);
            if (rv != 0) {
                return new Answer(rv, new byte[0]);
            }
            MemorySegment buf = a.allocate(len.get(ValueLayout.JAVA_LONG, 0));
            rv = call(WRAP, session, m, wrapKey, key, buf, len);
            return new Answer(rv, rv == 0 ? buf.asSlice(0, len.get(ValueLayout.JAVA_LONG, 0)).toArray(ValueLayout.JAVA_BYTE) : new byte[0]);
        }
    }

    /** C_UnwrapKey into a new, non-extractable P-256 private key on the token. Returns the new handle, or 0 with the reason. */
    Answer unwrapEc(long session, long mech, long unwrapKey, byte[] blob, String label) {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment out = a.allocate(8);
            MemorySegment w = a.allocate(blob.length);
            w.copyFrom(MemorySegment.ofArray(blob));
            List<Attr> t = List.of(Attr.ulong(CKA_CLASS, CKO_PRIVATE_KEY), Attr.ulong(CKA_KEY_TYPE, CKK_EC), Attr.bool(CKA_TOKEN, true),
                    Attr.bool(CKA_PRIVATE, true), Attr.bool(CKA_SENSITIVE, true), Attr.bool(CKA_EXTRACTABLE, false), Attr.bool(CKA_SIGN, true),
                    Attr.text(CKA_LABEL, label));
            long rv = call(UNWRAP, session, mechanism(a, mech), unwrapKey, w, (long) blob.length, template(a, t), (long) t.size(), out);
            return new Answer(rv, java.nio.ByteBuffer.allocate(8).order(java.nio.ByteOrder.nativeOrder()).putLong(out.get(ValueLayout.JAVA_LONG, 0)).array());
        }
    }

    static long handle(Answer a) {
        return java.nio.ByteBuffer.wrap(a.bytes()).order(java.nio.ByteOrder.nativeOrder()).getLong();
    }

    /** ECDSA over a SHA-256 digest done by the token (r and s, 64 bytes). */
    Answer signEcdsa(long session, long key, byte[] digest) {
        return sign(session, CKM_ECDSA, key, digest);
    }

    void destroy(long session, long object) {
        call(DESTROY, session, object);
    }

    @Override
    public void close() {
        // the library stays loaded and initialised: the provider that opened it first still uses it
    }
}
