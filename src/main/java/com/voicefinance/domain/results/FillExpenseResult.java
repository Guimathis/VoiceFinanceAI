package com.voicefinance.domain.results;

import com.voicefinance.domain.enums.ExpenseStatus;

import java.util.List;
import java.util.UUID;

public sealed interface FillExpenseResult {
    record Saved() implements FillExpenseResult {}
    record Confirmed() implements FillExpenseResult {}
    record Canceled() implements FillExpenseResult {}
    record Incomplete(List<String> missing) implements FillExpenseResult {}
    record NotFound(UUID id) implements FillExpenseResult {}
    record NotEditable(ExpenseStatus status) implements FillExpenseResult {}
    record InvalidFields(List<String> invalidFields, String message) implements FillExpenseResult {}
}
