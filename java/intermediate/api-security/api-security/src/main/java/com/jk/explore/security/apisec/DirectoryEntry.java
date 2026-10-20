package com.jk.explore.security.apisec;

/** What the branch directory shows of any account: an id and a display name, nothing about money or cards. */
public record DirectoryEntry(String id, String displayName) {
}
