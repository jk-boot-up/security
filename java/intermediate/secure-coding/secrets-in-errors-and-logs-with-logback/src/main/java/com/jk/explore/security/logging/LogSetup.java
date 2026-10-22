package com.jk.explore.security.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import net.logstash.logback.composite.loggingevent.ArgumentsJsonProvider;
import net.logstash.logback.composite.loggingevent.LogLevelJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggingEventJsonProviders;
import net.logstash.logback.composite.loggingevent.LoggerNameJsonProvider;
import net.logstash.logback.composite.loggingevent.MdcJsonProvider;
import net.logstash.logback.composite.loggingevent.MessageJsonProvider;
import net.logstash.logback.composite.loggingevent.ThrowableClassNameJsonProvider;
import net.logstash.logback.composite.loggingevent.ThrowableMessageJsonProvider;
import net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder;
import net.logstash.logback.mask.MaskingJsonGeneratorDecorator;

/** One Logback logger writing into memory, configured in code: a layout, or JSON. A context of its own, so that nothing else in the program writes into it. */
public final class LogSetup implements AutoCloseable {

    private final LoggerContext context = new LoggerContext();
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final Logger logger;

    private LogSetup(Encoder<ILoggingEvent> encoder) {
        context.setName("lab");
        context.setMDCAdapter(new ch.qos.logback.classic.util.LogbackMDCAdapter());
        context.start();
        encoder.setContext(context);
        encoder.start();
        OutputStreamAppender<ILoggingEvent> appender = new OutputStreamAppender<>();
        appender.setContext(context);
        appender.setName("memory");
        appender.setEncoder(encoder);
        appender.setOutputStream(bytes);
        appender.start();
        logger = context.getLogger("shop.checkout");
        logger.setLevel(Level.INFO);
        logger.setAdditive(false);
        logger.addAppender(appender);
    }

    public Logger logger() {
        return logger;
    }

    /** A line layout with the conversion words %mask and %maskedEx registered. */
    public static LogSetup pattern(String pattern) {
        LoggerContext scratch = new LoggerContext();
        PatternLayout layout = new PatternLayout();
        layout.setContext(scratch);
        layout.setPattern(pattern);
        layout.getInstanceConverterMap().put("mask", MaskConverter::new);
        layout.getInstanceConverterMap().put("maskedEx", MaskedThrowableConverter::new);
        layout.getInstanceConverterMap().put("plainEx", MaskedThrowableConverter.Plain::new);
        layout.start();
        LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
        encoder.setLayout(layout);
        return new LogSetup(encoder);
    }

    /** JSON events with a fixed set of fields (no times, so that the output can be compared), optionally with masking. */
    public static LogSetup json(MaskingJsonGeneratorDecorator masking) {
        LoggingEventCompositeJsonEncoder encoder = new LoggingEventCompositeJsonEncoder();
        LoggingEventJsonProviders providers = new LoggingEventJsonProviders();
        providers.addLogLevel(new LogLevelJsonProvider());
        providers.addLoggerName(new LoggerNameJsonProvider());
        providers.addMessage(new MessageJsonProvider());
        providers.addMdc(new MdcJsonProvider());
        providers.addArguments(new ArgumentsJsonProvider());
        providers.addThrowableClassName(new ThrowableClassNameJsonProvider());
        providers.addThrowableMessage(new ThrowableMessageJsonProvider());
        encoder.setProviders(providers);
        if (masking != null) {
            masking.start();
            encoder.addDecorator(masking);
        }
        return new LogSetup(encoder);
    }

    /** Puts a value in this logger's own MDC (the context data that every event carries). */
    public void mdc(String key, String value) {
        context.getMDCAdapter().put(key, value);
    }

    public String output() {
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Override
    public void close() {
        context.stop();
    }
}
