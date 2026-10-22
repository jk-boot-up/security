package com.jk.explore.security.jacksondeser;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

/**
 * The cart as the first version keeps it in a cookie: lines with their prices, and a note whose kind is a class name. "Let the cookie
 * say which kind of note it holds" is a convenient way to support new kinds, and a way to let the cookie choose any class that implements {@link Note}.
 */
public record Cart(List<Line> lines, @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) Note gift) {

    public long total() {
        return lines.stream().mapToLong(l -> l.qty() * l.price()).sum();
    }
}
