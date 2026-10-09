package com.voicefinance.exception;

import java.math.BigDecimal;

public class AudioDurationExceededException extends RuntimeException {
    public AudioDurationExceededException(BigDecimal durationSeconds, int maxDurationSeconds) {
        super(String.format("A duração do áudio (%.1fs) excede o limite máximo permitido de %d segundos.",
                durationSeconds != null ? durationSeconds.doubleValue() : 0.0, maxDurationSeconds));
    }

    public AudioDurationExceededException(String message) {
        super(message);
    }
}
