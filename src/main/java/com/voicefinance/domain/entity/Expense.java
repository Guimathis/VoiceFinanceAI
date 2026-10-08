package com.voicefinance.domain.entity;

import com.voicefinance.domain.annotation.Confirmable;
import com.voicefinance.domain.enums.Category;
import com.voicefinance.domain.enums.ExpenseOrigin;
import com.voicefinance.domain.enums.ExpenseStatus;
import com.voicefinance.domain.commands.FillExpenseCommand;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "expenses")
public class Expense implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Embedded
    private Money amount;

    @Enumerated(EnumType.STRING)
    @Confirmable(label = "categoria")
    private Category category;

    @Column(length = 200)
    @Confirmable(label = "descrição", required = false)
    private String description;

    @Column(name = "occurred_on")
    @Confirmable(label = "occurredOn")
    private LocalDate occurredOn;

    // Metadata
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExpenseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false)
    private ExpenseOrigin origin;

    @Column(name = "capture_id", nullable = false)
    private UUID captureId;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL)
    private List<ExpenseFieldConfirmation> expenseConfirmations;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @PrePersist
    private void createdAt() {
        this.createdAt = Instant.now();
    }

    @PreUpdate
    private void updatedAt() {
        this.updatedAt = Instant.now();
    }

    public boolean isEditable() { return status != ExpenseStatus.CONFIRMED && status != ExpenseStatus.CANCELED; }

    public void fill(FillExpenseCommand cmd) {
        if (cmd.amount() != null) {
            String cur = cmd.currency() != null ? cmd.currency()
                    : amount != null ? amount.currency() : null;
            this.amount = new Money(cmd.amount(), cur);
        }
        if (cmd.description() != null && !cmd.description().isBlank()) this.description = cmd.description();
        if (cmd.category() != null) this.category = cmd.category();
        if (cmd.OccurredOn() != null) this.occurredOn = cmd.OccurredOn();

    }

    public void addConfirmation(ExpenseFieldConfirmation confirmation) {
        if (this.expenseConfirmations == null) {
            this.expenseConfirmations = new ArrayList<>();
        }
        confirmation.setExpense(this);
        this.expenseConfirmations.add(confirmation);
    }

    public void confirmExpense() {
        this.confirmedAt = Instant.now();
        this.status = ExpenseStatus.CONFIRMED;
    }

    public List<String> missingFields() {
        List<String> missing = new ArrayList<>();
        if (amount == null) missing.add("amount");
        if (category == null) missing.add("category");
        if (occurredOn == null) missing.add("occurredOnDate");
        return missing;
    }
}
