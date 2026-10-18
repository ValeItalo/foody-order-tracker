package com.foody.ordertracker.exception;

public class EmailAlreadyInUseException extends RuntimeException {

    public EmailAlreadyInUseException(String email) {
        super("O e-mail já está cadastrado: " + email);
    }
}