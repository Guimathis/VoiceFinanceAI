package com.voicefinance.service;

import com.voicefinance.domain.annotation.ConfirmableFields;
import com.voicefinance.domain.entity.Expense;
import com.voicefinance.domain.results.FillExpenseResult;
import com.voicefinance.repository.ExpenseRepository;
import com.voicefinance.domain.commands.FillExpenseCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import com.voicefinance.domain.entity.ExpenseFieldConfirmation;
import com.voicefinance.domain.enums.ConfirmationField;
import com.voicefinance.domain.enums.ConfirmationStatus;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ConfirmableFields confirmableFields;

    public void Create(Expense expense) {
        expenseRepository.save(expense);
    }

    @Transactional
    public FillExpenseResult fill(FillExpenseCommand cmd) {
        Expense expense = expenseRepository.findById(cmd.expenseId()).orElse(null);
        if (expense == null) return new FillExpenseResult.NotFound(cmd.expenseId());
        if (!expense.isEditable()) return new FillExpenseResult.NotEditable(expense.getStatus());

        expense.fill(cmd);
        expenseRepository.save(expense);

        List<String> missing = confirmableFields.missing(expense);
        return missing.isEmpty() ? new FillExpenseResult.Saved() : new FillExpenseResult.Incomplete(missing);
    }

    @Transactional
    public FillExpenseResult createExpenseFieldConfirmation(List<String> missingFields, UUID expenseId, String question) {
        Expense expense = expenseRepository.findById(expenseId).orElse(null);
        if (expense == null) return new FillExpenseResult.NotFound(expenseId);
        if (!expense.isEditable()) return new FillExpenseResult.NotEditable(expense.getStatus());

        if (missingFields == null || missingFields.isEmpty()) {
            return new FillExpenseResult.InvalidFields(List.of(), "Nenhum campo informado para confirmação.");
        }

        List<String> invalidFields = confirmableFields.isValid(Expense.class, missingFields);
        if (!invalidFields.isEmpty()) {
            return new FillExpenseResult.InvalidFields(invalidFields, "Campos não suportados para confirmação.");
        }

        for (String fieldName : missingFields) {
            ExpenseFieldConfirmation confirmation = ExpenseFieldConfirmation.builder()
                    .id(UUID.randomUUID())
                    .question(question)
                    .field(ConfirmationField.fromFieldName(fieldName))
                    .status(ConfirmationStatus.PENDING)
                    .build();
            expense.addConfirmation(confirmation);
        }

        expenseRepository.save(expense);
        log.info("Confirmações criadas para gasto id={}: campos={}", expenseId, missingFields);

        return new FillExpenseResult.Incomplete(missingFields);
    }

}

