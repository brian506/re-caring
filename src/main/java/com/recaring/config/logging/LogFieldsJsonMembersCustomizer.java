package com.recaring.config.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.event.KeyValuePair;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

import java.util.List;

public class LogFieldsJsonMembersCustomizer implements StructuredLoggingJsonMembersCustomizer<ILoggingEvent> {

    @Override
    public void customize(JsonWriter.Members<ILoggingEvent> members) {
        members.add(LogDomain.FIELD, LogFieldsJsonMembersCustomizer::loggerDomain).whenNotNull();
        members.from(event -> LogMessagePrefix.parse(event.getFormattedMessage()))
                .whenNotNull()
                .usingMembers(prefix -> {
                    prefix.add("category", LogMessagePrefix::category).whenNotNull();
                    prefix.add("detail", LogMessagePrefix::detail).whenNotNull();
                });
    }

    private static String loggerDomain(ILoggingEvent event) {
        List<KeyValuePair> pairs = event.getKeyValuePairs();
        if (pairs != null && pairs.stream().anyMatch(pair -> LogDomain.FIELD.equals(pair.key))) {
            return null;
        }
        return LogDomain.resolve(event.getLoggerName());
    }
}
