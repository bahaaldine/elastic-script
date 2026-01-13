/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.escript.scheduling;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple cron expression parser for scheduling jobs.
 * 
 * <p>Supports standard 5-field cron format: minute hour day-of-month month day-of-week</p>
 * 
 * <p>Examples:</p>
 * <ul>
 *   <li>{@code * * * * *} - Every minute</li>
 *   <li>{@code 0 * * * *} - Every hour at :00</li>
 *   <li>{@code 0 2 * * *} - Daily at 2:00 AM</li>
 *   <li>{@code 0 0 * * 0} - Weekly on Sunday at midnight</li>
 *   <li>{@code 0 0 1 * *} - Monthly on 1st at midnight</li>
 * </ul>
 * 
 * <p>Also supports aliases: {@code @hourly}, {@code @daily}, {@code @weekly}, 
 * {@code @monthly}, {@code @yearly}</p>
 */
public class CronParser {

    private static final Pattern STEP_PATTERN = Pattern.compile("\\*/?(\\d+)");
    private static final Pattern RANGE_PATTERN = Pattern.compile("(\\d+)-(\\d+)");
    
    private final Set<Integer> minutes;
    private final Set<Integer> hours;
    private final Set<Integer> daysOfMonth;
    private final Set<Integer> months;
    private final Set<Integer> daysOfWeek;

    /**
     * Parse a cron expression.
     * 
     * @param cronExpression The cron expression (5 fields or alias)
     * @throws IllegalArgumentException if the expression is invalid
     */
    public CronParser(String cronExpression) {
        String normalized = normalizeAliases(cronExpression.trim());
        String[] parts = normalized.split("\\s+");
        
        if (parts.length != 5) {
            throw new IllegalArgumentException(
                "Invalid cron expression: expected 5 fields (minute hour day month weekday), got " + parts.length
            );
        }
        
        this.minutes = parseField(parts[0], 0, 59, "minute");
        this.hours = parseField(parts[1], 0, 23, "hour");
        this.daysOfMonth = parseField(parts[2], 1, 31, "day of month");
        this.months = parseField(parts[3], 1, 12, "month");
        this.daysOfWeek = parseField(parts[4], 0, 6, "day of week");
    }

    /**
     * Convert aliases to standard cron format.
     */
    private String normalizeAliases(String expression) {
        return switch (expression.toLowerCase()) {
            case "@yearly", "@annually" -> "0 0 1 1 *";
            case "@monthly" -> "0 0 1 * *";
            case "@weekly" -> "0 0 * * 0";
            case "@daily", "@midnight" -> "0 0 * * *";
            case "@hourly" -> "0 * * * *";
            default -> expression;
        };
    }

    /**
     * Parse a single cron field.
     */
    private Set<Integer> parseField(String field, int min, int max, String fieldName) {
        Set<Integer> values = new TreeSet<>();
        
        // Handle wildcard
        if (field.equals("*")) {
            for (int i = min; i <= max; i++) {
                values.add(i);
            }
            return values;
        }
        
        // Handle comma-separated values
        for (String part : field.split(",")) {
            // Handle step (*/5 or 0-30/5)
            Matcher stepMatcher = STEP_PATTERN.matcher(part);
            if (part.startsWith("*/") || part.startsWith("*")) {
                int step = 1;
                if (stepMatcher.matches()) {
                    step = Integer.parseInt(stepMatcher.group(1));
                }
                for (int i = min; i <= max; i += step) {
                    values.add(i);
                }
                continue;
            }
            
            // Handle range (1-5)
            Matcher rangeMatcher = RANGE_PATTERN.matcher(part);
            if (rangeMatcher.matches()) {
                int start = Integer.parseInt(rangeMatcher.group(1));
                int end = Integer.parseInt(rangeMatcher.group(2));
                if (start < min || end > max || start > end) {
                    throw new IllegalArgumentException(
                        "Invalid range " + part + " for " + fieldName + " (valid: " + min + "-" + max + ")"
                    );
                }
                for (int i = start; i <= end; i++) {
                    values.add(i);
                }
                continue;
            }
            
            // Handle single value
            try {
                int value = Integer.parseInt(part);
                if (value < min || value > max) {
                    throw new IllegalArgumentException(
                        "Value " + value + " out of range for " + fieldName + " (valid: " + min + "-" + max + ")"
                    );
                }
                values.add(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid value '" + part + "' for " + fieldName);
            }
        }
        
        return values;
    }

    /**
     * Calculate the next run time after the given instant.
     * 
     * @param after The instant to calculate from
     * @param zoneId The timezone to use
     * @return The next run time
     */
    public Instant getNextRunTime(Instant after, ZoneId zoneId) {
        ZonedDateTime dt = after.atZone(zoneId).plusMinutes(1)
            .withSecond(0)
            .withNano(0);
        
        // Try up to 4 years to find next match (handles rare schedules)
        ZonedDateTime limit = dt.plusYears(4);
        
        while (dt.isBefore(limit)) {
            // Check month
            if (!months.contains(dt.getMonthValue())) {
                dt = dt.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0);
                continue;
            }
            
            // Check day of month
            if (!daysOfMonth.contains(dt.getDayOfMonth())) {
                dt = dt.plusDays(1).withHour(0).withMinute(0);
                continue;
            }
            
            // Check day of week (0 = Sunday, 1 = Monday, ...)
            int dow = dt.getDayOfWeek().getValue() % 7;  // Java: 1=Mon, 7=Sun -> 0=Sun
            if (!daysOfWeek.contains(dow)) {
                dt = dt.plusDays(1).withHour(0).withMinute(0);
                continue;
            }
            
            // Check hour
            if (!hours.contains(dt.getHour())) {
                dt = dt.plusHours(1).withMinute(0);
                continue;
            }
            
            // Check minute
            if (!minutes.contains(dt.getMinute())) {
                dt = dt.plusMinutes(1);
                continue;
            }
            
            // Found match!
            return dt.toInstant();
        }
        
        throw new IllegalStateException("Could not find next run time within 4 years for cron expression");
    }

    /**
     * Validate a cron expression without parsing.
     * 
     * @param expression The cron expression to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String expression) {
        try {
            new CronParser(expression);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Get a human-readable description of the cron expression.
     */
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        
        if (minutes.size() == 60 && hours.size() == 24) {
            sb.append("Every minute");
        } else if (minutes.size() == 1 && minutes.contains(0) && hours.size() == 24) {
            sb.append("Every hour");
        } else if (minutes.size() == 1 && hours.size() == 1) {
            int minute = minutes.iterator().next();
            int hour = hours.iterator().next();
            sb.append(String.format("At %02d:%02d", hour, minute));
        } else {
            sb.append("At minutes ").append(minutes);
            sb.append(" of hours ").append(hours);
        }
        
        if (daysOfMonth.size() < 31 || months.size() < 12 || daysOfWeek.size() < 7) {
            if (daysOfMonth.size() < 31) {
                sb.append(" on days ").append(daysOfMonth);
            }
            if (months.size() < 12) {
                sb.append(" in months ").append(months);
            }
            if (daysOfWeek.size() < 7) {
                sb.append(" on weekdays ").append(daysOfWeek);
            }
        }
        
        return sb.toString();
    }

    @Override
    public String toString() {
        return "CronParser{" + getDescription() + "}";
    }
}
