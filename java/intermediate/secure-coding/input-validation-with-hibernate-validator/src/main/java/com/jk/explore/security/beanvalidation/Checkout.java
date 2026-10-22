package com.jk.explore.security.beanvalidation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** What the checkout page posts. There is no price: the catalogue has it. */
@PostalCodeMatchesCountry
public record Checkout(
        @NotBlank @Size(max = 254, groups = Basic.Limits.class) @Email String email,
        @NotNull @Country String country,
        @NotBlank @Size(max = 10, groups = Basic.Limits.class) String postalCode,
        @NotNull @NoInvisible @Size(min = 3, max = 20, groups = Basic.Limits.class) @Pattern(regexp = "[a-z0-9._-]+") String login,
        @NotEmpty @Size(max = 50, groups = Basic.Limits.class) List<@NotNull @Valid Line> lines) {
}
