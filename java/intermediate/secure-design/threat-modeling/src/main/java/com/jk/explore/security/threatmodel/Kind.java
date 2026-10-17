package com.jk.explore.security.threatmodel;

/** The three kinds of element in a data-flow diagram. Flows are the fourth kind of thing in the model, kept in {@link Flow}. */
enum Kind {
    EXTERNAL_ENTITY("external entity"),
    PROCESS("process"),
    DATA_STORE("data store");

    final String label;

    Kind(String label) {
        this.label = label;
    }
}
