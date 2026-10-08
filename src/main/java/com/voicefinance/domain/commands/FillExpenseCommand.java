package com.voicefinance.domain.commands;

import com.voicefinance.domain.enums.Category;
import jakarta.validation.constraints.NotNull;
import org.springframework.ai.tool.annotation.ToolParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FillExpenseCommand(
        @NotNull(message = "O ID do gasto é obrigatório")
        @ToolParam(description = "The id of the expense") UUID expenseId,
        @ToolParam(description = "The amount/value of the expense") BigDecimal amount,
        @ToolParam(description = "The currency of the expense") String currency,
        @ToolParam(description = "The description or category of the expense") String description,
        @ToolParam(description = "The category or category of the expense") Category category,
        @ToolParam(description = "Date of expense") LocalDate OccurredOn
) {
}
