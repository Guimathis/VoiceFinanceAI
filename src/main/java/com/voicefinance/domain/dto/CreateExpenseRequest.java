package com.voicefinance.domain.dto;

import com.voicefinance.domain.enums.Category;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateExpenseRequest(
        @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        BigDecimal amount,

        String currency,

        @NotNull(message = "A categoria é obrigatória.")
        Category category,

        @Size(max = 200, message = "A descrição pode ter no máximo 200 caracteres.")
        String description,

        LocalDate occurredOn
) {
}
