package com.golu.expense.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.golu.expense.entity.Expense;
import com.golu.expense.repository.ExpenseRepository;

@Service
public class ExpenseSummaryService {

    private final ExpenseRepository expenseRepository;

    public ExpenseSummaryService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    public Map<String, Object> getSummary(
            Long userId,
            LocalDate startDate,
            LocalDate endDate) {

        List<Expense> expenses
                = expenseRepository.findByUserIdAndDateBetween(
                        userId, startDate, endDate);

        BigDecimal total = BigDecimal.ZERO;
        Map<String, BigDecimal> categoryTotals
                = new LinkedHashMap<>();

        for (Expense expense : expenses) {
            BigDecimal amount = expense.getAmount();

            if (amount == null) {
                amount = BigDecimal.ZERO;
            }

            total = total.add(amount);

            String category = expense.getCategory().getName();

            categoryTotals.merge(
                    category,
                    amount,
                    BigDecimal::add
            );
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalExpenses", total);
        result.put("transactionCount", expenses.size());
        result.put("categorySummary", categoryTotals);
        result.put("startDate", startDate);
        result.put("endDate", endDate);

        return result;
    }
}
