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
import java.util.Map;
import java.util.regex.Pattern;

/** A rule across two fields: the postal code has the shape of the country's. The violation is reported on the postal code, where the person can fix it. */
@Documented
@Constraint(validatedBy = PostalCodeMatchesCountry.Check.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PostalCodeMatchesCountry {

    String message() default "does not look like a postal code of that country";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** The check. */
    class Check implements ConstraintValidator<PostalCodeMatchesCountry, Checkout> {

        private static final Map<String, Pattern> SHAPES = Map.of("DE", Pattern.compile("\\d{5}"), "IN", Pattern.compile("\\d{6}"), "GB", Pattern.compile("[A-Z0-9]{2,4} ?[0-9][A-Z]{2}"));

        @Override
        public boolean isValid(Checkout value, ConstraintValidatorContext context) {
            if (value == null || value.country() == null || value.postalCode() == null || !SHAPES.containsKey(value.country())) {
                return true;
            }
            if (SHAPES.get(value.country()).matcher(value.postalCode()).matches()) {
                return true;
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate()).addPropertyNode("postalCode").addConstraintViolation();
            return false;
        }
    }
}
