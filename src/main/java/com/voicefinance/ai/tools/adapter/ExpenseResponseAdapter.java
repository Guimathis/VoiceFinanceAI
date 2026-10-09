package com.voicefinance.ai.tools.adapter;

import com.voicefinance.domain.entity.Expense;

import java.util.List;

public interface ExpenseResponseAdapter {
    String adapt(List<Expense> expenses);
}
