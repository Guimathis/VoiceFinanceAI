package com.voicefinance.ai.tools;

import com.voicefinance.domain.commands.CancelExpenseCommand;
import com.voicefinance.domain.commands.ConfirmExpenseCommand;
import com.voicefinance.domain.results.FillExpenseResult;
import com.voicefinance.service.ExpenseService;
import com.voicefinance.domain.commands.CreateConfirmationCommand;
import com.voicefinance.domain.commands.FillExpenseCommand;
import com.voicefinance.domain.commands.StartExpenseCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;


// TODO - Implement recurring expenses
@Slf4j
@RequiredArgsConstructor
@Component
public class FinanceTools {

    private final ExpenseService expenseService;

    @Tool(description = "Create a new Expense and save it to the database. Returns the id of the new created expense and the missing fields.")
    public String startExpense(StartExpenseCommand command) {
        if (command == null) {
            return "Error creating confirmation: command cannot be null";
        }

        String result = expenseService.startExpense(command);
        log.info("Retorno obtido no salvamento de expense: {}", result);
        return result;
    }

    @Tool(description = "Fill in the details of an existing expense and save it to the database.")
    public String fillExpense(
            FillExpenseCommand command
    ) {
        if (command == null) {
            return "Error creating confirmation: command cannot be null";
        }

        var result = expenseService.fillExpense(command);
        String present = present(result);

        log.info("Retorno obtido no salvameto de expense para Expendeid = {} : {} ", command.expenseId(), present);

        return present;
    }

    @Tool(description = "Save a confirmation question for missing fields of an expense so the user can be asked.")
    public String createExpenseFieldConfirmation(
            CreateConfirmationCommand command
    ) {
        if (command == null) {
            return "Error creating confirmation: command cannot be null";
        }

        log.info("Criando confirmação para gasto id={}: campos={}", command.expenseId(), command.missingFields());

        FillExpenseResult result = expenseService.createExpenseFieldConfirmation(command);
        String returnConfirmation = presentConfirmation(result);

        log.info("Retorno obtido na criação de confirmação para Expendeid = {} : {} ", command.expenseId(), returnConfirmation);

        return returnConfirmation;
    }

    @Tool(description = "Confirm an expense.")
    public String confirmExpense(
            ConfirmExpenseCommand command
    ) {
        if (command == null) {
            return "Error confirming expense: command cannot be null";
        }

        FillExpenseResult result = expenseService.confirmExpense(command);
        String present = present(result);

        log.info("Retorno obtido na confirmação de expense para ExpenseId = {} : {}", command.expenseId(), present);

        return present;
    }

    @Tool(description = "Cancel an expense.")
    public String cancelExpense(
            CancelExpenseCommand command
    ) {
        if (command == null) {
            return "Error canceling expense: command cannot be null";
        }

        FillExpenseResult result = expenseService.cancelExpense(command);
        String present = present(result);

        log.info("Retorno obtido no cancelamento de expense para ExpenseId = {} : {}", command.expenseId(), present);

        return present;
    }

    private String present(FillExpenseResult result) {
        return switch (result) {
            case FillExpenseResult.Saved s -> "Expense saved";
            case FillExpenseResult.Confirmed c -> "Expense confirmed";
            case FillExpenseResult.Canceled c -> "Expense canceled";
            case FillExpenseResult.Incomplete i ->
                    "Expense saved, but still missing: " + String.join(", ", i.missing());
            case FillExpenseResult.NotFound n -> "Expense not found: " + n.id();
            case FillExpenseResult.NotEditable e -> "Expense can no longer be changed, status: " + e.status();
            case FillExpenseResult.InvalidFields inv ->
                    "Invalid confirmation fields: " + String.join(", ", inv.invalidFields());
        };
    }

    private String presentConfirmation(FillExpenseResult result) {
        return switch (result) {
            case FillExpenseResult.Saved s -> "Confirmation created, aguarde as próximas instruções";
            case FillExpenseResult.Confirmed c -> "Expense confirmed";
            case FillExpenseResult.Canceled c -> "Expense canceled";
            case FillExpenseResult.Incomplete i ->
                    "Confirmation created for missing fields: " + String.join(", ", i.missing());
            case FillExpenseResult.NotFound n -> "Expense not found: " + n.id();
            case FillExpenseResult.NotEditable e -> "Expense can no longer be changed, status: " + e.status();
            case FillExpenseResult.InvalidFields inv ->
                    "Invalid confirmation fields: " + String.join(", ", inv.invalidFields());
        };
    }
}
