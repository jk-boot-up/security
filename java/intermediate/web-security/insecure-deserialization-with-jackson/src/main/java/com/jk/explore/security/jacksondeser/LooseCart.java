package com.jk.explore.security.jacksondeser;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

/** The same idea with the laziest possible type: an Object. Jackson 3 refuses to resolve class names against this base type by default. */
public record LooseCart(List<Line> lines, @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) Object gift) {
}
