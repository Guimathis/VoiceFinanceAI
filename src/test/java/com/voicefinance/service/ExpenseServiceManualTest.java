package com.voicefinance.service;

import com.voicefinance.domain.dto.CreateExpenseRequest;
import com.voicefinance.domain.dto.ExpenseResponse;
import com.voicefinance.domain.dto.ExpenseSummaryResponse;
import com.voicefinance.domain.dto.UpdateExpenseRequest;
import com.voicefinance.domain.entity.Expense;
import com.voicefinance.domain.entity.Money;
import com.voicefinance.domain.enums.Category;
import com.voicefinance.domain.enums.ExpenseOrigin;
import com.voicefinance.domain.enums.ExpenseStatus;
import com.voicefinance.exception.ExpenseNotEditableException;
import com.voicefinance.exception.ExpenseNotFoundException;
import com.voicefinance.repository.ExpenseRepository;
import com.voicefinance.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceManualTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private ExpenseService expenseService;

    private UUID userId;
    private UUID expenseId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        expenseId = UUID.randomUUID();
        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
    }

    @Test
    @DisplayName("Deve criar despesa manual com status CONFIRMED e origem MANUAL")
    void shouldCreateManualExpenseSuccessfully() {
        CreateExpenseRequest request = new CreateExpenseRequest(
                new BigDecimal("55.00"),
                "BRL",
                Category.ALIMENTACAO,
                "Almoço executivo",
                LocalDate.of(2026, 10, 9)
        );

        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
            Expense toSave = invocation.getArgument(0);
            toSave.setId(expenseId);
            return toSave;
        });

        ExpenseResponse response = expenseService.createManualExpense(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(expenseId);
        assertThat(response.amount()).isEqualByComparingTo("55.00");
        assertThat(response.currency()).isEqualTo("BRL");
        assertThat(response.category()).isEqualTo(Category.ALIMENTACAO);
        assertThat(response.status()).isEqualTo(ExpenseStatus.CONFIRMED);
        assertThat(response.origin()).isEqualTo(ExpenseOrigin.MANUAL);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        Expense captured = captor.getValue();
        assertThat(captured.getUserId()).isEqualTo(userId);
        assertThat(captured.getStatus()).isEqualTo(ExpenseStatus.CONFIRMED);
        assertThat(captured.getOrigin()).isEqualTo(ExpenseOrigin.MANUAL);
    }

    @Test
    @DisplayName("Deve buscar despesa por id com sucesso")
    void shouldGetExpenseByIdSuccessfully() {
        Expense expense = Expense.builder()
                .id(expenseId)
                .userId(userId)
                .amount(new Money(new BigDecimal("120.00"), "BRL"))
                .category(Category.MERCADO)
                .status(ExpenseStatus.CONFIRMED)
                .origin(ExpenseOrigin.MANUAL)
                .build();

        when(expenseRepository.findByIdAndUserId(expenseId, userId)).thenReturn(Optional.of(expense));

        ExpenseResponse response = expenseService.getExpenseById(expenseId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(expenseId);
        assertThat(response.amount()).isEqualByComparingTo("120.00");
        assertThat(response.category()).isEqualTo(Category.MERCADO);
    }

    @Test
    @DisplayName("Deve lançar ExpenseNotFoundException quando despesa não for encontrada")
    void shouldThrowWhenExpenseNotFound() {
        when(expenseRepository.findByIdAndUserId(expenseId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.getExpenseById(expenseId))
                .isInstanceOf(ExpenseNotFoundException.class)
                .hasMessageContaining(expenseId.toString());
    }

    @Test
    @DisplayName("Deve atualizar campos de despesa em estado editável")
    void shouldUpdateExpenseSuccessfully() {
        Expense expense = Expense.builder()
                .id(expenseId)
                .userId(userId)
                .amount(new Money(new BigDecimal("50.00"), "BRL"))
                .category(Category.TRANSPORTE)
                .status(ExpenseStatus.PENDING_CONFIRMATION)
                .origin(ExpenseOrigin.AUDIO)
                .build();

        when(expenseRepository.findByIdAndUserId(expenseId, userId)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);

        UpdateExpenseRequest updateRequest = new UpdateExpenseRequest(
                new BigDecimal("60.00"),
                "BRL",
                Category.LAZER,
                "Cinema atualizado",
                LocalDate.of(2026, 10, 10)
        );

        ExpenseResponse response = expenseService.updateExpense(expenseId, updateRequest);

        assertThat(response).isNotNull();
        assertThat(expense.getAmount().amount()).isEqualByComparingTo("60.00");
        assertThat(expense.getCategory()).isEqualTo(Category.LAZER);
        assertThat(expense.getDescription()).isEqualTo("Cinema atualizado");
    }

    @Test
    @DisplayName("Deve lançar ExpenseNotEditableException ao tentar atualizar despesa confirmada")
    void shouldThrowWhenUpdatingConfirmedExpense() {
        Expense expense = Expense.builder()
                .id(expenseId)
                .userId(userId)
                .amount(new Money(new BigDecimal("50.00"), "BRL"))
                .category(Category.TRANSPORTE)
                .status(ExpenseStatus.CONFIRMED)
                .origin(ExpenseOrigin.MANUAL)
                .build();

        when(expenseRepository.findByIdAndUserId(expenseId, userId)).thenReturn(Optional.of(expense));

        UpdateExpenseRequest updateRequest = new UpdateExpenseRequest(
                new BigDecimal("70.00"), null, null, null, null
        );

        assertThatThrownBy(() -> expenseService.updateExpense(expenseId, updateRequest))
                .isInstanceOf(ExpenseNotEditableException.class);
    }

    @Test
    @DisplayName("Deve confirmar despesa pendente diretamente")
    void shouldConfirmExpenseDirectSuccessfully() {
        Expense expense = Expense.builder()
                .id(expenseId)
                .userId(userId)
                .amount(new Money(new BigDecimal("80.00"), "BRL"))
                .category(Category.SAUDE)
                .status(ExpenseStatus.PENDING_CONFIRMATION)
                .origin(ExpenseOrigin.AUDIO)
                .build();

        when(expenseRepository.findByIdAndUserId(expenseId, userId)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);

        ExpenseResponse response = expenseService.confirmExpenseDirect(expenseId);

        assertThat(response).isNotNull();
        assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.CONFIRMED);
        assertThat(expense.getConfirmedAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve cancelar despesa pendente diretamente")
    void shouldCancelExpenseDirectSuccessfully() {
        Expense expense = Expense.builder()
                .id(expenseId)
                .userId(userId)
                .amount(new Money(new BigDecimal("80.00"), "BRL"))
                .category(Category.SAUDE)
                .status(ExpenseStatus.PENDING_CONFIRMATION)
                .origin(ExpenseOrigin.AUDIO)
                .build();

        when(expenseRepository.findByIdAndUserId(expenseId, userId)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);

        ExpenseResponse response = expenseService.cancelExpenseDirect(expenseId);

        assertThat(response).isNotNull();
        assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.CANCELED);
    }

    @Test
    @DisplayName("Deve excluir despesa do banco de dados")
    void shouldDeleteExpenseSuccessfully() {
        Expense expense = Expense.builder()
                .id(expenseId)
                .userId(userId)
                .build();

        when(expenseRepository.findByIdAndUserId(expenseId, userId)).thenReturn(Optional.of(expense));

        expenseService.deleteExpense(expenseId);

        verify(expenseRepository).delete(expense);
    }

    @Test
    @DisplayName("Deve calcular resumo financeiro com somas e percentuais por categoria")
    void shouldCalculateSummaryCorrectly() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 31);

        Expense exp1 = Expense.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .amount(new Money(new BigDecimal("100.00"), "BRL"))
                .category(Category.ALIMENTACAO)
                .status(ExpenseStatus.CONFIRMED)
                .occurredOn(LocalDate.of(2026, 10, 5))
                .build();

        Expense exp2 = Expense.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .amount(new Money(new BigDecimal("100.00"), "BRL"))
                .category(Category.TRANSPORTE)
                .status(ExpenseStatus.CONFIRMED)
                .occurredOn(LocalDate.of(2026, 10, 6))
                .build();

        Expense exp3 = Expense.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .amount(new Money(new BigDecimal("50.00"), "BRL"))
                .category(Category.MERCADO)
                .status(ExpenseStatus.PENDING_CONFIRMATION)
                .occurredOn(LocalDate.of(2026, 10, 7))
                .build();

        Expense exp4 = Expense.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .amount(new Money(new BigDecimal("30.00"), "BRL"))
                .category(Category.LAZER)
                .status(ExpenseStatus.CANCELED)
                .occurredOn(LocalDate.of(2026, 10, 8))
                .build();

        when(expenseRepository.findByUserIdAndOccurredOnBetween(userId, start, end))
                .thenReturn(List.of(exp1, exp2, exp3, exp4));

        ExpenseSummaryResponse summary = expenseService.getExpenseSummary(start, end);

        assertThat(summary).isNotNull();
        assertThat(summary.totalSpent()).isEqualByComparingTo("200.00");
        assertThat(summary.totalPending()).isEqualByComparingTo("50.00");
        assertThat(summary.totalExpensesCount()).isEqualTo(4);
        assertThat(summary.confirmedCount()).isEqualTo(2);
        assertThat(summary.pendingCount()).isEqualTo(1);
        assertThat(summary.canceledCount()).isEqualTo(1);

        assertThat(summary.categoryBreakdown()).hasSize(2);
        // Each category has 100.00 out of 200.00, so 50%
        assertThat(summary.categoryBreakdown().get(0).percentage()).isEqualTo(50.0);
        assertThat(summary.categoryBreakdown().get(1).percentage()).isEqualTo(50.0);
    }
}
