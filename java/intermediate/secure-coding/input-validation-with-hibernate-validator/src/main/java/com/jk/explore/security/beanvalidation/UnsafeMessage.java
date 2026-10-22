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

/** A custom constraint written the way it should not be: it puts what the visitor typed into the message template, which is then interpolated. */
@Documented
@Constraint(validatedBy = UnsafeMessage.Check.class)
@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface UnsafeMessage {

    String message() default "invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** The check: it never accepts, and says why with the visitor's own text in the template. */
    class Check implements ConstraintValidator<UnsafeMessage, String> {

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("the coupon " + value + " does not exist").addConstraintViolation();
            return false;
        }
    }

    /** The safe version: the visitor's text is a parameter of the message, which is not interpolated again. */
    class Safe implements ConstraintValidator<SafeMessage, String> {

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();
            context.unwrap(org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext.class).addMessageParameter("coupon", value)
                    .buildConstraintViolationWithTemplate("the coupon {coupon} does not exist").addConstraintViolation();
            return false;
        }
    }
}
