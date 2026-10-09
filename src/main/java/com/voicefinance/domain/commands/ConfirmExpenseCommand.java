package com.voicefinance.domain.commands;

import jakarta.validation.constraints.NotNull;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.UUID;

public record ConfirmExpenseCommand(
        @NotNull(message = "The expenseId can't be null")
        @ToolParam(description = "The id of the expense")
        UUID expenseId

) {
}
