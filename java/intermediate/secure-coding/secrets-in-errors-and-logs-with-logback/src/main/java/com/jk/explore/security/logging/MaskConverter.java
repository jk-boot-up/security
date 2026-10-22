package com.jk.explore.security.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/** %mask: the formatted message on one line, with the shapes of secrets covered. */
public class MaskConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return Masker.mask(event.getFormattedMessage()).replace("\r", "\\r").replace("\n", "\\n");
    }
}
