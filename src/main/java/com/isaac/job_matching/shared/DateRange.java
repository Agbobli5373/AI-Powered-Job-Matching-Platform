package com.isaac.job_matching.shared;

import java.time.LocalDate;
import java.time.Period;

import jakarta.persistence.Embeddable;

/**
 * Value object representing a date range, typically for employment or education
 * periods.
 * 
 * <p>
 * Supports open-ended ranges where the end date is null (indicating "current"
 * or "ongoing").
 */
@Embeddable
public record DateRange(
        LocalDate startDate,
        LocalDate endDate) {

    public DateRange {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
    }

    /**
     * Creates a current/ongoing date range (no end date).
     * 
     * @param startDate the start date
     * @return DateRange with null end date
     */
    public static DateRange current(LocalDate startDate) {
        return new DateRange(startDate, null);
    }

    /**
     * Creates a completed date range.
     * 
     * @param startDate the start date
     * @param endDate   the end date
     * @return DateRange with both dates
     */
    public static DateRange completed(LocalDate startDate, LocalDate endDate) {
        return new DateRange(startDate, endDate);
    }

    /**
     * Checks if this date range represents a current/ongoing period.
     * 
     * @return true if end date is null
     */
    public boolean isCurrent() {
        return endDate == null;
    }

    /**
     * Calculates the duration of this date range.
     * 
     * @return Period representing the duration (uses current date if ongoing)
     */
    public Period duration() {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        return Period.between(startDate, end);
    }

    /**
     * Calculates the duration in total months.
     * 
     * @return total months duration
     */
    public int durationInMonths() {
        Period period = duration();
        return period.getYears() * 12 + period.getMonths();
    }

    /**
     * Calculates the duration in years (rounded down).
     * 
     * @return total years duration
     */
    public int durationInYears() {
        return duration().getYears();
    }

    /**
     * Returns a human-readable duration string.
     * 
     * @return formatted duration (e.g., "2 years, 3 months")
     */
    public String durationFormatted() {
        Period period = duration();
        int years = period.getYears();
        int months = period.getMonths();

        if (years == 0 && months == 0) {
            return "Less than a month";
        }

        StringBuilder sb = new StringBuilder();
        if (years > 0) {
            sb.append(years).append(years == 1 ? " year" : " years");
        }
        if (months > 0) {
            if (!sb.isEmpty())
                sb.append(", ");
            sb.append(months).append(months == 1 ? " month" : " months");
        }
        return sb.toString();
    }

    /**
     * Checks if a given date falls within this date range.
     * 
     * @param date the date to check
     * @return true if date is within range (inclusive)
     */
    public boolean contains(LocalDate date) {
        if (date == null || startDate == null)
            return false;
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        return !date.isBefore(startDate) && !date.isAfter(end);
    }

    /**
     * Checks if this date range overlaps with another.
     * 
     * @param other the other date range
     * @return true if ranges overlap
     */
    public boolean overlaps(DateRange other) {
        if (other == null || startDate == null || other.startDate == null)
            return false;

        LocalDate thisEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate otherEnd = other.endDate != null ? other.endDate : LocalDate.now();

        return !startDate.isAfter(otherEnd) && !thisEnd.isBefore(other.startDate);
    }
}
