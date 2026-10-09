package com.voicefinance.exception;

public class InvalidAudioFormatException extends RuntimeException {
    public InvalidAudioFormatException(String message) {
        super(message);
    }

    public InvalidAudioFormatException() {
        super("Formato de arquivo de áudio não suportado ou assinatura binária inválida.");
    }
}
