package com.voicefinance.exception;

public class AudioStorageException extends RuntimeException {
    public AudioStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public AudioStorageException(String message) {
        super(message);
    }
}
