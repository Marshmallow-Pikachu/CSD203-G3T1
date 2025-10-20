package com.ratewise.security.exception;


public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
    public UserNotFoundException(Long id) {
        super("Could not find user " + id);
    }

}
