package com.voicefinance.service;

import com.voicefinance.context.CaptureContextHolder;
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
import com.voicefinance.domain.dto.CreateExpenseRequest;
import com.voicefinance.domain.dto.UpdateExpenseRequest;
import com.voicefinance.domain.dto.ExpenseResponse;
import com.voicefinance.domain.dto.ExpenseSummaryResponse;
import com.voicefinance.exception.ExpenseNotFoundException;
import com.voicefinance.exception.ExpenseNotEditableException;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
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

        UUID captureId = CaptureContextHolder.getCaptureId();
        ExpenseOrigin origin = CaptureContextHolder.getOrigin();

        Expense expense = Expense.builder()
                .userId(currentUserId)
                .status(ExpenseStatus.PENDING_CONFIRMATION)
                .origin(origin)
                .amount(amount)
                .description(command.description())
                .category(command.category())
                .occurredOn(command.OccurredOn())
                .captureId(captureId)
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

    @Transactional
    public ExpenseResponse createManualExpense(CreateExpenseRequest request) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        String currency = (request.currency() != null && !request.currency().isBlank())
                ? request.currency() : "BRL";
        Money money = new Money(request.amount(), currency);
        LocalDate occurredOn = request.occurredOn() != null ? request.occurredOn() : LocalDate.now();

        Expense expense = Expense.builder()
                .userId(currentUserId)
                .amount(money)
                .category(request.category())
                .description(request.description())
                .occurredOn(occurredOn)
                .status(ExpenseStatus.CONFIRMED)
                .origin(ExpenseOrigin.MANUAL)
                .captureId(null)
                .confirmedAt(Instant.now())
                .build();

        Expense saved = expenseRepository.save(expense);
        log.info("Despesa manual criada com id={}", saved.getId());
        return ExpenseResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(UUID id) {
        UUID userId = currentUserProvider.getCurrentUserId();
        Expense expense = expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ExpenseNotFoundException(id));
        return ExpenseResponse.from(expense);
    }

    @Transactional
    public ExpenseResponse updateExpense(UUID id, UpdateExpenseRequest request) {
        UUID userId = currentUserProvider.getCurrentUserId();
        Expense expense = expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ExpenseNotFoundException(id));

        if (!expense.isEditable()) {
            throw new ExpenseNotEditableException(expense.getStatus());
        }

        expense.update(request);
        Expense saved = expenseRepository.save(expense);
        log.info("Despesa atualizada com id={}", saved.getId());
        return ExpenseResponse.from(saved);
    }

    @Transactional
    public ExpenseResponse confirmExpenseDirect(UUID id) {
        UUID userId = currentUserProvider.getCurrentUserId();
        Expense expense = expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ExpenseNotFoundException(id));

        if (!expense.isEditable()) {
            throw new ExpenseNotEditableException(expense.getStatus());
        }

        expense.confirmExpense();
        Expense saved = expenseRepository.save(expense);
        log.info("Despesa confirmada diretamente com id={}", saved.getId());
        return ExpenseResponse.from(saved);
    }

    @Transactional
    public ExpenseResponse cancelExpenseDirect(UUID id) {
        UUID userId = currentUserProvider.getCurrentUserId();
        Expense expense = expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ExpenseNotFoundException(id));

        if (!expense.isEditable()) {
            throw new ExpenseNotEditableException(expense.getStatus());
        }

        expense.cancelExpense();
        Expense saved = expenseRepository.save(expense);
        log.info("Despesa cancelada diretamente com id={}", saved.getId());
        return ExpenseResponse.from(saved);
    }

    @Transactional
    public void deleteExpense(UUID id) {
        UUID userId = currentUserProvider.getCurrentUserId();
        Expense expense = expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ExpenseNotFoundException(id));

        expenseRepository.delete(expense);
        log.info("Despesa excluída com id={}", id);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ExpenseResponse> findExpensesPaginated(ListExpensesCommand command, Pageable pageable) {
        UUID userId = currentUserProvider.getCurrentUserId();
        ListExpensesCommand cmd = command != null ? command : new ListExpensesCommand(null, null, null, null, null, null);

        Specification<Expense> spec = buildSpecification(userId, cmd);
        return expenseRepository.findAll(spec, pageable).map(ExpenseResponse::from);
    }

    @Transactional(readOnly = true)
    public ExpenseSummaryResponse getExpenseSummary(LocalDate startDate, LocalDate endDate) {
        UUID userId = currentUserProvider.getCurrentUserId();
        LocalDate start = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        List<Expense> expenses = expenseRepository.findByUserIdAndOccurredOnBetween(userId, start, end);

        java.math.BigDecimal totalSpent = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalPending = java.math.BigDecimal.ZERO;
        long confirmedCount = 0;
        long pendingCount = 0;
        long canceledCount = 0;

        java.util.Map<com.voicefinance.domain.enums.Category, java.math.BigDecimal> categorySums = new java.util.EnumMap<>(com.voicefinance.domain.enums.Category.class);
        java.util.Map<com.voicefinance.domain.enums.Category, Long> categoryCounts = new java.util.EnumMap<>(com.voicefinance.domain.enums.Category.class);

        for (Expense exp : expenses) {
            java.math.BigDecimal amt = (exp.getAmount() != null && exp.getAmount().amount() != null)
                    ? exp.getAmount().amount()
                    : java.math.BigDecimal.ZERO;

            if (exp.getStatus() == ExpenseStatus.CONFIRMED) {
                confirmedCount++;
                totalSpent = totalSpent.add(amt);
                if (exp.getCategory() != null) {
                    categorySums.merge(exp.getCategory(), amt, java.math.BigDecimal::add);
                    categoryCounts.merge(exp.getCategory(), 1L, Long::sum);
                }
            } else if (exp.getStatus() == ExpenseStatus.PENDING_CONFIRMATION) {
                pendingCount++;
                totalPending = totalPending.add(amt);
            } else if (exp.getStatus() == ExpenseStatus.CANCELED) {
                canceledCount++;
            }
        }

        long totalExpensesCount = expenses.size();
        final java.math.BigDecimal finalTotalSpent = totalSpent;

        List<ExpenseSummaryResponse.CategorySummary> breakdown = categorySums.entrySet().stream()
                .map(entry -> {
                    com.voicefinance.domain.enums.Category cat = entry.getKey();
                    java.math.BigDecimal total = entry.getValue();
                    long count = categoryCounts.getOrDefault(cat, 0L);
                    double pct = finalTotalSpent.compareTo(java.math.BigDecimal.ZERO) > 0
                            ? total.divide(finalTotalSpent, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100.0
                            : 0.0;
                    return new ExpenseSummaryResponse.CategorySummary(cat, total, count, Math.round(pct * 100.0) / 100.0);
                })
                .sorted((a, b) -> b.total().compareTo(a.total()))
                .toList();

        return new ExpenseSummaryResponse(
                totalSpent,
                totalPending,
                totalExpensesCount,
                confirmedCount,
                pendingCount,
                canceledCount,
                breakdown
        );
    }

    @Transactional(readOnly = true)
    public List<Expense> listExpenses(ListExpensesCommand command) {
        UUID userId = currentUserProvider.getCurrentUserId();
        ListExpensesCommand cmd = command != null ? command : new ListExpensesCommand(null, null, null, null, null, null);

        Specification<Expense> spec = buildSpecification(userId, cmd);

        int pageSize = (cmd.limit() != null && cmd.limit() > 0) ? Math.min(cmd.limit(), 50) : 10;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "occurredOn", "createdAt"));

        return expenseRepository.findAll(spec, pageable).getContent();
    }

    private Specification<Expense> buildSpecification(UUID userId, ListExpensesCommand cmd) {
        return (root, query, cb) -> {
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
    }
}

