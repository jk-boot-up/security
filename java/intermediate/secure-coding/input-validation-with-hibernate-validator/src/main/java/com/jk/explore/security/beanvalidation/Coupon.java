package com.jk.explore.security.beanvalidation;

import jakarta.validation.constraints.Size;

/** A coupon code: its length is checked first, so the slow pattern never sees a long value. */
public record Coupon(@Size(max = 20, groups = Basic.Limits.class) @Redos(groups = Basic.Expensive.class) String code) {
}
