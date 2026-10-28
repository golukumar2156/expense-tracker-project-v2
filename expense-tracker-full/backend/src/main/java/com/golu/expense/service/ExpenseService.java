package com.golu.expense.service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.golu.expense.dto.ExpenseDtos.CategorySummary;
import com.golu.expense.dto.ExpenseDtos.ExpenseRequest;
import com.golu.expense.dto.ExpenseDtos.ExpenseResponse;
import com.golu.expense.dto.ExpenseDtos.SummaryResponse;
import com.golu.expense.entity.Expense;
import com.golu.expense.entity.ExpenseCategory;
import com.golu.expense.entity.User;
import com.golu.expense.exception.ApiException;
import com.golu.expense.repository.CategoryRepository;
import com.golu.expense.repository.ExpenseRepository;
import com.golu.expense.repository.UserRepository;

@Service
public class ExpenseService {

    private final ExpenseRepository expenses;
    private final CategoryRepository categories;
    private final UserRepository users;

    public ExpenseService(ExpenseRepository expenses, CategoryRepository categories, UserRepository users) {
        this.expenses = expenses;
        this.categories = categories;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> list(Long uid, Long categoryId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        return expenses.searchForUser(uid, categoryId, from, to).stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(Long uid, Long id) {
        return dto(expenses.findOneForUser(id, uid).orElseThrow(() -> ApiException.notFound("Expense not found.")));
    }

    @Transactional
    public ExpenseResponse create(Long uid, ExpenseRequest req) {
        User user = users.findById(uid).orElseThrow();
        Expense e = new Expense();
        e.setUser(user);
        apply(e, uid, req);
        return dto(expenses.save(e));
    }

    @Transactional
    public ExpenseResponse update(Long uid, Long id, ExpenseRequest req) {
        Expense e = expenses.findOneForUser(id, uid).orElseThrow(() -> ApiException.notFound("Expense not found."));
        apply(e, uid, req);
        return dto(e);
    }

    @Transactional
    public void delete(Long uid, Long id) {
        Expense e = expenses.findOneForUser(id, uid).orElseThrow(() -> ApiException.notFound("Expense not found."));
        expenses.delete(e);
    }

    @Transactional(readOnly = true)
    public SummaryResponse summary(Long uid, LocalDate from, LocalDate to) {
        validateRange(from, to);
        List<Object[]> rows = expenses.summarizeByCategory(uid, from, to);
        List<CategorySummary> summaries = rows.stream().map(r -> new CategorySummary((String) r[0], (BigDecimal) r[1], ((Number) r[2]).longValue())).toList();
        BigDecimal sum = summaries.stream().map(CategorySummary::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        long count = summaries.stream().mapToLong(CategorySummary::transactionCount).sum();
        return new SummaryResponse(sum, count, summaries);
    }

    private void apply(Expense e, Long uid, ExpenseRequest req) {
        ExpenseCategory c = categories.findByIdAndUserId(req.categoryId(), uid)
                .orElseThrow(() -> ApiException.badRequest("Category does not exist for this user."));
                e.setTitle(req.title());
                e.setAmount(req.amount());
                e.setDate(req.date());
                e.setCategory(c);
    }

    private ExpenseResponse dto(Expense e) {
        return new ExpenseResponse(e.getId(), e.getTitle(), e.getAmount(), e.getDate(),
                e.getCategory().getId(), e.getCategory().getName(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.badRequest("The from date must be before or equal to the to date.");
        }
    }
}
