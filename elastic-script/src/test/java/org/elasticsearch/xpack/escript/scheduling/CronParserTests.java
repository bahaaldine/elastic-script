/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.escript.scheduling;

import org.elasticsearch.test.ESTestCase;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class CronParserTests extends ESTestCase {

    // ==================== Valid Cron Expressions ====================

    public void testEveryMinute() {
        CronParser cron = new CronParser("* * * * *");
        assertNotNull(cron);
        
        Instant now = Instant.parse("2026-01-12T10:30:00Z");
        Instant next = cron.getNextRunTime(now, ZoneId.of("UTC"));
        
        // Should be next minute
        assertEquals("2026-01-12T10:31:00Z", next.toString());
    }

    public void testEveryHour() {
        CronParser cron = new CronParser("0 * * * *");
        
        Instant now = Instant.parse("2026-01-12T10:30:00Z");
        Instant next = cron.getNextRunTime(now, ZoneId.of("UTC"));
        
        // Should be next hour at :00
        assertEquals("2026-01-12T11:00:00Z", next.toString());
    }

    public void testDailyAt2AM() {
        CronParser cron = new CronParser("0 2 * * *");
        
        Instant now = Instant.parse("2026-01-12T10:30:00Z");
        Instant next = cron.getNextRunTime(now, ZoneId.of("UTC"));
        
        // Should be next day at 2 AM
        assertEquals("2026-01-13T02:00:00Z", next.toString());
    }

    public void testDailyAt2AMBeforeSchedule() {
        CronParser cron = new CronParser("0 2 * * *");
        
        // Before 2 AM on same day
        Instant now = Instant.parse("2026-01-12T01:30:00Z");
        Instant next = cron.getNextRunTime(now, ZoneId.of("UTC"));
        
        // Should be same day at 2 AM
        assertEquals("2026-01-12T02:00:00Z", next.toString());
    }

    public void testEvery5Minutes() {
        CronParser cron = new CronParser("*/5 * * * *");
        
        Instant now = Instant.parse("2026-01-12T10:32:00Z");
        Instant next = cron.getNextRunTime(now, ZoneId.of("UTC"));
        
        // Should be :35
        assertEquals("2026-01-12T10:35:00Z", next.toString());
    }

    public void testEvery15Minutes() {
        CronParser cron = new CronParser("*/15 * * * *");
        
        Instant now = Instant.parse("2026-01-12T10:20:00Z");
        Instant next = cron.getNextRunTime(now, ZoneId.of("UTC"));
        
        // Should be :30
        assertEquals("2026-01-12T10:30:00Z", next.toString());
    }

    public void testWeeklyOnSunday() {
        CronParser cron = new CronParser("0 0 * * 0");
        
        // Monday
        Instant now = Instant.parse("2026-01-12T10:30:00Z");
        Instant next = cron.getNextRunTime(now, ZoneId.of("UTC"));
        
        // Should be next Sunday at midnight
        ZonedDateTime nextZdt = next.atZone(ZoneId.of("UTC"));
        assertEquals(java.time.DayOfWeek.SUNDAY, nextZdt.getDayOfWeek());
        assertEquals(0, nextZdt.getHour());
        assertEquals(0, nextZdt.getMinute());
    }

    public void testMonthlyOnFirst() {
        CronParser cron = new CronParser("0 0 1 * *");
        
        Instant now = Instant.parse("2026-01-12T10:30:00Z");
        Instant next = cron.getNextRunTime(now, ZoneId.of("UTC"));
        
        // Should be Feb 1st at midnight
        assertEquals("2026-02-01T00:00:00Z", next.toString());
    }

    public void testSpecificTime() {
        CronParser cron = new CronParser("30 14 * * *");  // 2:30 PM daily
        
        Instant now = Instant.parse("2026-01-12T10:00:00Z");
        Instant next = cron.getNextRunTime(now, ZoneId.of("UTC"));
        
        assertEquals("2026-01-12T14:30:00Z", next.toString());
    }

    public void testRange() {
        CronParser cron = new CronParser("0 9-17 * * *");  // Every hour 9 AM to 5 PM
        
        Instant now = Instant.parse("2026-01-12T08:30:00Z");
        Instant next = cron.getNextRunTime(now, ZoneId.of("UTC"));
        
        assertEquals("2026-01-12T09:00:00Z", next.toString());
    }

    public void testList() {
        CronParser cron = new CronParser("0 0,12 * * *");  // Midnight and noon
        
        Instant now = Instant.parse("2026-01-12T10:00:00Z");
        Instant next = cron.getNextRunTime(now, ZoneId.of("UTC"));
        
        assertEquals("2026-01-12T12:00:00Z", next.toString());
    }

    // ==================== Aliases ====================

    public void testAliasHourly() {
        CronParser cron = new CronParser("@hourly");
        
        Instant now = Instant.parse("2026-01-12T10:30:00Z");
        Instant next = cron.getNextRunTime(now, ZoneId.of("UTC"));
        
        assertEquals("2026-01-12T11:00:00Z", next.toString());
    }

    public void testAliasDaily() {
        CronParser cron = new CronParser("@daily");
        
        Instant now = Instant.parse("2026-01-12T10:30:00Z");
        Instant next = cron.getNextRunTime(now, ZoneId.of("UTC"));
        
        assertEquals("2026-01-13T00:00:00Z", next.toString());
    }

    public void testAliasMidnight() {
        CronParser cron = new CronParser("@midnight");
        
        Instant now = Instant.parse("2026-01-12T10:30:00Z");
        Instant next = cron.getNextRunTime(now, ZoneId.of("UTC"));
        
        assertEquals("2026-01-13T00:00:00Z", next.toString());
    }

    public void testAliasWeekly() {
        CronParser cron = new CronParser("@weekly");
        
        Instant now = Instant.parse("2026-01-12T10:30:00Z");
        Instant next = cron.getNextRunTime(now, ZoneId.of("UTC"));
        
        ZonedDateTime nextZdt = next.atZone(ZoneId.of("UTC"));
        assertEquals(java.time.DayOfWeek.SUNDAY, nextZdt.getDayOfWeek());
    }

    public void testAliasMonthly() {
        CronParser cron = new CronParser("@monthly");
        
        Instant now = Instant.parse("2026-01-12T10:30:00Z");
        Instant next = cron.getNextRunTime(now, ZoneId.of("UTC"));
        
        assertEquals("2026-02-01T00:00:00Z", next.toString());
    }

    public void testAliasYearly() {
        CronParser cron = new CronParser("@yearly");
        
        Instant now = Instant.parse("2026-01-12T10:30:00Z");
        Instant next = cron.getNextRunTime(now, ZoneId.of("UTC"));
        
        assertEquals("2027-01-01T00:00:00Z", next.toString());
    }

    // ==================== Timezone Support ====================

    public void testTimezoneNewYork() {
        CronParser cron = new CronParser("0 9 * * *");  // 9 AM
        
        Instant now = Instant.parse("2026-01-12T10:00:00Z");  // 5 AM in NY (EST)
        Instant next = cron.getNextRunTime(now, ZoneId.of("America/New_York"));
        
        // 9 AM in NY = 14:00 UTC
        ZonedDateTime nextNY = next.atZone(ZoneId.of("America/New_York"));
        assertEquals(9, nextNY.getHour());
    }

    public void testTimezoneTokyo() {
        CronParser cron = new CronParser("0 9 * * *");  // 9 AM
        
        Instant now = Instant.parse("2026-01-12T00:00:00Z");  // 9 AM in Tokyo (JST)
        Instant next = cron.getNextRunTime(now, ZoneId.of("Asia/Tokyo"));
        
        ZonedDateTime nextTokyo = next.atZone(ZoneId.of("Asia/Tokyo"));
        assertEquals(9, nextTokyo.getHour());
    }

    // ==================== Validation ====================

    public void testIsValidTrue() {
        assertTrue(CronParser.isValid("* * * * *"));
        assertTrue(CronParser.isValid("0 2 * * *"));
        assertTrue(CronParser.isValid("*/5 * * * *"));
        assertTrue(CronParser.isValid("@hourly"));
        assertTrue(CronParser.isValid("@daily"));
    }

    public void testIsValidFalse() {
        assertFalse(CronParser.isValid("invalid"));
        assertFalse(CronParser.isValid("* * *"));  // Too few fields
        assertFalse(CronParser.isValid("* * * * * *"));  // Too many fields
        assertFalse(CronParser.isValid("60 * * * *"));  // Invalid minute
        assertFalse(CronParser.isValid("* 25 * * *"));  // Invalid hour
    }

    // ==================== Invalid Expressions ====================

    public void testInvalidTooFewFields() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, 
            () -> new CronParser("* * *"));
        assertTrue(e.getMessage().contains("expected 5 fields"));
    }

    public void testInvalidTooManyFields() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, 
            () -> new CronParser("* * * * * *"));
        assertTrue(e.getMessage().contains("expected 5 fields"));
    }

    public void testInvalidMinute() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, 
            () -> new CronParser("60 * * * *"));
        assertTrue(e.getMessage().contains("out of range"));
    }

    public void testInvalidHour() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, 
            () -> new CronParser("* 24 * * *"));
        assertTrue(e.getMessage().contains("out of range"));
    }

    public void testInvalidDayOfMonth() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, 
            () -> new CronParser("* * 32 * *"));
        assertTrue(e.getMessage().contains("out of range"));
    }

    public void testInvalidMonth() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, 
            () -> new CronParser("* * * 13 *"));
        assertTrue(e.getMessage().contains("out of range"));
    }

    public void testInvalidDayOfWeek() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, 
            () -> new CronParser("* * * * 7"));
        assertTrue(e.getMessage().contains("out of range"));
    }

    // ==================== Description ====================

    public void testDescriptionEveryMinute() {
        CronParser cron = new CronParser("* * * * *");
        String desc = cron.getDescription();
        assertTrue(desc.contains("Every minute"));
    }

    public void testDescriptionEveryHour() {
        CronParser cron = new CronParser("0 * * * *");
        String desc = cron.getDescription();
        assertTrue(desc.contains("Every hour"));
    }

    public void testDescriptionSpecificTime() {
        CronParser cron = new CronParser("30 14 * * *");
        String desc = cron.getDescription();
        assertTrue(desc.contains("14:30"));
    }
}
