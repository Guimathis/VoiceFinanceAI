package com.voicefinance.exception;

public class EmptyAudioException extends RuntimeException {
    public EmptyAudioException(String message) {
        super(message);
    }

    public EmptyAudioException() {
        super("O arquivo de áudio está vazio ou não foi fornecido.");
    }
}
