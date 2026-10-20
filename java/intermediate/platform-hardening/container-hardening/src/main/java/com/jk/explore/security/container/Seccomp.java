package com.jk.explore.security.container;

/** Which system calls the container may make. */
public enum Seccomp {
    /** No filter: every system call the kernel has. */
    UNCONFINED,
    /** The runtime's default profile: blocks mount, ptrace on others, keyctl, kernel module loading and a few dozen more. */
    RUNTIME_DEFAULT
}
