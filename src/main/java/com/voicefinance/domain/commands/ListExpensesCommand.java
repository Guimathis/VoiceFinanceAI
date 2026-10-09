package com.voicefinance.domain.commands;

import com.voicefinance.domain.enums.Category;
import com.voicefinance.domain.enums.ExpenseStatus;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDate;

public record ListExpensesCommand(
        @ToolParam(description = "Start date for filtering expenses (inclusive). Optional.", required = false)
        LocalDate startDate,

        @ToolParam(description = "End date for filtering expenses (inclusive). Optional.", required = false)
        LocalDate endDate,

        @ToolParam(description = "Category of the expense to filter by. Optional.", required = false)
        Category category,

        @ToolParam(description = "Status of the expense to filter by (PENDING_CONFIRMATION, CONFIRMED, CANCELED). Optional.", required = false)
        ExpenseStatus status,

        @ToolParam(description = "Keyword to search within the expense description. Optional.", required = false)
        String description,

        @ToolParam(description = "Maximum number of expenses to retrieve (default is 10). Optional.", required = false)
        Integer limit
) {
}
