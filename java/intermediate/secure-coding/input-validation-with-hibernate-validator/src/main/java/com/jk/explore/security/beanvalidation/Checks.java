package com.jk.explore.security.beanvalidation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.messageinterpolation.ExpressionLanguageFeatureLevel;

/** A validator the way a service would build one: English messages, and a factory that gives a constraint validator the services it needs. */
public final class Checks implements AutoCloseable {

    private final ValidatorFactory factory;
    private final Validator validator;
    private final Catalogue catalogue;

    public Checks(Catalogue catalogue) {
        this(catalogue, null);
    }

    /** @param customLevel how much of the expression language a message template built inside a constraint may use, or null for Hibernate Validator's own default */
    public Checks(Catalogue catalogue, ExpressionLanguageFeatureLevel customLevel) {
        this.catalogue = catalogue;
        ConstraintValidatorFactory base = Validation.byDefaultProvider().configure().getDefaultConstraintValidatorFactory();
        ConstraintValidatorFactory withCatalogue = new ConstraintValidatorFactory() {
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
                return key == InStock.Check.class ? (T) new InStock.Check(catalogue) : base.getInstance(key);
            }

            @Override
            public void releaseInstance(ConstraintValidator<?, ?> instance) {
                base.releaseInstance(instance);
            }
        };
        HibernateValidatorConfiguration configuration = Validation.byProvider(HibernateValidator.class).configure().defaultLocale(Locale.ENGLISH).locales(Locale.ENGLISH)
                .constraintValidatorFactory(withCatalogue);
        if (customLevel != null) {
            configuration.customViolationExpressionLanguageFeatureLevel(customLevel);
        }
        this.factory = configuration.buildValidatorFactory();
        this.validator = factory.getValidator();
    }

    public Validator validator() {
        return validator;
    }

    public Catalogue catalogue() {
        return catalogue;
    }

    /** The violations as sorted lines of "path: message". */
    public static List<String> lines(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream().map(v -> v.getPropertyPath() + ": " + v.getMessage()).sorted().toList();
    }

    /** Validates in the order limits, default, expensive. */
    public <T> List<String> check(T value) {
        return lines(validator.validate(value, Basic.Ordered.class));
    }

    @Override
    public void close() {
        factory.close();
    }
}
