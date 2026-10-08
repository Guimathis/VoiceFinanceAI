package com.voicefinance.domain.entity;

import com.voicefinance.domain.annotation.Confirmable;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Embeddable
public record Money(
        @Positive(message = "O valor não pode ser menor ou igual a zero.")
        @Column(name = "amount")
        @Confirmable(label = "amount")
        BigDecimal amount,

        @Column(name = "currency", length = 3)
        @Confirmable(label = "currency")
        String currency
) {
    public Money {
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        currency = (currency == null) ? "BRL" : currency;
    }

    public static Money brl(BigDecimal value) {
        return new Money(value, "BRL");
    }
}
