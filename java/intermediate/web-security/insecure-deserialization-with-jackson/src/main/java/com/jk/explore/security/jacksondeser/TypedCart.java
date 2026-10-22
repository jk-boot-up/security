package com.jk.explore.security.jacksondeser;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

/** The same cookie, but the kind of note is a short name from a list kept here, never a class name from the cookie. */
public record TypedCart(List<Line> lines,
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
        @JsonSubTypes({@JsonSubTypes.Type(value = Note.Gift.class, name = "gift"), @JsonSubTypes.Type(value = Note.Coupon.class, name = "coupon")})
        Note gift) {

    public long total() {
        return lines.stream().mapToLong(l -> l.qty() * l.price()).sum();
    }
}
