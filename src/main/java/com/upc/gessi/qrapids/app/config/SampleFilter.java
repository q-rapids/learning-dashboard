package com.upc.gessi.qrapids.app.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

import java.util.Objects;

public class SampleFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if ((event.getMessage().contains("GET") || event.getMessage().contains("POST") || event.getMessage().contains("DELETE") || event.getMessage().contains("PUT"))
            && ((event.getMessage().contains("Action performed by")) || (event.getMessage().contains("No user is logged in yet")))
            && (!event.getMessage().contains("api") && !event.getMessage().contains("js") && !event.getMessage().contains("css") &&
                        !event.getMessage().contains("icons") && !event.getMessage().contains(".ico") && !event.getMessage().contains("fonts") &&
                        !event.getMessage().contains("ws") && !event.getMessage().contains("elasticsearch") && !event.getMessage().contains("http-outgoing-") &&
                        !event.getMessage().contains("Ant") && !event.getMessage().contains("FORWARD") && !event.getMessage().contains("doesn't match") )) {
            return FilterReply.ACCEPT;
        }
        else if (Objects.equals(event.getLoggerName(), "ActionLogger")) {
            return FilterReply.ACCEPT;
        }
        else {
            return FilterReply.DENY;
        }
    }
}