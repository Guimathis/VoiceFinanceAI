package com.voicefinance.ai.tools.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voicefinance.domain.entity.Expense;
import com.voicefinance.domain.enums.Category;
import com.voicefinance.domain.enums.ExpenseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class JsonExpenseResponseAdapter implements ExpenseResponseAdapter {

    private final ObjectMapper objectMapper;

    public record ExpenseSummaryDto(
            UUID id,
            BigDecimal amount,
            String currency,
            Category category,
            String description,
            LocalDate occurredOn,
            ExpenseStatus status
    ) {
        public static ExpenseSummaryDto from(Expense e) {
            BigDecimal amt = e.getAmount() != null ? e.getAmount().amount() : null;
            String cur = e.getAmount() != null ? e.getAmount().currency() : null;
            return new ExpenseSummaryDto(
                    e.getId(),
                    amt,
                    cur,
                    e.getCategory(),
                    e.getDescription(),
                    e.getOccurredOn(),
                    e.getStatus()
            );
        }
    }

    public JsonExpenseResponseAdapter() {
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public JsonExpenseResponseAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String adapt(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return "[]";
        }
        try {
            List<ExpenseSummaryDto> dtos = expenses.stream()
                    .map(ExpenseSummaryDto::from)
                    .toList();
            return objectMapper.writeValueAsString(dtos);
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar despesas para JSON", e);
            return "[]";
        }
    }
}
