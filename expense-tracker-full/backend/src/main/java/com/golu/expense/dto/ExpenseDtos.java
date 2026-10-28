package com.golu.expense.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class ExpenseDtos {

    private ExpenseDtos() {
    }

    public record ExpenseRequest(
            @NotBlank
            @Size(max = 120) String title,
            @NotNull
            @DecimalMin(value = "0.01")
            @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @NotNull LocalDate date,
            @NotNull
            @Positive Long categoryId) {}

    public record ExpenseResponse(Long id, String title, BigDecimal amount, LocalDate date,
            Long categoryId, String category, Instant createdAt, Instant updatedAt) {}

    public record CategoryRequest(@NotBlank
            @Size(min = 2, max = 60) String name,
            @Size(max = 180) String description) {}

    public record CategoryResponse(Long id, String name, String description, Instant createdAt) {}

    public record CategorySummary(String category, BigDecimal total, long transactionCount) {}

    public record SummaryResponse(BigDecimal total, long transactionCount, java.util.List<CategorySummary> categories) {}
}
