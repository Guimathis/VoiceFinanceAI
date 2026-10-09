package com.voicefinance.domain.dto;

import com.voicefinance.domain.enums.ConfirmationField;
import com.voicefinance.domain.enums.ConfirmationStatus;

import java.util.UUID;

public record ConfirmationResponse(
        UUID id,
        String question,
        String answer,
        ConfirmationField field,
        ConfirmationStatus status
) {
}
