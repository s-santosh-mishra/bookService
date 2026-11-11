package com.servicehub.serv.exception;

public class BookingConflictException extends IllegalStateException {

    public BookingConflictException(String message) {
        super(message);
    }
}
