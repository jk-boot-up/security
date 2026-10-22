package com.jk.explore.security.failclosedspring;

/** What the bank answers. Only an approval that a check really gave is ever {@code approved}. */
record Decision(boolean approved, String reason) {
}
