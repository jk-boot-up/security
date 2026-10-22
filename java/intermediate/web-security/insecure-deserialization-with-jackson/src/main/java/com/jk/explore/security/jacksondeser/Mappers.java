package com.jk.explore.security.jacksondeser;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

/** The ways the shop configures Jackson in this lesson. Each one is a few lines, and each line is a decision. */
public final class Mappers {

    private Mappers() {
    }

    /** Jackson 3's defaults: class names in the cookie are resolved only against base types that are not too generic. */
    public static JsonMapper defaults() {
        return JsonMapper.builder().build();
    }

    /** What "make polymorphism just work" looks like: every non-final type may carry a class name, and any class may be named. */
    public static JsonMapper anyClassMayBeNamed() {
        PolymorphicTypeValidator anything = BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build();
        return JsonMapper.builder().activateDefaultTyping(anything, DefaultTyping.NON_FINAL).build();
    }

    /** Class names stay, but only the classes a note may be are allowed. The validator is the list. */
    public static JsonMapper onlyOurNotesMayBeNamed() {
        PolymorphicTypeValidator ours = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Note.Gift.class)
                .allowIfSubType(Note.Coupon.class)
                .build();
        return JsonMapper.builder().polymorphicTypeValidator(ours).build();
    }

    /** Strict binding for a request: an unknown property is an error, and the stream limits are tightened to what a cart needs. */
    public static JsonMapper strictRequest() {
        JsonFactory factory = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(10).maxStringLength(64).maxNumberLength(10).build())
                .build();
        return JsonMapper.builder(factory)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .build();
    }
}
