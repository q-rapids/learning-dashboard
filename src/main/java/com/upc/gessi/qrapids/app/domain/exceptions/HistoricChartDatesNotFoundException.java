package com.upc.gessi.qrapids.app.domain.exceptions;

import com.upc.gessi.qrapids.app.presentation.rest.services.exceptions.ResourceNotFoundException;
import com.upc.gessi.qrapids.app.presentation.rest.services.helpers.Messages;

public class HistoricChartDatesNotFoundException extends ResourceNotFoundException {

    public HistoricChartDatesNotFoundException() {
        super(Messages.HISTORIC_CHART_DATES_NOT_FOUND);
    }

    public HistoricChartDatesNotFoundException(String message, String... args) {
        super(message, args);
    }
}
