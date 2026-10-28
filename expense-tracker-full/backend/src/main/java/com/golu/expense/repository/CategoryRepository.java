package com.golu.expense.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.golu.expense.entity.ExpenseCategory;

public interface CategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    List<ExpenseCategory> findAllByUserIdOrderByNameAsc(Long userId);

    Optional<ExpenseCategory> findByIdAndUserId(Long id, Long userId);

    Optional<ExpenseCategory> findByNameIgnoreCaseAndUserId(String name, Long userId);

    boolean existsByNameIgnoreCaseAndUserId(String name, Long userId);
}
