package com.jk.explore.security.beanvalidation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** One line of a cart. */
public record Line(
        @NotNull @Pattern(regexp = "[A-Z]{3}-\\d{4}") @InStock(groups = Basic.Expensive.class) String sku,
        @Min(1) @Max(99) int quantity) {
}
