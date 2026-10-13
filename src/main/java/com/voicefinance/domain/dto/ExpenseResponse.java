package com.voicefinance.domain.dto;

import com.voicefinance.domain.entity.Expense;
import com.voicefinance.domain.enums.Category;
import com.voicefinance.domain.enums.ExpenseOrigin;
import com.voicefinance.domain.enums.ExpenseStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        BigDecimal amount,
        String currency,
        Category category,
        String description,
        LocalDate occurredOn,
        ExpenseStatus status,
        ExpenseOrigin origin,
        Instant createdAt,
        Instant confirmedAt,
        List<ConfirmationResponse> confirmations
) {
    public static ExpenseResponse from(Expense expense) {
        if (expense == null) {
            return null;
        }

        BigDecimal amount = expense.getAmount() != null ? expense.getAmount().amount() : null;
        String currency = expense.getAmount() != null ? expense.getAmount().currency() : null;

        List<ConfirmationResponse> confirmationResponses = expense.getExpenseConfirmations() != null
                ? expense.getExpenseConfirmations().stream()
                .map(c -> new ConfirmationResponse(
                        c.getId(),
                        c.getQuestion(),
                        c.getAnswer(),
                        c.getField(),
                        c.getStatus()
                ))
                .toList()
                : List.of();

        return new ExpenseResponse(
                expense.getId(),
                amount,
                currency,
                expense.getCategory(),
                expense.getDescription(),
                expense.getOccurredOn(),
                expense.getStatus(),
                expense.getOrigin(),
                expense.getCreatedAt(),
                expense.getConfirmedAt(),
                confirmationResponses
        );
    }
}
