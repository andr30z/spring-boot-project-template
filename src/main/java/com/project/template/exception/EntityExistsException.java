package com.project.template.exception;


public class EntityExistsException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public EntityExistsException(String message) {
        super(message);
    }
}
