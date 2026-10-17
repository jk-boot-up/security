package com.jk.explore.security.hashing;

/** Where salts come from: a salt is a few extra bytes, different for every account, mixed into the hash. */
public interface SaltSource {
    byte[] next();
}
