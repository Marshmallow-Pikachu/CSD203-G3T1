package com.ratewise.security.exception;


public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String id) {
        super("Could not find user with ID: " + id);
    }
}
