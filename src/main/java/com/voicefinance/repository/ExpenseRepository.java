package com.voicefinance.repository;

import com.voicefinance.domain.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID>, JpaSpecificationExecutor<Expense> {

    Optional<Expense> findByIdAndUserId(UUID id, UUID userId);

    List<Expense> findByUserId(UUID userId);

    List<Expense> findByUserIdAndOccurredOnBetween(UUID userId, LocalDate startDate, LocalDate endDate);
}
