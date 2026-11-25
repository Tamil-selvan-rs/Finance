package com.myfinance.repository;

import com.myfinance.entity.FinanceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FinanceItemRepository extends JpaRepository<FinanceItem, Long> {

    @Query("SELECT f FROM FinanceItem f WHERE f.user.id = :userId ORDER BY CASE WHEN f.dueDate IS NULL THEN 1 ELSE 0 END, f.dueDate ASC, f.createdAt DESC")
    List<FinanceItem> findByUserIdOrderByDueDateAsc(@Param("userId") Long userId);

    Optional<FinanceItem> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT f FROM FinanceItem f JOIN FETCH f.user WHERE f.dueDate IS NOT NULL AND f.dueDate <= :targetDate AND f.status = 'PENDING' AND f.reminderSent = false")
    List<FinanceItem> findDueItemsForNotification(@Param("targetDate") LocalDate targetDate);
}
