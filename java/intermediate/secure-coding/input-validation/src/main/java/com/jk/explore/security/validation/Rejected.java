package com.jk.explore.security.validation;

/**
 * A value refused at the trust boundary. The message names the field and the rule, and never repeats the raw input: the input may be
 * huge, may hold control characters that forge log lines, or may be markup that a careless error page would render.
 */
public final class Rejected extends Exception {

    private final String field;
    private final String rule;

    public Rejected(String field, String rule) {
        super(field + ": " + rule);
        this.field = field;
        this.rule = rule;
    }

    public String field() {
        return field;
    }

    public String rule() {
        return rule;
    }
}
