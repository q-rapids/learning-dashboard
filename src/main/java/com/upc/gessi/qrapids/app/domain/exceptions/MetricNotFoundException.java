package com.upc.gessi.qrapids.app.domain.exceptions;

import com.upc.gessi.qrapids.app.presentation.rest.services.exceptions.ResourceNotFoundException;
import com.upc.gessi.qrapids.app.presentation.rest.services.helpers.Messages;

public class MetricNotFoundException extends ResourceNotFoundException {

    public MetricNotFoundException() {
        super(Messages.METRICS_NOT_FOUND);
    }

    public MetricNotFoundException(String metricId) {
        super(String.format(Messages.METRIC_NOT_FOUND, metricId));
    }

    public MetricNotFoundException(String message, String... args) {
        super(message, args);
    }
}
