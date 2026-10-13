package com.voicefinance.service.audio;

import java.math.BigDecimal;

public record ValidatedAudioMetadata(
        String detectedMimeType,
        String extension,
        long sizeBytes,
        BigDecimal durationSeconds,
        String originalFilename
) {}
