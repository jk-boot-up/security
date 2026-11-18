package com.jk.explore.security.k8snetpol;

/**
 * One network policy: its name, its namespace, a one-line summary of what it does, and the manifest the API server gets.
 *
 * @param name the policy's name
 * @param namespace where it lives; it only selects pods of this namespace
 * @param summary what it lets in or out, in one line
 * @param manifest the YAML that is applied
 */
public record Policy(String name, String namespace, String summary, String manifest) {
}
