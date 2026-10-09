package com.voicefinance.domain.dto;

import com.voicefinance.domain.enums.Category;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseSummaryResponse(
        BigDecimal totalSpent,
        BigDecimal totalPending,
        long totalExpensesCount,
        long confirmedCount,
        long pendingCount,
        long canceledCount,
        List<CategorySummary> categoryBreakdown
) {
    public record CategorySummary(
            Category category,
            BigDecimal total,
            long count,
            double percentage
    ) {
    }
}
