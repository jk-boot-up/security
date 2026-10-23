package com.jk.explore.security.hsmkeys;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.ValueLayout;

/** The one libc call Java does not have: setenv. SoftHSM reads SOFTHSM2_CONF from the process environment when the library starts. */
final class Native {

    private Native() {
    }

    static void setenv(String name, String value) {
        Linker linker = Linker.nativeLinker();
        try (Arena arena = Arena.ofConfined()) {
            var handle = linker.downcallHandle(linker.defaultLookup().find("setenv").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            int rc = (int) handle.invokeExact(arena.allocateFrom(name), arena.allocateFrom(value), 1);
            if (rc != 0) {
                throw new IllegalStateException("setenv failed: " + rc);
            }
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }
}
