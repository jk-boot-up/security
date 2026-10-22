package com.jk.explore.security.logging;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;

/** %maskedEx: " | " and the exception and its causes, class and message only, with the shapes of secrets covered; no frames, so no code layout either. */
public class MaskedThrowableConverter extends ThrowableHandlingConverter {

    private final boolean mask;

    public MaskedThrowableConverter() {
        this(true);
    }

    protected MaskedThrowableConverter(boolean mask) {
        this.mask = mask;
    }

    @Override
    public String convert(ILoggingEvent event) {
        IThrowableProxy proxy = event.getThrowableProxy();
        if (proxy == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(" | ");
        String prefix = "";
        for (IThrowableProxy p = proxy; p != null; p = p.getCause()) {
            out.append(prefix).append(shortName(p.getClassName())).append(": ").append(mask ? Masker.mask(p.getMessage()) : p.getMessage());
            prefix = " <- caused by ";
        }
        return out.toString();
    }

    private static String shortName(String className) {
        return className.substring(className.lastIndexOf('.') + 1);
    }

    /** %plainEx: the same, with nothing covered: what a layout without care keeps. */
    public static class Plain extends MaskedThrowableConverter {

        public Plain() {
            super(false);
        }
    }
}
