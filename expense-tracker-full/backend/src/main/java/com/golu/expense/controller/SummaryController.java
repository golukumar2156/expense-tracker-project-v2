package com.golu.expense.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.golu.expense.dto.ExpenseDtos.SummaryResponse;
import com.golu.expense.service.ExpenseService;

@RestController
@RequestMapping("/api/summary")
public class SummaryController {

    private final ExpenseService service;
    private final AppUserPrincipal principal;

    public SummaryController(ExpenseService service, AppUserPrincipal principal) {
        this.service = service;
        this.principal = principal;
    }

    @GetMapping
    public SummaryResponse summary(Authentication auth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.summary(principal.id(auth), from, to);
    }
}
