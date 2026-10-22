package com.jk.explore.security.logging;

import com.fasterxml.jackson.annotation.JsonValue;

/** A card number that prints as its last four digits, in a string and in JSON. */
public record Card(String number) {

    @Override
    public String toString() {
        return masked();
    }

    @JsonValue
    public String masked() {
        return "****" + number.substring(number.length() - 4);
    }
}
