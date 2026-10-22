package com.jk.explore.security.apispring;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** The only fields a customer may change about themselves, with their limits. A field that is left out stays as it was; a body with any other field is refused before this record is built. */
record UpdateProfileRequest(@Size(min = 1, max = 40) String displayName, @Email @Size(max = 80) String email) {
}
