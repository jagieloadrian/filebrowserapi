package com.anjo.filebrowserapi.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import org.springframework.boot.json.JsonWriter
import org.springframework.boot.logging.structured.StructuredLogFormatter

class JsonLogFormatter : StructuredLogFormatter<ILoggingEvent> {
    private val writer =
        JsonWriter
            .of<ILoggingEvent> { members ->
                members.add("timestamp") { event: ILoggingEvent -> event.instant.toString() }
                members.add("level") { event: ILoggingEvent -> event.level.toString() }
                members.add("message") { event: ILoggingEvent -> event.formattedMessage }
                members.add("logger") { event: ILoggingEvent -> event.loggerName }
                members.add("traceId") { event: ILoggingEvent -> event.mdcPropertyMap["traceId"] }.whenNotNull()
            }.withNewLineAtEnd()

    override fun format(event: ILoggingEvent): String = writer.writeToString(event)
}
