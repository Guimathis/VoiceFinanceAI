package com.voicefinance.service;

import com.voicefinance.domain.ConfirmableFields;
import com.voicefinance.domain.commands.FillExpenseCommand;
import com.voicefinance.domain.entity.Expense;
import com.voicefinance.domain.enums.Category;
import com.voicefinance.domain.enums.ExpenseStatus;
import com.voicefinance.domain.results.FillExpenseResult;
import com.voicefinance.repository.ExpenseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ConfirmableFields confirmableFields;

    @InjectMocks
    private ExpenseService expenseService;

    @Test
    void shouldReturnNotFound_WhenExpenseDoesNotExist() {
        UUID expenseId = UUID.randomUUID();
        FillExpenseCommand cmd = new FillExpenseCommand(
                expenseId,
                new BigDecimal("100.00"),
                "BRL",
                "Almoço",
                Category.ALIMENTACAO,
                LocalDate.now()
        );

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.empty());

        FillExpenseResult result = expenseService.fill(cmd);

        assertThat(result).isInstanceOf(FillExpenseResult.NotFound.class);
        FillExpenseResult.NotFound notFound = (FillExpenseResult.NotFound) result;
        assertThat(notFound.id()).isEqualTo(expenseId);

        verify(expenseRepository, never()).save(any());
        verify(confirmableFields, never()).missing(any());
    }

    @Test
    void shouldReturnNotEditable_WhenExpenseStatusIsConfirmed() {
        UUID expenseId = UUID.randomUUID();
        FillExpenseCommand cmd = new FillExpenseCommand(
                expenseId,
                new BigDecimal("100.00"),
                "BRL",
                "Almoço",
                Category.ALIMENTACAO,
                LocalDate.now()
        );

        Expense expense = Expense.builder()
                .id(expenseId)
                .status(ExpenseStatus.CONFIRMED)
                .build();

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));

        FillExpenseResult result = expenseService.fill(cmd);

        assertThat(result).isInstanceOf(FillExpenseResult.NotEditable.class);
        FillExpenseResult.NotEditable notEditable = (FillExpenseResult.NotEditable) result;
        assertThat(notEditable.status()).isEqualTo(ExpenseStatus.CONFIRMED);

        verify(expenseRepository, never()).save(any());
        verify(confirmableFields, never()).missing(any());
    }

    @Test
    void shouldReturnNotEditable_WhenExpenseStatusIsCanceled() {
        UUID expenseId = UUID.randomUUID();
        FillExpenseCommand cmd = new FillExpenseCommand(
                expenseId,
                new BigDecimal("100.00"),
                "BRL",
                "Cancelado",
                Category.OUTROS,
                LocalDate.now()
        );

        Expense expense = Expense.builder()
                .id(expenseId)
                .status(ExpenseStatus.CANCELED)
                .build();

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));

        FillExpenseResult result = expenseService.fill(cmd);

        assertThat(result).isInstanceOf(FillExpenseResult.NotEditable.class);
        FillExpenseResult.NotEditable notEditable = (FillExpenseResult.NotEditable) result;
        assertThat(notEditable.status()).isEqualTo(ExpenseStatus.CANCELED);

        verify(expenseRepository, never()).save(any());
    }

    @Test
    void shouldSaveAndReturnSaved_WhenAllRequiredFieldsAreFilled() {
        UUID expenseId = UUID.randomUUID();
        FillExpenseCommand cmd = new FillExpenseCommand(
                expenseId,
                new BigDecimal("150.00"),
                "BRL",
                "Supermercado",
                Category.MERCADO,
                LocalDate.of(2026, 10, 6)
        );

        Expense expense = Expense.builder()
                .id(expenseId)
                .status(ExpenseStatus.PENDING_CONFIRMATION)
                .build();

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));
        when(confirmableFields.missing(expense)).thenReturn(List.of());

        FillExpenseResult result = expenseService.fill(cmd);

        assertThat(result).isInstanceOf(FillExpenseResult.Saved.class);
        verify(expenseRepository).save(expense);
        verify(confirmableFields).missing(expense);

        assertThat(expense.getAmount().amount()).isEqualByComparingTo("150.00");
        assertThat(expense.getCategory()).isEqualTo(Category.MERCADO);
        assertThat(expense.getDescription()).isEqualTo("Supermercado");
        assertThat(expense.getOccurredOn()).isEqualTo(LocalDate.of(2026, 10, 6));
    }

    @Test
    void shouldSaveAndReturnIncomplete_WhenRequiredFieldsAreMissing() {
        UUID expenseId = UUID.randomUUID();
        FillExpenseCommand cmd = new FillExpenseCommand(
                expenseId,
                new BigDecimal("50.00"),
                "BRL",
                null,
                null,
                null
        );

        Expense expense = Expense.builder()
                .id(expenseId)
                .status(ExpenseStatus.PENDING_CONFIRMATION)
                .build();

        List<String> missing = List.of("categoria", "data do gasto");
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));
        when(confirmableFields.missing(expense)).thenReturn(missing);

        FillExpenseResult result = expenseService.fill(cmd);

        assertThat(result).isInstanceOf(FillExpenseResult.Incomplete.class);
        FillExpenseResult.Incomplete incomplete = (FillExpenseResult.Incomplete) result;
        assertThat(incomplete.missing()).containsExactlyElementsOf(missing);

        verify(expenseRepository).save(expense);
    }

    @Test
    void createExpenseFieldConfirmation_WhenExpenseNotFound_ReturnsNotFound() {
        UUID expenseId = UUID.randomUUID();
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.empty());

        FillExpenseResult result = expenseService.createExpenseFieldConfirmation(
                List.of("category"),
                expenseId,
                "Qual a categoria?"
        );

        assertThat(result).isInstanceOf(FillExpenseResult.NotFound.class);
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void createExpenseFieldConfirmation_WhenExpenseNotEditable_ReturnsNotEditable() {
        UUID expenseId = UUID.randomUUID();
        Expense expense = Expense.builder()
                .id(expenseId)
                .status(ExpenseStatus.CONFIRMED)
                .build();

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));

        FillExpenseResult result = expenseService.createExpenseFieldConfirmation(
                List.of("category"),
                expenseId,
                "Qual a categoria?"
        );

        assertThat(result).isInstanceOf(FillExpenseResult.NotEditable.class);
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void createExpenseFieldConfirmation_WhenFieldsEmpty_ThrowsException() {
        UUID expenseId = UUID.randomUUID();
        Expense expense = Expense.builder()
                .id(expenseId)
                .status(ExpenseStatus.PENDING_CONFIRMATION)
                .build();

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));

        assertThat(expenseService.createExpenseFieldConfirmation(List.of(), expenseId, "Qual a categoria?"))
                .isInstanceOf(FillExpenseResult.InvalidFields.class);
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void createExpenseFieldConfirmation_WhenFieldsInvalid_ThrowsException() {
        UUID expenseId = UUID.randomUUID();
        Expense expense = Expense.builder()
                .id(expenseId)
                .status(ExpenseStatus.PENDING_CONFIRMATION)
                .build();

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));
        when(confirmableFields.isValid(Expense.class, List.of("invalido"))).thenReturn(List.of("invalido"));

        assertThat(expenseService.createExpenseFieldConfirmation(List.of("invalido"), expenseId, "Qual a categoria?"))
                .isInstanceOf(FillExpenseResult.InvalidFields.class);

        verify(expenseRepository, never()).save(any());
    }

    @Test
    void createExpenseFieldConfirmation_WhenValid_AddsConfirmationsAndSavesExpense() {
        UUID expenseId = UUID.randomUUID();
        Expense expense = Expense.builder()
                .id(expenseId)
                .status(ExpenseStatus.PENDING_CONFIRMATION)
                .build();

        List<String> missingFields = List.of("category", "occurredOn");
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));
        when(confirmableFields.isValid(Expense.class, missingFields)).thenReturn(List.of());

        FillExpenseResult result = expenseService.createExpenseFieldConfirmation(
                missingFields,
                expenseId,
                "Pode informar a categoria e a data do gasto?"
        );

        assertThat(result).isInstanceOf(FillExpenseResult.Incomplete.class);
        FillExpenseResult.Incomplete incomplete = (FillExpenseResult.Incomplete) result;
        assertThat(incomplete.missing()).containsExactlyElementsOf(missingFields);

        verify(expenseRepository).save(expense);
        assertThat(expense.getExpenseConfirmations()).hasSize(2);

        var first = expense.getExpenseConfirmations().get(0);
        assertThat(first.getQuestion()).isEqualTo("Pode informar a categoria e a data do gasto?");
        assertThat(first.getField()).isEqualTo(com.voicefinance.domain.enums.ConfirmationField.CATEGORY);
        assertThat(first.getStatus()).isEqualTo(com.voicefinance.domain.enums.ConfirmationStatus.PENDING);
        assertThat(first.getExpense()).isEqualTo(expense);

        var second = expense.getExpenseConfirmations().get(1);
        assertThat(second.getField()).isEqualTo(com.voicefinance.domain.enums.ConfirmationField.OCCURRED_ON);
        assertThat(second.getStatus()).isEqualTo(com.voicefinance.domain.enums.ConfirmationStatus.PENDING);
    }
}
