package com.voicefinance.domain.entity;

import com.voicefinance.domain.enums.ConfirmationField;
import com.voicefinance.domain.enums.ConfirmationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "expense_confirmations")
public class ExpenseFieldConfirmation implements Serializable {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", referencedColumnName = "id")
    private Expense expense;

    @Column(length = 200, nullable = false)
    private String question;

    @Column(length = 200, nullable = true)
    private String answer;

    @Enumerated(EnumType.STRING)
    private ConfirmationField field;

    @Enumerated(EnumType.STRING)
    private ConfirmationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    private void createdAt() {
        this.createdAt = Instant.now() ;
    }

    @PreUpdate
    private void updatedAt() {
        this.updatedAt = Instant.now();
    }

    public void confirm(String answer) {
        this.status = ConfirmationStatus.CONFIRMED;
        if (answer != null) {
            this.answer = answer;
        }
    }

    public void reject() {
        this.status = ConfirmationStatus.REJECTED;
    }
}
