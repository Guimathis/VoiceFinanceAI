package com.voicefinance.domain.dto;

import com.voicefinance.domain.enums.Category;
import org.springframework.ai.tool.annotation.ToolParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseRequest(
        @ToolParam(description = "The amount/value of the expense") UUID userId,
        @ToolParam(description = "The category or category of the expense") Category category,
        @ToolParam(description = "The description or category of the expense") String description,
        @ToolParam(description = "Date of expense") LocalDate OccurredOnDate,

        @ToolParam(description = "The amount/value of the expense") BigDecimal amount,
        @ToolParam(description = "The currency of the expense") String currency

) {}