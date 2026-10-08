package com.voicefinance.ai.tools;

import com.voicefinance.domain.results.FillExpenseResult;
import com.voicefinance.service.ExpenseService;
import com.voicefinance.domain.commands.CreateConfirmationCommand;
import com.voicefinance.domain.commands.FillExpenseCommand;
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

    @Tool(description = "Fill in the details of an existing expense and save it to the database.")
    public String fillExpense(
            FillExpenseCommand command
    ) {
        var result = expenseService.fill(command);
        String present = present(result);

        log.info("Retorno obtido no salvameto de expense para Expendeid = {} : {} ",command.expenseId(), present);

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

        var result = expenseService.createExpenseFieldConfirmation(command.missingFields(), command.expenseId(), command.question());
        String returnConfirmation = presentConfirmation(result);

        log.info("Retorno obtido na criação de confirmação para Expendeid = {} : {} ",command.expenseId(), returnConfirmation);

        return returnConfirmation;
    }


    private String present(FillExpenseResult result) {
        return switch (result) {
            case FillExpenseResult.Saved s -> "Expense saved";
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
            case FillExpenseResult.Incomplete i ->
                    "Confirmation created for missing fields: " + String.join(", ", i.missing());
            case FillExpenseResult.NotFound n -> "Expense not found: " + n.id();
            case FillExpenseResult.NotEditable e -> "Expense can no longer be changed, status: " + e.status();
            case FillExpenseResult.InvalidFields inv ->
                    "Invalid confirmation fields: " + String.join(", ", inv.invalidFields());
        };
    }
}
