package com.voicefinance.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AudioChatResponse(
        UUID captureId,
        String transcript,
        String message,
        BigDecimal durationSeconds,
        Long sizeBytes,
        String contentType,
        Instant createdAt
) {}
