package com.voicefinance.domain.commands;

import com.voicefinance.domain.enums.Category;
import jakarta.validation.constraints.NotNull;
import org.springframework.ai.tool.annotation.ToolParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record StartExpenseCommand(
        @ToolParam(description = "The amount/value of the expense", required = false)
        BigDecimal amount,
        @ToolParam(description = "The currency of the expense", required = false)
        String currency,
        @ToolParam(description = "The description or category of the expense", required = false)
        String description,
        @ToolParam(description = "The category or category of the expense", required = false)
        Category category,
        @ToolParam(description = "Date of expense", required = false)
        LocalDate OccurredOn

) {
}
