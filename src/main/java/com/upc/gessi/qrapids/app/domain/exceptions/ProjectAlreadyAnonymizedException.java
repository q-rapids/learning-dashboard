package com.upc.gessi.qrapids.app.domain.exceptions;

import com.upc.gessi.qrapids.app.presentation.rest.services.exceptions.ConflictException;
import com.upc.gessi.qrapids.app.presentation.rest.services.helpers.Messages;

import java.util.List;
import java.util.stream.Collectors;

public class ProjectAlreadyAnonymizedException extends ConflictException {

    public ProjectAlreadyAnonymizedException(String projectIdentifier) {
        super(String.format(Messages.PROJECT_ALREADY_ANONYMIZED, projectIdentifier));
    }

    public ProjectAlreadyAnonymizedException(List<String> projectIdentifiers) {
        super(String.format(Messages.PROJECTS_ALREADY_ANONYMIZED, String.join(",", projectIdentifiers)));
    }

    public ProjectAlreadyAnonymizedException(String message, String... args) {
        super(message, args);
    }
}

