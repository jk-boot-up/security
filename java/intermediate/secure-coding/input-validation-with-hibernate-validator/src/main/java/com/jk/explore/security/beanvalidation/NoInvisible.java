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
import java.text.Normalizer;

/** No control or invisible characters, and already in normal form C: what is checked is what is stored. */
@Documented
@Constraint(validatedBy = NoInvisible.Check.class)
@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoInvisible {

    String message() default "no control or invisible characters, in normal form C";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** The check. */
    class Check implements ConstraintValidator<NoInvisible, String> {

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }
            if (!Normalizer.isNormalized(value, Normalizer.Form.NFC)) {
                return false;
            }
            return value.codePoints().noneMatch(c -> Character.getType(c) == Character.CONTROL || Character.getType(c) == Character.FORMAT || Character.getType(c) == Character.LINE_SEPARATOR
                    || Character.getType(c) == Character.PARAGRAPH_SEPARATOR);
        }
    }
}
