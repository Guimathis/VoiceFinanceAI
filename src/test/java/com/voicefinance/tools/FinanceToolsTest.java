package com.voicefinance.tools;

import com.voicefinance.domain.commands.FillExpenseCommand;
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

        when(expenseService.fill(command)).thenReturn(new FillExpenseResult.Saved());

        String result = financeTools.fillExpense(command);

        assertThat(result).isEqualTo("Expense saved");
        verify(expenseService).fill(command);
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

        when(expenseService.fill(command)).thenReturn(new FillExpenseResult.Incomplete(List.of("categoria", "occurredOn")));

        String result = financeTools.fillExpense(command);

        assertThat(result).isEqualTo("Expense saved, but still missing: categoria, occurredOn");
        verify(expenseService).fill(command);
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

        when(expenseService.fill(command)).thenReturn(new FillExpenseResult.NotFound(expenseId));

        String result = financeTools.fillExpense(command);

        assertThat(result).isEqualTo("Expense not found: " + expenseId);
        verify(expenseService).fill(command);
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

        when(expenseService.fill(command)).thenReturn(new FillExpenseResult.NotEditable(ExpenseStatus.CONFIRMED));

        String result = financeTools.fillExpense(command);

        assertThat(result).isEqualTo("Expense can no longer be changed, status: CONFIRMED");
        verify(expenseService).fill(command);
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

        when(expenseService.createExpenseFieldConfirmation(List.of("category"), expenseId, "Qual a categoria do gasto?"))
                .thenReturn(new FillExpenseResult.Incomplete(List.of("category")));

        String result = financeTools.createExpenseFieldConfirmation(command);

        assertThat(result).isEqualTo("Confirmation created for missing fields: category");
        verify(expenseService).createExpenseFieldConfirmation(List.of("category"), expenseId, "Qual a categoria do gasto?");
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

        when(expenseService.createExpenseFieldConfirmation(List.of("category"), expenseId, "Qual a categoria?"))
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

        when(expenseService.createExpenseFieldConfirmation(List.of("category"), expenseId, "Qual a categoria?"))
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

        when(expenseService.createExpenseFieldConfirmation(command.missingFields(), expenseId, command.question()))
                .thenReturn(new FillExpenseResult.InvalidFields(List.of("invalido"), "Campos não suportados para confirmação."));

        String result = financeTools.createExpenseFieldConfirmation(command);

        assertThat(result).isEqualTo("Invalid confirmation fields: invalido");
    }

    @Test
    void testCreateExpenseConfirmation_WhenNullCommand() {
        String result = financeTools.createExpenseFieldConfirmation(null);

        assertThat(result).isEqualTo("Error creating confirmation: command cannot be null");
    }
}