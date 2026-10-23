package com.jk.explore.security.runtimesec;

import java.util.List;

/**
 * One audited syscall of a process in a container: what ran (key exec), what was opened for writing (key write) or what was connected to (key connect).
 *
 * @param id the audit event number
 * @param key exec, write or connect
 * @param comm the process name as the kernel knows it
 * @param success whether the syscall worked
 * @param containerId the 64 characters of the container the process runs in, or null
 * @param paths the files named by the syscall, relative names resolved against the working directory
 * @param address ip:port of a connect to an IPv4 address, else null
 */
public record Event(String id, String key, String comm, boolean success, String containerId, List<String> paths, String address) {

    /** The address without the port. */
    public String ip() {
        return address == null ? null : address.substring(0, address.lastIndexOf(':'));
    }
}
