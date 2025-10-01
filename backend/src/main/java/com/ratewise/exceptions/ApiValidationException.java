package com.ratewise.exceptions;

import java.util.List;

public class ApiValidationException extends RuntimeException {
    private final List<String> errors;

    public ApiValidationException(List<String> errors) {
        super("Invalid input.");       // top-level title
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
