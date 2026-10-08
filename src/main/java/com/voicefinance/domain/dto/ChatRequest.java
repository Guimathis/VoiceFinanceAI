package com.voicefinance.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "A ação deve ser preenchida.") String action
) {
}
