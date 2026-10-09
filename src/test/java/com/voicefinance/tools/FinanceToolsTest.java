package com.voicefinance.tools;

import com.voicefinance.ai.tools.FinanceTools;
import com.voicefinance.domain.commands.CancelExpenseCommand;
import com.voicefinance.domain.commands.ConfirmExpenseCommand;
import com.voicefinance.domain.commands.FillExpenseCommand;
import com.voicefinance.domain.commands.StartExpenseCommand;
import com.voicefinance.domain.enums.Category;
import com.voicefinance.domain.enums.ExpenseStatus;
import com.voicefinance.domain.results.FillExpenseResult;
import com.voicefinance.service.ExpenseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceToolsTest {

    @Mock
    private ExpenseService expenseService;

    @InjectMocks
    private FinanceTools financeTools;

    @Test
    void testStartExpense() {
        StartExpenseCommand command = new StartExpenseCommand(
                new BigDecimal("45.00"),
                "BRL",
                "Almoço",
                Category.ALIMENTACAO,
                LocalDate.now()
        );
        String expectedMessage = "Expense recorded with id=" + UUID.randomUUID() + ". All required fields are present.";
        when(expenseService.startExpense(command)).thenReturn(expectedMessage);

        String result = financeTools.startExpense(command);

        assertThat(result).isEqualTo(expectedMessage);
        verify(expenseService).startExpense(command);
    }

    @Test
    void testFillExpense_WhenSaved() {
        UUID expenseId = UUID.randomUUID();
        FillExpenseCommand command = new FillExpenseCommand(
                expenseId,
                new BigDecimal("50.00"),
                "BRL",
                "Almoço",
                Category.ALIMENTACAO,
                LocalDate.now()
        );

        when(expenseService.fillExpense(command)).thenReturn(new FillExpenseResult.Saved());

        String result = financeTools.fillExpense(command);

        assertThat(result).isEqualTo("Expense saved");
        verify(expenseService).fillExpense(command);
    }

    @Test
    void testFillExpense_WhenIncomplete() {
        UUID expenseId = UUID.randomUUID();
        FillExpenseCommand command = new FillExpenseCommand(
                expenseId,
                new BigDecimal("50.00"),
                "BRL",
                null,
                null,
                null
        );

        when(expenseService.fillExpense(command)).thenReturn(new FillExpenseResult.Incomplete(List.of("categoria", "occurredOn")));

        String result = financeTools.fillExpense(command);

        assertThat(result).isEqualTo("Expense saved, but still missing: categoria, occurredOn");
        verify(expenseService).fillExpense(command);
    }

    @Test
    void testFillExpense_WhenNotFound() {
        UUID expenseId = UUID.randomUUID();
        FillExpenseCommand command = new FillExpenseCommand(
                expenseId,
                new BigDecimal("100.00"),
                "USD",
                "Grocery shopping",
                Category.MERCADO,
                LocalDate.now()
        );

        when(expenseService.fillExpense(command)).thenReturn(new FillExpenseResult.NotFound(expenseId));

        String result = financeTools.fillExpense(command);

        assertThat(result).isEqualTo("Expense not found: " + expenseId);
        verify(expenseService).fillExpense(command);
    }

    @Test
    void testFillExpense_WhenNotEditable() {
        UUID expenseId = UUID.randomUUID();
        FillExpenseCommand command = new FillExpenseCommand(
                expenseId,
                new BigDecimal("100.00"),
                "BRL",
                "Farmácia",
                Category.SAUDE,
                LocalDate.now()
        );

        when(expenseService.fillExpense(command)).thenReturn(new FillExpenseResult.NotEditable(ExpenseStatus.CONFIRMED));

        String result = financeTools.fillExpense(command);

        assertThat(result).isEqualTo("Expense can no longer be changed, status: CONFIRMED");
        verify(expenseService).fillExpense(command);
    }

    @Test
    void testCreateExpenseConfirmation_WhenIncomplete() {
        UUID expenseId = UUID.randomUUID();
        com.voicefinance.domain.commands.CreateConfirmationCommand command =
                new com.voicefinance.domain.commands.CreateConfirmationCommand(
                        expenseId,
                        List.of("category"),
                        "Qual a categoria do gasto?"
                );

        when(expenseService.createExpenseFieldConfirmation(command))
                .thenReturn(new FillExpenseResult.Incomplete(List.of("category")));

        String result = financeTools.createExpenseFieldConfirmation(command);

        assertThat(result).isEqualTo("Confirmation created for missing fields: category");
        verify(expenseService).createExpenseFieldConfirmation(command);
    }

    @Test
    void testCreateExpenseConfirmation_WhenNotFound() {
        UUID expenseId = UUID.randomUUID();
        com.voicefinance.domain.commands.CreateConfirmationCommand command =
                new com.voicefinance.domain.commands.CreateConfirmationCommand(
                        expenseId,
                        List.of("category"),
                        "Qual a categoria?"
                );

        when(expenseService.createExpenseFieldConfirmation(command))
                .thenReturn(new FillExpenseResult.NotFound(expenseId));

        String result = financeTools.createExpenseFieldConfirmation(command);

        assertThat(result).isEqualTo("Expense not found: " + expenseId);
    }

    @Test
    void testCreateExpenseConfirmation_WhenNotEditable() {
        UUID expenseId = UUID.randomUUID();
        com.voicefinance.domain.commands.CreateConfirmationCommand command =
                new com.voicefinance.domain.commands.CreateConfirmationCommand(
                        expenseId,
                        List.of("category"),
                        "Qual a categoria?"
                );

        when(expenseService.createExpenseFieldConfirmation(command))
                .thenReturn(new FillExpenseResult.NotEditable(ExpenseStatus.CONFIRMED));

        String result = financeTools.createExpenseFieldConfirmation(command);

        assertThat(result).isEqualTo("Expense can no longer be changed, status: CONFIRMED");
    }

    @Test
    void testCreateExpenseConfirmation_WhenIllegalArgumentException() {
        UUID expenseId = UUID.randomUUID();
        com.voicefinance.domain.commands.CreateConfirmationCommand command =
                new com.voicefinance.domain.commands.CreateConfirmationCommand(
                        expenseId,
                        List.of("invalido"),
                        "Qual o valor?"
                );

        when(expenseService.createExpenseFieldConfirmation(command))
                .thenReturn(new FillExpenseResult.InvalidFields(List.of("invalido"), "Campos não suportados para confirmação."));

        String result = financeTools.createExpenseFieldConfirmation(command);

        assertThat(result).isEqualTo("Invalid confirmation fields: invalido");
    }

    @Test
    void testCreateExpenseConfirmation_WhenNullCommand() {
        String result = financeTools.createExpenseFieldConfirmation(null);

        assertThat(result).isEqualTo("Error creating confirmation: command cannot be null");
    }

    @Test
    void testConfirmExpense_WhenConfirmed() {
        UUID expenseId = UUID.randomUUID();
        ConfirmExpenseCommand command = new ConfirmExpenseCommand(expenseId);

        when(expenseService.confirmExpense(command)).thenReturn(new FillExpenseResult.Confirmed());

        String result = financeTools.confirmExpense(command);

        assertThat(result).isEqualTo("Expense confirmed");
        verify(expenseService).confirmExpense(command);
    }

    @Test
    void testConfirmExpense_WhenNotFound() {
        UUID expenseId = UUID.randomUUID();
        ConfirmExpenseCommand command = new ConfirmExpenseCommand(expenseId);

        when(expenseService.confirmExpense(command)).thenReturn(new FillExpenseResult.NotFound(expenseId));

        String result = financeTools.confirmExpense(command);

        assertThat(result).isEqualTo("Expense not found: " + expenseId);
        verify(expenseService).confirmExpense(command);
    }

    @Test
    void testConfirmExpense_WhenNotEditable() {
        UUID expenseId = UUID.randomUUID();
        ConfirmExpenseCommand command = new ConfirmExpenseCommand(expenseId);

        when(expenseService.confirmExpense(command)).thenReturn(new FillExpenseResult.NotEditable(ExpenseStatus.CONFIRMED));

        String result = financeTools.confirmExpense(command);

        assertThat(result).isEqualTo("Expense can no longer be changed, status: CONFIRMED");
        verify(expenseService).confirmExpense(command);
    }

    @Test
    void testConfirmExpense_WhenNullCommand() {
        String result = financeTools.confirmExpense(null);

        assertThat(result).isEqualTo("Error confirming expense: command cannot be null");
    }

    @Test
    void testCancelExpense_WhenCanceled() {
        UUID expenseId = UUID.randomUUID();
        CancelExpenseCommand command = new CancelExpenseCommand(expenseId);

        when(expenseService.cancelExpense(command)).thenReturn(new FillExpenseResult.Canceled());

        String result = financeTools.cancelExpense(command);

        assertThat(result).isEqualTo("Expense canceled");
        verify(expenseService).cancelExpense(command);
    }

    @Test
    void testCancelExpense_WhenNotFound() {
        UUID expenseId = UUID.randomUUID();
        CancelExpenseCommand command = new CancelExpenseCommand(expenseId);

        when(expenseService.cancelExpense(command)).thenReturn(new FillExpenseResult.NotFound(expenseId));

        String result = financeTools.cancelExpense(command);

        assertThat(result).isEqualTo("Expense not found: " + expenseId);
        verify(expenseService).cancelExpense(command);
    }

    @Test
    void testCancelExpense_WhenNotEditable() {
        UUID expenseId = UUID.randomUUID();
        CancelExpenseCommand command = new CancelExpenseCommand(expenseId);

        when(expenseService.cancelExpense(command)).thenReturn(new FillExpenseResult.NotEditable(ExpenseStatus.CANCELED));

        String result = financeTools.cancelExpense(command);

        assertThat(result).isEqualTo("Expense can no longer be changed, status: CANCELED");
        verify(expenseService).cancelExpense(command);
    }

    @Test
    void testCancelExpense_WhenNullCommand() {
        String result = financeTools.cancelExpense(null);

        assertThat(result).isEqualTo("Error canceling expense: command cannot be null");
    }
}