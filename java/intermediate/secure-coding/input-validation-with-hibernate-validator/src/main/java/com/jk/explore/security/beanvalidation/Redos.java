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
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * A pattern with repetitions nested two deep, as a constraint, with its work counted: every character the regular expression engine reads is a step.
 * The JDK's engine remembers its way through the simpler (a+)+$ and takes quadratic time; one more level of nesting is exponential again.
 */
@Documented
@Constraint(validatedBy = Redos.Check.class)
@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Redos {

    String message() default "must match ((a+)+)+b";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** The steps taken by all checks so far. */
    AtomicLong STEPS = new AtomicLong();

    /** The check. */
    class Check implements ConstraintValidator<Redos, String> {

        private static final Pattern NESTED = Pattern.compile("((a+)+)+b");

        /** Runs a pattern's find() on the value, adding one step for every character the engine reads. */
        static boolean find(Pattern pattern, String value) {
            CharSequence counted = new CharSequence() {
                @Override
                public int length() {
                    return value.length();
                }

                @Override
                public char charAt(int index) {
                    STEPS.incrementAndGet();
                    return value.charAt(index);
                }

                @Override
                public CharSequence subSequence(int start, int end) {
                    return value.subSequence(start, end);
                }
            };
            return pattern.matcher(counted).find();
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }
            return find(NESTED, value);
        }
    }
}
