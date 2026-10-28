package com.golu.expense.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.golu.expense.dto.ExpenseDtos.*;
import com.golu.expense.dto.ExpenseDtos.CategoryRequest;
import com.golu.expense.dto.ExpenseDtos.CategoryResponse;
import com.golu.expense.service.CategoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService service;
    private final AppUserPrincipal principal;

    public CategoryController(CategoryService service, AppUserPrincipal principal) {
        this.service = service;
        this.principal = principal;
    }

    @GetMapping
    public List<CategoryResponse> list(Authentication auth) {
        return service.list(uid(auth));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(Authentication auth, @Valid @RequestBody CategoryRequest req) {
        return service.create(uid(auth), req);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(Authentication auth, @PathVariable Long id,
            @Valid @RequestBody CategoryRequest req) {
        return service.update(uid(auth), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication auth, @PathVariable Long id) {
        service.delete(uid(auth), id);
    }

    private Long uid(Authentication auth) {
        return principal.id(auth);
    }
}
