package com.recaring.config.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

public class LogFieldsJsonMembersCustomizer implements StructuredLoggingJsonMembersCustomizer<ILoggingEvent> {

    @Override
    public void customize(JsonWriter.Members<ILoggingEvent> members) {
        members.add("domain", event -> LogDomain.resolve(event.getLoggerName())).whenNotNull();
        members.from(event -> LogMessagePrefix.parse(event.getFormattedMessage()))
                .whenNotNull()
                .usingMembers(prefix -> {
                    prefix.add("category", LogMessagePrefix::category).whenNotNull();
                    prefix.add("detail", LogMessagePrefix::detail).whenNotNull();
                });
    }
}
