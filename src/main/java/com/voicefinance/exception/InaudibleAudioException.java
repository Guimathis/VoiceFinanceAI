package com.voicefinance.exception;

public class InaudibleAudioException extends RuntimeException {
    public InaudibleAudioException(String message) {
        super(message);
    }

    public InaudibleAudioException() {
        super("Não foi possível identificar nenhuma fala com clareza no áudio enviado.");
    }
}
