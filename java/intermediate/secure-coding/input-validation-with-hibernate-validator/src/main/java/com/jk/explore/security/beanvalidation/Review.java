package com.jk.explore.security.beanvalidation;

import jakarta.validation.constraints.Size;

/** A product review title whose message repeats the value, and a coupon field written two ways, for what validation does not do. */
public record Review(@Size(min = 3, max = 10, message = "'${validatedValue}' is not 3 to 10 characters") String title) {

    /** The coupon field with a constraint that puts the visitor's text in its template. */
    public record Unsafe(@UnsafeMessage String coupon) {
    }

    /** The coupon field with a constraint that passes the visitor's text as a parameter. */
    public record Safe(@SafeMessage String coupon) {
    }
}
