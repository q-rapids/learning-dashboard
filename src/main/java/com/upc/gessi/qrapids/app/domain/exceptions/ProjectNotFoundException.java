package com.upc.gessi.qrapids.app.domain.exceptions;

import com.upc.gessi.qrapids.app.presentation.rest.services.exceptions.ResourceNotFoundException;
import com.upc.gessi.qrapids.app.presentation.rest.services.helpers.Messages;

public class ProjectNotFoundException extends ResourceNotFoundException {
    public ProjectNotFoundException(String projectIdentifier) {
        super(String.format(Messages.PROJECT_NOT_FOUND, projectIdentifier));
    }

    public ProjectNotFoundException(String message, String... args) {
        super(message, args);
    }
}
