package com.isaac.job_matching.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import jakarta.persistence.Embeddable;

/**
 * Value object representing a monetary amount with currency.
 * 
 * <p>
 * Immutable record with validation ensuring amount is not null.
 * Currency defaults to USD if not specified.
 */
@Embeddable
public record Money(
        BigDecimal amount,
        String currency) {

    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        currency = currency == null || currency.isBlank() ? "USD" : currency;
    }

    /**
     * Creates a Money instance with USD currency.
     * 
     * @param amount the monetary amount
     * @return Money with USD currency
     */
    public static Money usd(BigDecimal amount) {
        return new Money(amount, "USD");
    }

    /**
     * Creates a Money instance with USD currency from a double.
     * 
     * @param amount the monetary amount
     * @return Money with USD currency
     */
    public static Money usd(double amount) {
        return new Money(BigDecimal.valueOf(amount), "USD");
    }

    /**
     * Formats the money as a string with currency symbol.
     * 
     * @return formatted string (e.g., "USD 50,000.00")
     */
    public String format() {
        return currency + " " + amount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Checks if this amount is greater than another.
     * 
     * @param other the other Money to compare
     * @return true if this amount is greater
     */
    public boolean isGreaterThan(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot compare different currencies");
        }
        return this.amount.compareTo(other.amount) > 0;
    }

    /**
     * Checks if this amount is less than another.
     * 
     * @param other the other Money to compare
     * @return true if this amount is less
     */
    public boolean isLessThan(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot compare different currencies");
        }
        return this.amount.compareTo(other.amount) < 0;
    }

    /**
     * Checks if this amount falls within a salary range.
     * 
     * @param min minimum salary
     * @param max maximum salary
     * @return true if amount is within range (inclusive)
     */
    public boolean isWithinRange(Money min, Money max) {
        return !isLessThan(min) && !isGreaterThan(max);
    }
}
