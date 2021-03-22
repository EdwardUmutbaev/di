package ru.edwum.di.exception;

public class AmbiguousValueNameException extends RuntimeException {
    public AmbiguousValueNameException(String message) {
        super(message);
    }

    public AmbiguousValueNameException() {
        super();
    }

    public AmbiguousValueNameException(String message, Throwable cause) {
        super(message, cause);
    }

    public AmbiguousValueNameException(Throwable cause) {
        super(cause);
    }

    protected AmbiguousValueNameException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
