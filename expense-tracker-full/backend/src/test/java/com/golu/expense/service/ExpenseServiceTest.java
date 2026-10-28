package com.golu.expense.service;

import com.golu.expense.exception.ApiException;
import com.golu.expense.repository.CategoryRepository;
import com.golu.expense.repository.ExpenseRepository;
import com.golu.expense.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ExpenseServiceTest {
    private ExpenseRepository expenses;
    private ExpenseService service;

    @BeforeEach
    void setUp() {
        expenses = mock(ExpenseRepository.class);
        service = new ExpenseService(expenses, mock(CategoryRepository.class), mock(UserRepository.class));
    }

    @Test
    void listPassesDateAndCategoryFiltersToRepository() {
        when(expenses.searchForUser(7L, 3L, LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31")))
                .thenReturn(java.util.List.of());

        var result = service.list(7L, 3L, LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"));

        assertEquals(0, result.size());
        verify(expenses).searchForUser(7L, 3L, LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"));
    }

    @Test
    void listRejectsInvalidDateRangeBeforeQueryingDatabase() {
        ApiException exception = assertThrows(ApiException.class,
                () -> service.list(7L, null, LocalDate.parse("2026-10-31"), LocalDate.parse("2026-10-01")));

        assertEquals("The from date must be before or equal to the to date.", exception.getMessage());
        verifyNoInteractions(expenses);
    }
}
