package com.voicefinance.exception;

public class AudioTooLargeException extends RuntimeException {
    public AudioTooLargeException(long sizeBytes, long maxSizeBytes) {
        super(String.format("O arquivo de áudio (%d bytes) excede o limite máximo permitido de %d bytes.", sizeBytes, maxSizeBytes));
    }

    public AudioTooLargeException(String message) {
        super(message);
    }
}
