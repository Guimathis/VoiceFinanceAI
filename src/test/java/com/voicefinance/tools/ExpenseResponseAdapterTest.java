package com.voicefinance.tools;

import com.voicefinance.ai.tools.adapter.JsonExpenseResponseAdapter;
import com.voicefinance.ai.tools.adapter.TextExpenseResponseAdapter;
import com.voicefinance.domain.entity.Expense;
import com.voicefinance.domain.entity.Money;
import com.voicefinance.domain.enums.Category;
import com.voicefinance.domain.enums.ExpenseStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExpenseResponseAdapterTest {

    private final TextExpenseResponseAdapter textAdapter = new TextExpenseResponseAdapter();
    private final JsonExpenseResponseAdapter jsonAdapter = new JsonExpenseResponseAdapter();

    @Test
    void textAdapter_WhenEmpty_ReturnsFriendlyMessage() {
        String result = textAdapter.adapt(List.of());
        assertThat(result).isEqualTo("Nenhum gasto encontrado para os critérios informados.");

        String nullResult = textAdapter.adapt(null);
        assertThat(nullResult).isEqualTo("Nenhum gasto encontrado para os critérios informados.");
    }

    @Test
    void textAdapter_WhenHasExpenses_FormatsLinesCorrectly() {
        UUID id = UUID.randomUUID();
        Expense expense = Expense.builder()
                .id(id)
                .amount(new Money(new BigDecimal("45.50"), "BRL"))
                .category(Category.ALIMENTACAO)
                .description("Almoço executivo")
                .occurredOn(LocalDate.of(2026, 10, 8))
                .status(ExpenseStatus.CONFIRMED)
                .build();

        String result = textAdapter.adapt(List.of(expense));

        assertThat(result).contains("Gastos encontrados (1):");
        assertThat(result).contains(id.toString());
        assertThat(result).contains("BRL 45.50");
        assertThat(result).contains("ALIMENTACAO");
        assertThat(result).contains("Almoço executivo");
        assertThat(result).contains("CONFIRMED");
        assertThat(result).contains("2026-10-08");
    }

    @Test
    void jsonAdapter_WhenEmpty_ReturnsEmptyArrayJson() {
        assertThat(jsonAdapter.adapt(List.of())).isEqualTo("[]");
        assertThat(jsonAdapter.adapt(null)).isEqualTo("[]");
    }

    @Test
    void jsonAdapter_WhenHasExpenses_ReturnsValidJsonArray() {
        UUID id = UUID.randomUUID();
        Expense expense = Expense.builder()
                .id(id)
                .amount(new Money(new BigDecimal("120.00"), "BRL"))
                .category(Category.TRANSPORTE)
                .description("Uber")
                .occurredOn(LocalDate.of(2026, 10, 7))
                .status(ExpenseStatus.CONFIRMED)
                .build();

        String json = jsonAdapter.adapt(List.of(expense));

        assertThat(json).contains(id.toString());
        assertThat(json).contains("120.00");
        assertThat(json).contains("BRL");
        assertThat(json).contains("TRANSPORTE");
        assertThat(json).contains("Uber");
        assertThat(json).contains("CONFIRMED");
    }
}
