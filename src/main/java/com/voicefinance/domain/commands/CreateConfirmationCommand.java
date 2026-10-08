package com.voicefinance.domain.commands;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.UUID;

public record CreateConfirmationCommand(
        @NotNull(message = "expenseId é obrigatório")
        @ToolParam(description = "The id of the expense that requires confirmation") UUID expenseId,
        @NotEmpty(message = "A lista de campos não pode ser vazia")
        @ToolParam(description = "List of missing field names that need confirmation (e.g. category, occurredOn, description)") List<String> missingFields,
        @NotBlank(message = "A pergunta de esclarecimento é obrigatória")
        @ToolParam(description = "The clarification question to ask the user") String question
) {}
