package com.voicefinance.service;

import com.voicefinance.domain.annotation.ConfirmableFields;
import com.voicefinance.domain.commands.*;
import com.voicefinance.domain.entity.Expense;
import com.voicefinance.domain.entity.Money;
import com.voicefinance.domain.enums.ExpenseOrigin;
import com.voicefinance.domain.enums.ExpenseStatus;
import com.voicefinance.domain.results.FillExpenseResult;
import com.voicefinance.repository.ExpenseRepository;
import com.voicefinance.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.voicefinance.domain.entity.ExpenseFieldConfirmation;
import com.voicefinance.domain.enums.ConfirmationField;
import com.voicefinance.domain.enums.ConfirmationStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@RequiredArgsConstructor
@Slf4j
@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ConfirmableFields confirmableFields;
    private final CurrentUserProvider currentUserProvider;

    @Transactional
    public String startExpense(StartExpenseCommand command) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Money amount = command.amount() != null
                ? new Money(command.amount(), command.currency() != null ? command.currency() : "BRL")
                : null;

        Expense expense = Expense.builder()
                .userId(currentUserId)
                .status(ExpenseStatus.PENDING_CONFIRMATION)
                .origin(ExpenseOrigin.AUDIO)
                .amount(amount)
                .description(command.description())
                .category(command.category())
                .occurredOn(command.OccurredOn())
                .captureId(UUID.randomUUID())
                .build();

        Expense savedExpense = expenseRepository.save(expense);
        log.info("Gasto criado com id={}", savedExpense.getId());

        List<String> missing = savedExpense.missingFields();
        if (missing.isEmpty()) {
            return "Expense recorded with id=" + savedExpense.getId() + ". All required fields are present.";
        }
        return "Expense recorded with id=" + savedExpense.getId() + ", but still missing: " + String.join(", ", missing);
    }

    @Transactional
    public FillExpenseResult fillExpense(FillExpenseCommand cmd) {
        Expense expense = expenseRepository.findById(cmd.expenseId()).orElse(null);
        if (expense == null) return new FillExpenseResult.NotFound(cmd.expenseId());
        if (!expense.isEditable()) return new FillExpenseResult.NotEditable(expense.getStatus());

        expense.fill(cmd);
        expenseRepository.save(expense);

        List<String> missing = confirmableFields.missing(expense);
        return missing.isEmpty() ? new FillExpenseResult.Saved() : new FillExpenseResult.Incomplete(missing);
    }

    @Transactional
    public FillExpenseResult createExpenseFieldConfirmation(CreateConfirmationCommand command) {
        Expense expense = expenseRepository.findById(command.expenseId()).orElse(null);
        if (expense == null) return new FillExpenseResult.NotFound(command.expenseId());
        if (!expense.isEditable()) return new FillExpenseResult.NotEditable(expense.getStatus());

        if (command.missingFields() == null || command.missingFields().isEmpty()) {
            return new FillExpenseResult.InvalidFields(List.of(), "Nenhum campo informado para confirmação.");
        }

        List<String> invalidFields = confirmableFields.isValid(Expense.class, command.missingFields());
        if (!invalidFields.isEmpty()) {
            return new FillExpenseResult.InvalidFields(invalidFields, "Campos não suportados para confirmação.");
        }

        for (String fieldName : command.missingFields()) {
            ExpenseFieldConfirmation confirmation = ExpenseFieldConfirmation.builder()
                    .id(UUID.randomUUID())
                    .question(command.question())
                    .field(ConfirmationField.fromFieldName(fieldName))
                    .status(ConfirmationStatus.PENDING)
                    .build();
            expense.addConfirmation(confirmation);
        }

        expenseRepository.save(expense);
        log.info("Confirmações criadas para gasto id={}: campos={}", command.expenseId(), command.missingFields());

        return new FillExpenseResult.Incomplete(command.missingFields());
    }

    @Transactional
    public FillExpenseResult confirmExpense(ConfirmExpenseCommand command) {
        Expense expense = expenseRepository.findById(command.expenseId()).orElse(null);
        if (expense == null) return new FillExpenseResult.NotFound(command.expenseId());
        if (!expense.isEditable()) return new FillExpenseResult.NotEditable(expense.getStatus());

        expense.confirmExpense();
        expenseRepository.save(expense);
        log.info("Gasto confirmado com id={}", expense.getId());

        return new FillExpenseResult.Confirmed();
    }

    @Transactional
    public FillExpenseResult cancelExpense(CancelExpenseCommand command) {
        Expense expense = expenseRepository.findById(command.expenseId()).orElse(null);
        if (expense == null) return new FillExpenseResult.NotFound(command.expenseId());
        if (!expense.isEditable()) return new FillExpenseResult.NotEditable(expense.getStatus());

        expense.cancelExpense();
        expenseRepository.save(expense);
        log.info("Gasto cancelado com id={}", expense.getId());

        return new FillExpenseResult.Canceled();
    }

    @Transactional(readOnly = true)
    public List<Expense> listExpenses(ListExpensesCommand command) {
        UUID userId = currentUserProvider.getCurrentUserId();
        ListExpensesCommand cmd = command != null ? command : new ListExpensesCommand(null, null, null, null, null, null);

        Specification<Expense> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));

            if (cmd.startDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredOn"), cmd.startDate()));
            }
            if (cmd.endDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredOn"), cmd.endDate()));
            }
            if (cmd.category() != null) {
                predicates.add(cb.equal(root.get("category"), cmd.category()));
            }
            if (cmd.status() != null) {
                predicates.add(cb.equal(root.get("status"), cmd.status()));
            }
            if (cmd.description() != null && !cmd.description().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("description")), "%" + cmd.description().trim().toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        int pageSize = (cmd.limit() != null && cmd.limit() > 0) ? Math.min(cmd.limit(), 50) : 10;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "occurredOn", "createdAt"));

        return expenseRepository.findAll(spec, pageable).getContent();
    }
}

