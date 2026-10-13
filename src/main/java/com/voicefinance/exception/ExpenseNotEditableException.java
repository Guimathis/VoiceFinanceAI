package com.voicefinance.exception;

import com.voicefinance.domain.enums.ExpenseStatus;

public class ExpenseNotEditableException extends RuntimeException {
    public ExpenseNotEditableException(ExpenseStatus status) {
        super("A despesa não pode ser alterada no estado atual: " + status);
    }
}
