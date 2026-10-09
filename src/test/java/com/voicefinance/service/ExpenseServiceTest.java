package com.voicefinance.service;

import com.voicefinance.domain.annotation.ConfirmableFields;
import com.voicefinance.domain.commands.CancelExpenseCommand;
import com.voicefinance.domain.commands.ConfirmExpenseCommand;
import com.voicefinance.domain.commands.CreateConfirmationCommand;
import com.voicefinance.domain.commands.FillExpenseCommand;
import com.voicefinance.domain.commands.ListExpensesCommand;
import com.voicefinance.domain.commands.StartExpenseCommand;
import com.voicefinance.domain.entity.Expense;
import com.voicefinance.domain.entity.ExpenseFieldConfirmation;
import com.voicefinance.domain.enums.Category;
import com.voicefinance.domain.enums.ExpenseOrigin;
import com.voicefinance.domain.enums.ExpenseStatus;
import com.voicefinance.domain.results.FillExpenseResult;
import com.voicefinance.repository.ExpenseRepository;
import com.voicefinance.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private ExpenseService expenseService;


    @Test
    void shouldReturnSaved_WhenExpenseIsSaved() {
        StartExpenseCommand command = new StartExpenseCommand(
                BigDecimal.TEN,
                "BRL",
                "Gasto com comida",
                Category.ALIMENTACAO,
                LocalDate.now()
        );
        UUID userId = UUID.randomUUID();
        UUID generatedExpenseId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
            Expense exp = invocation.getArgument(0);
            exp.setId(generatedExpenseId);
            return exp;
        });

        String result = expenseService.startExpense(command);

        assertThat(result).isEqualTo("Expense recorded with id=" + generatedExpenseId + ". All required fields are present.");

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());

        Expense saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getStatus()).isEqualTo(ExpenseStatus.PENDING_CONFIRMATION);
        assertThat(saved.getOrigin()).isEqualTo(ExpenseOrigin.AUDIO);
        assertThat(saved.getAmount().amount()).isEqualByComparingTo("10.00");
        assertThat(saved.getAmount().currency()).isEqualTo("BRL");
        assertThat(saved.getDescription()).isEqualTo("Gasto com comida");
        assertThat(saved.getCategory()).isEqualTo(Category.ALIMENTACAO);
        assertThat(saved.getOccurredOn()).isEqualTo(command.OccurredOn());
        assertThat(saved.getCaptureId()).isNotNull();
    }

    @Test
    void shouldReturnSaved_WhenExpenseIsSaved_withMissingFields() {
        StartExpenseCommand command = new StartExpenseCommand(
                null,
                "BRL",
                "Gasto sem valor e categoria",
                null,
                null
        );
        UUID userId = UUID.randomUUID();
        UUID generatedExpenseId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
            Expense exp = invocation.getArgument(0);
            exp.setId(generatedExpenseId);
            return exp;
        });

        String result = expenseService.startExpense(command);

        assertThat(result).isEqualTo("Expense recorded with id=" + generatedExpenseId + ", but still missing: amount, category, occurredOnDate");

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());

        Expense saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getAmount()).isNull();
        assertThat(saved.getCategory()).isNull();
        assertThat(saved.getOccurredOn()).isNull();
        assertThat(saved.getDescription()).isEqualTo("Gasto sem valor e categoria");
    }

    @Test
    void shouldDefaultCurrencyToBrl_WhenCurrencyIsNull() {
        StartExpenseCommand command = new StartExpenseCommand(
                new BigDecimal("50.00"),
                null,
                "Café",
                Category.ALIMENTACAO,
                LocalDate.now()
        );
        UUID userId = UUID.randomUUID();
        UUID generatedExpenseId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
            Expense exp = invocation.getArgument(0);
            exp.setId(generatedExpenseId);
            return exp;
        });

        String result = expenseService.startExpense(command);

        assertThat(result).isEqualTo("Expense recorded with id=" + generatedExpenseId + ". All required fields are present.");

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount().currency()).isEqualTo("BRL");
    }

    @Test
    void shouldReturnSaved_WhenOnlyOccurredOnIsMissing() {
        StartExpenseCommand command = new StartExpenseCommand(
                new BigDecimal("30.00"),
                "BRL",
                "Transporte",
                Category.TRANSPORTE,
                null
        );
        UUID userId = UUID.randomUUID();
        UUID generatedExpenseId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
            Expense exp = invocation.getArgument(0);
            exp.setId(generatedExpenseId);
            return exp;
        });

        String result = expenseService.startExpense(command);

        assertThat(result).isEqualTo("Expense recorded with id=" + generatedExpenseId + ", but still missing: occurredOnDate");
    }

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

        FillExpenseResult result = expenseService.fillExpense(cmd);

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

        FillExpenseResult result = expenseService.fillExpense(cmd);

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

        FillExpenseResult result = expenseService.fillExpense(cmd);

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

        FillExpenseResult result = expenseService.fillExpense(cmd);

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

        FillExpenseResult result = expenseService.fillExpense(cmd);

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
                new CreateConfirmationCommand(
                        expenseId,
                        List.of("category"),
                        "Qual a categoria?"
                ));

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
                new CreateConfirmationCommand(
                        expenseId,
                        List.of("category"),
                        "Qual a categoria?"
                ));

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

        assertThat(expenseService.createExpenseFieldConfirmation(new CreateConfirmationCommand(expenseId, List.of(), "Qual a categoria?")))
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

        assertThat(expenseService.createExpenseFieldConfirmation(
                new CreateConfirmationCommand(expenseId, List.of("invalido"), "Qual a categoria?")))
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
                new CreateConfirmationCommand(
                        expenseId,
                        missingFields,
                        "Pode informar a categoria e a data do gasto?"
                ));

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

    @Test
    void confirmExpense_WhenExpenseNotFound_ReturnsNotFound() {
        UUID expenseId = UUID.randomUUID();
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.empty());

        FillExpenseResult result = expenseService.confirmExpense(new ConfirmExpenseCommand(expenseId));

        assertThat(result).isInstanceOf(FillExpenseResult.NotFound.class);
        FillExpenseResult.NotFound notFound = (FillExpenseResult.NotFound) result;
        assertThat(notFound.id()).isEqualTo(expenseId);
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void confirmExpense_WhenExpenseNotEditable_ReturnsNotEditable() {
        UUID expenseId = UUID.randomUUID();
        Expense expense = Expense.builder()
                .id(expenseId)
                .status(ExpenseStatus.CONFIRMED)
                .build();

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));

        FillExpenseResult result = expenseService.confirmExpense(new ConfirmExpenseCommand(expenseId));

        assertThat(result).isInstanceOf(FillExpenseResult.NotEditable.class);
        FillExpenseResult.NotEditable notEditable = (FillExpenseResult.NotEditable) result;
        assertThat(notEditable.status()).isEqualTo(ExpenseStatus.CONFIRMED);
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void confirmExpense_WhenValid_ConfirmsAndSavesExpense() {
        UUID expenseId = UUID.randomUUID();
        Expense expense = Expense.builder()
                .id(expenseId)
                .status(ExpenseStatus.PENDING_CONFIRMATION)
                .build();

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));

        FillExpenseResult result = expenseService.confirmExpense(new ConfirmExpenseCommand(expenseId));

        assertThat(result).isInstanceOf(FillExpenseResult.Confirmed.class);
        assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.CONFIRMED);
        assertThat(expense.getConfirmedAt()).isNotNull();
        verify(expenseRepository).save(expense);
    }

    @Test
    void cancelExpense_WhenExpenseNotFound_ReturnsNotFound() {
        UUID expenseId = UUID.randomUUID();
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.empty());

        FillExpenseResult result = expenseService.cancelExpense(new CancelExpenseCommand(expenseId));

        assertThat(result).isInstanceOf(FillExpenseResult.NotFound.class);
        FillExpenseResult.NotFound notFound = (FillExpenseResult.NotFound) result;
        assertThat(notFound.id()).isEqualTo(expenseId);
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void cancelExpense_WhenExpenseNotEditable_ReturnsNotEditable() {
        UUID expenseId = UUID.randomUUID();
        Expense expense = Expense.builder()
                .id(expenseId)
                .status(ExpenseStatus.CANCELED)
                .build();

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));

        FillExpenseResult result = expenseService.cancelExpense(new CancelExpenseCommand(expenseId));

        assertThat(result).isInstanceOf(FillExpenseResult.NotEditable.class);
        FillExpenseResult.NotEditable notEditable = (FillExpenseResult.NotEditable) result;
        assertThat(notEditable.status()).isEqualTo(ExpenseStatus.CANCELED);
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void cancelExpense_WhenValid_CancelsAndSavesExpense() {
        UUID expenseId = UUID.randomUUID();
        Expense expense = Expense.builder()
                .id(expenseId)
                .status(ExpenseStatus.PENDING_CONFIRMATION)
                .build();

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));

        FillExpenseResult result = expenseService.cancelExpense(new CancelExpenseCommand(expenseId));

        assertThat(result).isInstanceOf(FillExpenseResult.Canceled.class);
        assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.CANCELED);
        assertThat(expense.getConfirmedAt()).isNull();
        verify(expenseRepository).save(expense);
    }

    @Test
    void fillExpense_ShouldResolvePendingConfirmation_WhenMatchingFieldIsFilled() {
        UUID expenseId = UUID.randomUUID();
        Expense expense = Expense.builder()
                .id(expenseId)
                .status(ExpenseStatus.PENDING_CONFIRMATION)
                .build();

        ExpenseFieldConfirmation confirmation = ExpenseFieldConfirmation.builder()
                .id(UUID.randomUUID())
                .field(com.voicefinance.domain.enums.ConfirmationField.CATEGORY)
                .status(com.voicefinance.domain.enums.ConfirmationStatus.PENDING)
                .question("Qual a categoria?")
                .build();
        expense.addConfirmation(confirmation);

        FillExpenseCommand cmd = new FillExpenseCommand(
                expenseId,
                null,
                null,
                null,
                Category.TRANSPORTE,
                null
        );

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));
        when(confirmableFields.missing(expense)).thenReturn(List.of());

        FillExpenseResult result = expenseService.fillExpense(cmd);

        assertThat(result).isInstanceOf(FillExpenseResult.Saved.class);
        assertThat(confirmation.getStatus()).isEqualTo(com.voicefinance.domain.enums.ConfirmationStatus.CONFIRMED);
        assertThat(confirmation.getAnswer()).isEqualTo("TRANSPORTE");
        verify(expenseRepository).save(expense);
    }

    @Test
    void cancelExpense_ShouldRejectPendingConfirmations() {
        UUID expenseId = UUID.randomUUID();
        Expense expense = Expense.builder()
                .id(expenseId)
                .status(ExpenseStatus.PENDING_CONFIRMATION)
                .build();

        ExpenseFieldConfirmation confirmation = ExpenseFieldConfirmation.builder()
                .id(UUID.randomUUID())
                .field(com.voicefinance.domain.enums.ConfirmationField.CATEGORY)
                .status(com.voicefinance.domain.enums.ConfirmationStatus.PENDING)
                .question("Qual a categoria?")
                .build();
        expense.addConfirmation(confirmation);

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));

        FillExpenseResult result = expenseService.cancelExpense(new CancelExpenseCommand(expenseId));

        assertThat(result).isInstanceOf(FillExpenseResult.Canceled.class);
        assertThat(confirmation.getStatus()).isEqualTo(com.voicefinance.domain.enums.ConfirmationStatus.REJECTED);
        verify(expenseRepository).save(expense);
    }

    @Test
    void listExpenses_ShouldFilterByCurrentUserAndLimit() {
        UUID userId = UUID.randomUUID();
        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);

        ListExpensesCommand command = new ListExpensesCommand(
                LocalDate.now().minusDays(5),
                LocalDate.now(),
                Category.ALIMENTACAO,
                ExpenseStatus.CONFIRMED,
                "Restaurante",
                15
        );

        Expense exp = Expense.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .category(Category.ALIMENTACAO)
                .build();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(expenseRepository.findAll(any(Specification.class), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of(exp)));

        List<Expense> result = expenseService.listExpenses(command);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(exp.getId());

        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getPageSize()).isEqualTo(15);
    }

    @Test
    void listExpenses_WhenCommandIsNull_UsesDefaultPageSize() {
        UUID userId = UUID.randomUUID();
        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(expenseRepository.findAll(any(Specification.class), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of()));

        List<Expense> result = expenseService.listExpenses(null);

        assertThat(result).isEmpty();

        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getPageSize()).isEqualTo(10);
    }
}
