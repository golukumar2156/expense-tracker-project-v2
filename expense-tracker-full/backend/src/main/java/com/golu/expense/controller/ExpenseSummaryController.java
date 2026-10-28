package com.golu.expense.controller;
import com.golu.expense.service.ExpenseSummaryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseSummaryController {

    private final ExpenseSummaryService summaryService;

    public ExpenseSummaryController(
            ExpenseSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(
            @RequestParam Long userId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {

        return summaryService.getSummary(
                userId, startDate, endDate);
    }
}