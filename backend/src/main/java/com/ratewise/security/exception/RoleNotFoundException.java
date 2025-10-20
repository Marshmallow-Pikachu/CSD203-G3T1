package com.ratewise.security.exception;

public class RoleNotFoundException extends RuntimeException {

    public RoleNotFoundException(String message) {
        super(message);
    }

    public RoleNotFoundException(Long id) {
        super("Could not find role " + id);
    }
}
