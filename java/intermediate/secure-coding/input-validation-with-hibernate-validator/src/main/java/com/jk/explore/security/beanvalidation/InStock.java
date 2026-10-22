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

/** The sku is in the catalogue. It costs a lookup, so it belongs in the expensive group. */
@Documented
@Constraint(validatedBy = InStock.Check.class)
@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface InStock {

    String message() default "not in the catalogue";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** The check; the catalogue comes from the validator factory, as a service would. */
    class Check implements ConstraintValidator<InStock, String> {

        private final Catalogue catalogue;

        public Check(Catalogue catalogue) {
            this.catalogue = catalogue;
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || catalogue.contains(value);
        }
    }
}
