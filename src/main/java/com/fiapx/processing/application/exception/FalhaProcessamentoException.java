package com.fiapx.processing.application.exception;

public class FalhaProcessamentoException extends RuntimeException {

    public FalhaProcessamentoException(String message) {
        super(message);
    }

    public FalhaProcessamentoException(String message, Throwable cause) {
        super(message, cause);
    }
}
