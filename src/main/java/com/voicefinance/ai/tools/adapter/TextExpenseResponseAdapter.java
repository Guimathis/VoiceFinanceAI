package com.voicefinance.ai.tools.adapter;

import com.voicefinance.domain.entity.Expense;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Primary
public class TextExpenseResponseAdapter implements ExpenseResponseAdapter {

    @Override
    public String adapt(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return "Nenhum gasto encontrado para os critérios informados.";
        }

        StringBuilder sb = new StringBuilder("Gastos encontrados (")
                .append(expenses.size())
                .append("):\n");

        for (Expense expense : expenses) {
            String amountFormatted = expense.getAmount() != null
                    ? expense.getAmount().currency() + " " + expense.getAmount().amount()
                    : "Valor não informado";

            String categoryName = expense.getCategory() != null
                    ? expense.getCategory().name()
                    : "N/A";

            String desc = expense.getDescription() != null && !expense.getDescription().isBlank()
                    ? expense.getDescription()
                    : "Sem descrição";

            String date = expense.getOccurredOn() != null
                    ? expense.getOccurredOn().toString()
                    : "N/A";

            sb.append("- ID: ").append(expense.getId())
                    .append(" | Data: ").append(date)
                    .append(" | ").append(amountFormatted)
                    .append(" | Categoria: ").append(categoryName)
                    .append(" | Descrição: ").append(desc)
                    .append(" | Status: ").append(expense.getStatus())
                    .append("\n");
        }

        return sb.toString().trim();
    }
}
