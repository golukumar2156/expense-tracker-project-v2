package com.golu.expense.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.golu.expense.dto.ExpenseDtos.ExpenseRequest;
import com.golu.expense.dto.ExpenseDtos.ExpenseResponse;
import com.golu.expense.service.ExpenseService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService service;
    private final AppUserPrincipal principal;

    public ExpenseController(ExpenseService service, AppUserPrincipal principal) {
        this.service = service;
        this.principal = principal;
    }

    @GetMapping
    public List<ExpenseResponse> list(Authentication auth,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.list(principal.id(auth), categoryId, from, to);
    }

    @GetMapping("/{id}")
    public ExpenseResponse get(Authentication auth, @PathVariable Long id) {
        return service.get(principal.id(auth), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(Authentication auth, @Valid @RequestBody ExpenseRequest req) {
        return service.create(principal.id(auth), req);
    }

    @PutMapping("/{id}")
    public ExpenseResponse update(Authentication auth, @PathVariable Long id, @Valid @RequestBody ExpenseRequest req) {
        return service.update(principal.id(auth), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication auth, @PathVariable Long id) {
        service.delete(principal.id(auth), id);
    }
}
