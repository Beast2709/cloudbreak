package com.sequenceiq.cloudbreak.controller.mapper;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.json.ValidationResult;

@Component
public class ConstraintViolationExceptionMapper extends SendNotificationExceptionMapper<ConstraintViolationException> {

    @Override
    protected String getErrorMessage(ConstraintViolationException exception) {
        StringBuilder violations = new StringBuilder();
        for (ConstraintViolation<?> constraintViolation : exception.getConstraintViolations()) {
            violations.append("constraintviolation: ")
                    .append(constraintViolation.getPropertyPath())
                    .append(" - ")
                    .append(constraintViolation.getMessage())
                    .append('\n');
        }
        return violations.toString();
    }

    @Override
    protected Object getEntity(ConstraintViolationException exception) {
        ValidationResult validationResult = new ValidationResult();
        exception.getConstraintViolations()
                .forEach(violation -> {
                    String propertyPath = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "";
                    validationResult.addValidationError(propertyPath, violation.getMessage());
                    });
        return validationResult;
    }

    @Override
    public Status getResponseStatus(ConstraintViolationException exception) {
        return Status.BAD_REQUEST;
    }

    @Override
    public Class<ConstraintViolationException> getExceptionType() {
        return ConstraintViolationException.class;
    }

}
