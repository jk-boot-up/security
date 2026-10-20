package com.jk.explore.security.xmlinj;

/** An invoice uploaded by a supplier as XML: who, how much, and a free-text note. */
public record Invoice(String customer, String total, String note) {
}
