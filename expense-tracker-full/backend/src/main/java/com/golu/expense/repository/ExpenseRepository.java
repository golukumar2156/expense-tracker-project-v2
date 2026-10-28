package com.golu.expense.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.golu.expense.entity.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("""
      select e from Expense e join fetch e.category
      where e.user.id = :userId
        and (:categoryId is null or e.category.id = :categoryId)
        and (:startDate is null or e.date >= :startDate)
        and (:endDate is null or e.date <= :endDate)
      order by e.date desc, e.id desc
      """)
    List<Expense> searchForUser(@Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("select e from Expense e join fetch e.category where e.id = :id and e.user.id = :userId")
    Optional<Expense> findOneForUser(@Param("id") Long id, @Param("userId") Long userId);

    @Query("""
      select e.category.name, sum(e.amount), count(e)
      from Expense e where e.user.id = :userId
        and (:startDate is null or e.date >= :startDate)
        and (:endDate is null or e.date <= :endDate)
      group by e.category.id, e.category.name order by sum(e.amount) desc
      """)
    List<Object[]> summarizeByCategory(@Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<Expense> findByUserIdAndDateBetween(
        Long userId,
        LocalDate startDate,
        LocalDate endDate);
}
