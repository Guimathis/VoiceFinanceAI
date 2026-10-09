package com.voicefinance.domain.entity;

import com.voicefinance.domain.commands.FillExpenseCommand;
import com.voicefinance.domain.enums.Category;
import com.voicefinance.domain.enums.ConfirmationField;
import com.voicefinance.domain.enums.ConfirmationStatus;
import com.voicefinance.domain.enums.ExpenseStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExpenseTest {

    @Test
    void isEditable_ShouldReturnTrueForPendingConfirmation() {
        Expense expense = Expense.builder()
                .status(ExpenseStatus.PENDING_CONFIRMATION)
                .build();

        assertThat(expense.isEditable()).isTrue();
    }

    @Test
    void isEditable_ShouldReturnFalseForConfirmedAndCanceled() {
        Expense confirmed = Expense.builder()
                .status(ExpenseStatus.CONFIRMED)
                .build();
        Expense canceled = Expense.builder()
                .status(ExpenseStatus.CANCELED)
                .build();

        assertThat(confirmed.isEditable()).isFalse();
        assertThat(canceled.isEditable()).isFalse();
    }

    @Test
    void fill_ShouldUpdateFieldsProvidedInCommand() {
        Expense expense = Expense.builder()
                .id(UUID.randomUUID())
                .status(ExpenseStatus.PENDING_CONFIRMATION)
                .build();

        LocalDate date = LocalDate.of(2026, 10, 6);
        FillExpenseCommand cmd = new FillExpenseCommand(
                expense.getId(),
                new BigDecimal("85.50"),
                "BRL",
                "Almoço de negócios",
                Category.ALIMENTACAO,
                date
        );

        expense.fill(cmd);

        assertThat(expense.getAmount()).isNotNull();
        assertThat(expense.getAmount().amount()).isEqualByComparingTo("85.50");
        assertThat(expense.getAmount().currency()).isEqualTo("BRL");
        assertThat(expense.getDescription()).isEqualTo("Almoço de negócios");
        assertThat(expense.getCategory()).isEqualTo(Category.ALIMENTACAO);
        assertThat(expense.getOccurredOn()).isEqualTo(date);
    }

    @Test
    void fill_ShouldPreserveExistingFieldsWhenCommandHasNulls() {
        LocalDate existingDate = LocalDate.of(2026, 10, 1);
        Expense expense = Expense.builder()
                .id(UUID.randomUUID())
                .amount(new Money(new BigDecimal("20.00"), "BRL"))
                .description("Descrição inicial")
                .category(Category.OUTROS)
                .occurredOn(existingDate)
                .status(ExpenseStatus.PENDING_CONFIRMATION)
                .build();

        FillExpenseCommand cmd = new FillExpenseCommand(
                expense.getId(),
                new BigDecimal("45.00"),
                null,
                null,
                null,
                null
        );

        expense.fill(cmd);

        assertThat(expense.getAmount().amount()).isEqualByComparingTo("45.00");
        assertThat(expense.getAmount().currency()).isEqualTo("BRL");
        assertThat(expense.getDescription()).isEqualTo("Descrição inicial");
        assertThat(expense.getCategory()).isEqualTo(Category.OUTROS);
        assertThat(expense.getOccurredOn()).isEqualTo(existingDate);
    }

    @Test
    void confirmExpense_ShouldUpdateStatusAndConfirmedAt() {
        Expense expense = Expense.builder()
                .status(ExpenseStatus.PENDING_CONFIRMATION)
                .build();

        expense.confirmExpense();

        assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.CONFIRMED);
        assertThat(expense.getConfirmedAt()).isNotNull();
    }

    @Test
    void cancelExpense_ShouldUpdateStatusAndResetConfirmedAt() {
        Expense expense = Expense.builder()
                .status(ExpenseStatus.PENDING_CONFIRMATION)
                .confirmedAt(Instant.now())
                .build();

        expense.cancelExpense();

        assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.CANCELED);
        assertThat(expense.getConfirmedAt()).isNull();
    }

    @Test
    void fill_ShouldAutoResolveMatchingPendingConfirmationsAndSetAnswer() {
        Expense expense = Expense.builder()
                .id(UUID.randomUUID())
                .status(ExpenseStatus.PENDING_CONFIRMATION)
                .build();

        ExpenseFieldConfirmation catConf = ExpenseFieldConfirmation.builder()
                .id(UUID.randomUUID())
                .field(ConfirmationField.CATEGORY)
                .status(ConfirmationStatus.PENDING)
                .question("Qual a categoria?")
                .build();

        ExpenseFieldConfirmation dateConf = ExpenseFieldConfirmation.builder()
                .id(UUID.randomUUID())
                .field(ConfirmationField.OCCURRED_ON)
                .status(ConfirmationStatus.PENDING)
                .question("Qual a data?")
                .build();

        expense.addConfirmation(catConf);
        expense.addConfirmation(dateConf);

        FillExpenseCommand cmd = new FillExpenseCommand(
                expense.getId(),
                null,
                null,
                null,
                Category.ALIMENTACAO,
                null
        );

        expense.fill(cmd);

        assertThat(catConf.getStatus()).isEqualTo(ConfirmationStatus.CONFIRMED);
        assertThat(catConf.getAnswer()).isEqualTo("ALIMENTACAO");
        assertThat(dateConf.getStatus()).isEqualTo(ConfirmationStatus.PENDING);
        assertThat(dateConf.getAnswer()).isNull();
    }

    @Test
    void confirmExpense_ShouldResolveRemainingPendingConfirmations() {
        Expense expense = Expense.builder()
                .status(ExpenseStatus.PENDING_CONFIRMATION)
                .build();

        ExpenseFieldConfirmation conf = ExpenseFieldConfirmation.builder()
                .id(UUID.randomUUID())
                .field(ConfirmationField.CATEGORY)
                .status(ConfirmationStatus.PENDING)
                .question("Qual a categoria?")
                .build();

        expense.addConfirmation(conf);

        expense.confirmExpense();

        assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.CONFIRMED);
        assertThat(conf.getStatus()).isEqualTo(ConfirmationStatus.CONFIRMED);
    }

    @Test
    void cancelExpense_ShouldRejectPendingConfirmations() {
        Expense expense = Expense.builder()
                .status(ExpenseStatus.PENDING_CONFIRMATION)
                .build();

        ExpenseFieldConfirmation conf = ExpenseFieldConfirmation.builder()
                .id(UUID.randomUUID())
                .field(ConfirmationField.CATEGORY)
                .status(ConfirmationStatus.PENDING)
                .question("Qual a categoria?")
                .build();

        expense.addConfirmation(conf);

        expense.cancelExpense();

        assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.CANCELED);
        assertThat(conf.getStatus()).isEqualTo(ConfirmationStatus.REJECTED);
    }
}
