package com.golu.expense.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.golu.expense.dto.ExpenseDtos.CategoryRequest;
import com.golu.expense.dto.ExpenseDtos.CategoryResponse;
import com.golu.expense.entity.ExpenseCategory;
import com.golu.expense.entity.User;
import com.golu.expense.exception.ApiException;
import com.golu.expense.repository.CategoryRepository;
import com.golu.expense.repository.UserRepository;

@Service
public class CategoryService {

    private final CategoryRepository categories;
    private final UserRepository users;

    public CategoryService(CategoryRepository categories, UserRepository users) {
        this.categories = categories;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(Long uid) {
        return categories.findAllByUserIdOrderByNameAsc(uid).stream().map(this::dto).toList();
    }

    @Transactional
    public CategoryResponse create(Long uid, CategoryRequest req) {
        String name = req.name().trim();
        if (categories.existsByNameIgnoreCaseAndUserId(name, uid)) {
            throw ApiException.conflict("Category already exists.");
        }
        User user = users.findById(uid).orElseThrow();
        ExpenseCategory c = new ExpenseCategory();
        c.setName(name);
        c.setDescription(req.description());
        c.setUser(user);
        return dto(categories.save(c));
    }

    @Transactional
    public CategoryResponse update(Long uid, Long id, CategoryRequest req) {
        ExpenseCategory c = categories.findByIdAndUserId(id, uid).orElseThrow(() -> ApiException.notFound("Category not found."));
        String name = req.name().trim();
        if (categories.findByNameIgnoreCaseAndUserId(name, uid).filter(other -> !other.getId().equals(id)).isPresent()) {
            throw ApiException.conflict("Category already exists.");
        }
        c.setName(name);
        c.setDescription(req.description());
        return dto(c);
    }

    @Transactional
    public void delete(Long uid, Long id) {
        ExpenseCategory c = categories.findByIdAndUserId(id, uid).orElseThrow(() -> ApiException.notFound("Category not found."));
        if (!c.getExpenses().isEmpty()) {
            throw ApiException.conflict("This category has expenses. Reassign or delete them first.");
        }
        categories.delete(c);
    }

    private CategoryResponse dto(ExpenseCategory c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getCreatedAt());
    }
}
