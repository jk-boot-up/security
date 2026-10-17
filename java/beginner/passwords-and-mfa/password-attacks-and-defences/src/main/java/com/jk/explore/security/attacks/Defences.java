package com.jk.explore.security.attacks;

import java.util.Set;

/**
 * Which defences the bank's login has switched on. A value of zero means that defence is off.
 *
 * @param lockoutAfter failures that lock an account (0 = never lock)
 * @param lockoutTicks how long a lock lasts
 * @param lockScope whether failures are counted per account, or per account and address
 * @param perAddressLimit attempts one address may make in a window (0 = unlimited)
 * @param windowTicks the length of that window
 * @param genericMessages whether wrong-user and wrong-password get the same message
 * @param breached passwords known from breaches; a right password found here must be changed (empty = no check)
 */
public record Defences(int lockoutAfter, int lockoutTicks, LockScope lockScope, int perAddressLimit, int windowTicks,
                       boolean genericMessages, Set<String> breached) {

    public static Defences none() {
        return new Defences(0, 0, LockScope.ACCOUNT, 0, 1, false, Set.of());
    }

    public Defences withLockout(int after, int ticks, LockScope scope) {
        return new Defences(after, ticks, scope, perAddressLimit, windowTicks, genericMessages, breached);
    }

    public Defences withRateLimit(int perWindow, int window) {
        return new Defences(lockoutAfter, lockoutTicks, lockScope, perWindow, window, genericMessages, breached);
    }

    public Defences withGenericMessages() {
        return new Defences(lockoutAfter, lockoutTicks, lockScope, perAddressLimit, windowTicks, true, breached);
    }

    public Defences withBreachedCheck(Set<String> known) {
        return new Defences(lockoutAfter, lockoutTicks, lockScope, perAddressLimit, windowTicks, genericMessages, known);
    }
}
