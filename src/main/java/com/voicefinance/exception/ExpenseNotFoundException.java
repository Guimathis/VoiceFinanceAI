package com.voicefinance.exception;

import java.util.UUID;

public class ExpenseNotFoundException extends RuntimeException {
    public ExpenseNotFoundException(UUID id) {
        super("Despesa não encontrada com id: " + id);
    }
}
