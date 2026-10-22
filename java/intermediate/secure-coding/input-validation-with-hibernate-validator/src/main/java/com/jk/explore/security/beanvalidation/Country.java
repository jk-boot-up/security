package com.jk.explore.security.beanvalidation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** A two-letter ISO country code in capitals, as the JDK knows them. */
@Documented
@Constraint(validatedBy = Country.Check.class)
@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Country {

    String message() default "a two-letter country code such as IN or DE";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** The check. */
    class Check implements ConstraintValidator<Country, String> {

        private static final Set<String> CODES = Arrays.stream(java.util.Locale.getISOCountries()).collect(Collectors.toUnmodifiableSet());

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || CODES.contains(value);
        }
    }
}
