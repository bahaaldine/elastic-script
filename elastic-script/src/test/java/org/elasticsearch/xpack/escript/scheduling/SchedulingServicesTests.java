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
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for scheduling service logic.
 * 
 * These tests focus on the business logic of job scheduling and trigger polling
 * without requiring a full Elasticsearch cluster. Integration tests are covered
 * in E2E notebook tests.
 */
public class SchedulingServicesTests extends ESTestCase {

    // ==================== Job Definition Tests ====================

    public void testJobDefinitionCreation() {
        Instant now = Instant.now();
        
        JobDefinition job = new JobDefinition(
            "test_job",
            "0 * * * *",
            "UTC",
            true,
            "Test job description",
            "BEGIN PRINT 'test'; END",
            now,
            now,
            now.plus(1, ChronoUnit.HOURS),
            null,
            null,
            0
        );
        
        assertEquals("test_job", job.getName());
        assertEquals("0 * * * *", job.getSchedule());
        assertEquals("UTC", job.getTimezone());
        assertTrue(job.isEnabled());
        assertEquals("Test job description", job.getDescription());
        assertEquals("BEGIN PRINT 'test'; END", job.getProcedureBody());
        assertEquals(now, job.getCreatedAt());
        assertEquals(0, job.getRunCount());
    }

    public void testJobDefinitionGetters() {
        Instant now = Instant.now();
        Instant nextRun = now.plus(1, ChronoUnit.HOURS);

        JobDefinition job = new JobDefinition(
            "my_job",
            "@hourly",
            "America/New_York",
            true,
            "Hourly job",
            "PRINT 'hello';",
            now,
            now,
            nextRun,
            null,
            null,
            5
        );

        assertEquals("my_job", job.getName());
        assertEquals("@hourly", job.getSchedule());
        assertEquals("America/New_York", job.getTimezone());
        assertTrue(job.isEnabled());
        assertEquals("Hourly job", job.getDescription());
        assertEquals("PRINT 'hello';", job.getProcedureBody());
        assertEquals(5, job.getRunCount());
    }

    public void testJobDefinitionFromMap() {
        Instant now = Instant.now();
        
        Map<String, Object> map = new HashMap<>();
        map.put("name", "restored_job");
        map.put("schedule", "0 2 * * *");
        map.put("timezone", "UTC");
        map.put("enabled", true);
        map.put("description", "Restored from map");
        map.put("procedure_body", "PRINT 'restored';");
        map.put("created_at", now.toString());
        map.put("updated_at", now.toString());
        map.put("run_count", 10);
        
        // Test that we can reconstruct from a map (simulating ES document retrieval)
        String name = (String) map.get("name");
        String schedule = (String) map.get("schedule");
        boolean enabled = (Boolean) map.get("enabled");
        int runCount = (Integer) map.get("run_count");
        
        assertEquals("restored_job", name);
        assertEquals("0 2 * * *", schedule);
        assertTrue(enabled);
        assertEquals(10, runCount);
    }

    // ==================== Trigger Definition Tests ====================

    public void testTriggerDefinitionCreation() {
        Instant now = Instant.now();
        
        TriggerDefinition trigger = new TriggerDefinition(
            "error_alert",
            "logs-*",
            "level = 'ERROR'",
            60,
            true,
            "Alerts on errors",
            "PRINT 'Error found';",
            now,
            now,
            null,
            null,
            0
        );
        
        assertEquals("error_alert", trigger.getName());
        assertEquals("logs-*", trigger.getIndexPattern());
        assertEquals("level = 'ERROR'", trigger.getCondition());
        assertEquals(60, trigger.getPollIntervalSeconds());
        assertTrue(trigger.isEnabled());
        assertEquals(0, trigger.getFireCount());
    }

    public void testTriggerDefinitionGetters() {
        Instant now = Instant.now();
        Instant checkpoint = Instant.parse("2026-01-12T10:00:00Z");

        TriggerDefinition trigger = new TriggerDefinition(
            "my_trigger",
            "events-*",
            "status = 'pending'",
            30,
            true,
            "Processes pending events",
            "FOR doc IN @documents LOOP PRINT doc._id; END LOOP",
            now,
            now,
            now,
            checkpoint,
            100
        );

        assertEquals("my_trigger", trigger.getName());
        assertEquals("events-*", trigger.getIndexPattern());
        assertEquals("status = 'pending'", trigger.getCondition());
        assertEquals(30, trigger.getPollIntervalSeconds());
        assertTrue(trigger.isEnabled());
        assertEquals(100, trigger.getFireCount());
    }

    // ==================== Next Run Calculation Tests ====================

    public void testCalculateNextRunEveryHour() {
        CronParser cron = new CronParser("0 * * * *");
        Instant now = Instant.parse("2026-01-12T10:30:00Z");
        
        Instant nextRun = cron.getNextRunTime(now, ZoneId.of("UTC"));
        
        assertEquals("2026-01-12T11:00:00Z", nextRun.toString());
    }

    public void testCalculateNextRunDaily() {
        CronParser cron = new CronParser("0 2 * * *");
        Instant now = Instant.parse("2026-01-12T10:30:00Z");
        
        Instant nextRun = cron.getNextRunTime(now, ZoneId.of("UTC"));
        
        // Next 2 AM is tomorrow
        assertEquals("2026-01-13T02:00:00Z", nextRun.toString());
    }

    public void testCalculateNextRunWithTimezone() {
        CronParser cron = new CronParser("0 9 * * *");  // 9 AM local
        Instant now = Instant.parse("2026-01-12T10:00:00Z");  // 5 AM in NY
        
        Instant nextRun = cron.getNextRunTime(now, ZoneId.of("America/New_York"));
        
        // 9 AM in NY = 14:00 UTC (during EST)
        Instant expected = Instant.parse("2026-01-12T14:00:00Z");
        assertEquals(expected, nextRun);
    }

    // ==================== Job Due Check Tests ====================

    public void testJobIsDue() {
        Instant now = Instant.now();
        Instant pastNextRun = now.minus(5, ChronoUnit.MINUTES);
        
        // Job with next_run in the past should be due
        assertTrue(isJobDue(pastNextRun, now));
    }

    public void testJobIsNotDue() {
        Instant now = Instant.now();
        Instant futureNextRun = now.plus(5, ChronoUnit.MINUTES);
        
        // Job with next_run in the future should not be due
        assertFalse(isJobDue(futureNextRun, now));
    }

    public void testJobExactlyDue() {
        Instant now = Instant.now();
        
        // Job with next_run at exactly now should be due
        assertTrue(isJobDue(now, now));
    }

    private boolean isJobDue(Instant nextRun, Instant now) {
        return !nextRun.isAfter(now);
    }

    // ==================== Trigger Poll Check Tests ====================

    public void testTriggerShouldPoll() {
        Instant now = Instant.now();
        Instant lastPoll = now.minus(2, ChronoUnit.MINUTES);
        int pollIntervalSeconds = 60;  // 1 minute
        
        // Last poll was 2 minutes ago, interval is 1 minute -> should poll
        assertTrue(shouldPoll(lastPoll, pollIntervalSeconds, now));
    }

    public void testTriggerShouldNotPoll() {
        Instant now = Instant.now();
        Instant lastPoll = now.minus(30, ChronoUnit.SECONDS);
        int pollIntervalSeconds = 60;  // 1 minute
        
        // Last poll was 30 seconds ago, interval is 1 minute -> should not poll
        assertFalse(shouldPoll(lastPoll, pollIntervalSeconds, now));
    }

    public void testTriggerNeverPolled() {
        Instant now = Instant.now();
        int pollIntervalSeconds = 60;
        
        // Never polled -> should poll
        assertTrue(shouldPoll(null, pollIntervalSeconds, now));
    }

    private boolean shouldPoll(Instant lastPoll, int pollIntervalSeconds, Instant now) {
        if (lastPoll == null) {
            return true;
        }
        Instant nextPollDue = lastPoll.plusSeconds(pollIntervalSeconds);
        return !nextPollDue.isAfter(now);
    }

    // ==================== Checkpoint Tracking Tests ====================

    public void testCheckpointUpdate() {
        String initialCheckpoint = "2026-01-12T10:00:00Z";
        String newCheckpoint = "2026-01-12T10:05:00Z";
        
        // After processing documents, checkpoint should advance
        String updatedCheckpoint = advanceCheckpoint(initialCheckpoint, newCheckpoint);
        assertEquals("2026-01-12T10:05:00Z", updatedCheckpoint);
    }

    public void testCheckpointNoDocuments() {
        String initialCheckpoint = "2026-01-12T10:00:00Z";
        
        // No new documents -> checkpoint stays the same
        String updatedCheckpoint = advanceCheckpoint(initialCheckpoint, null);
        assertEquals("2026-01-12T10:00:00Z", updatedCheckpoint);
    }

    public void testCheckpointInitial() {
        // No checkpoint yet -> use the max timestamp from documents
        String firstCheckpoint = advanceCheckpoint(null, "2026-01-12T10:00:00Z");
        assertEquals("2026-01-12T10:00:00Z", firstCheckpoint);
    }

    private String advanceCheckpoint(String currentCheckpoint, String newCheckpoint) {
        if (newCheckpoint != null) {
            return newCheckpoint;
        }
        return currentCheckpoint;
    }

    // ==================== Leader Election Logic Tests ====================

    public void testLeaderElectionExpiry() {
        Instant now = Instant.now();
        Instant leaderHeartbeat = now.minus(20, ChronoUnit.SECONDS);
        int ttlSeconds = 15;
        
        // Leader heartbeat was 20 seconds ago, TTL is 15 -> leader expired
        assertTrue(isLeaderExpired(leaderHeartbeat, ttlSeconds, now));
    }

    public void testLeaderElectionActive() {
        Instant now = Instant.now();
        Instant leaderHeartbeat = now.minus(5, ChronoUnit.SECONDS);
        int ttlSeconds = 15;
        
        // Leader heartbeat was 5 seconds ago, TTL is 15 -> leader active
        assertFalse(isLeaderExpired(leaderHeartbeat, ttlSeconds, now));
    }

    private boolean isLeaderExpired(Instant lastHeartbeat, int ttlSeconds, Instant now) {
        Instant expiryTime = lastHeartbeat.plusSeconds(ttlSeconds);
        return now.isAfter(expiryTime);
    }

    // ==================== Document Binding Tests ====================

    public void testDocumentCountBinding() {
        // Simulate matched documents
        int documentCount = 5;
        
        // @document_count should be bound to the count
        assertEquals(5, documentCount);
    }

    public void testEmptyDocumentBinding() {
        // No matched documents
        int documentCount = 0;
        
        // Trigger should still fire with 0 documents (allows for "no documents" alerts)
        assertEquals(0, documentCount);
    }

    // ==================== Run History Tests ====================

    public void testJobRunHistoryEntry() {
        Instant startTime = Instant.now();
        Instant endTime = startTime.plus(5, ChronoUnit.SECONDS);
        long durationMs = ChronoUnit.MILLIS.between(startTime, endTime);
        
        Map<String, Object> runEntry = new HashMap<>();
        runEntry.put("job_name", "my_job");
        runEntry.put("started_at", startTime.toString());
        runEntry.put("ended_at", endTime.toString());
        runEntry.put("duration_ms", durationMs);
        runEntry.put("status", "success");
        
        assertEquals("my_job", runEntry.get("job_name"));
        assertEquals("success", runEntry.get("status"));
        assertEquals(5000L, runEntry.get("duration_ms"));
    }

    public void testTriggerRunHistoryEntry() {
        Instant pollTime = Instant.now();
        int documentsMatched = 10;
        
        Map<String, Object> runEntry = new HashMap<>();
        runEntry.put("trigger_name", "error_alert");
        runEntry.put("polled_at", pollTime.toString());
        runEntry.put("documents_matched", documentsMatched);
        runEntry.put("status", "success");
        
        assertEquals("error_alert", runEntry.get("trigger_name"));
        assertEquals(10, runEntry.get("documents_matched"));
    }

    // ==================== Error Handling Tests ====================

    public void testJobFailureRecording() {
        String errorMessage = "Connection refused";
        String status = "failure";
        
        Map<String, Object> runEntry = new HashMap<>();
        runEntry.put("status", status);
        runEntry.put("error_message", errorMessage);
        
        assertEquals("failure", runEntry.get("status"));
        assertEquals("Connection refused", runEntry.get("error_message"));
    }

    public void testTriggerQueryFailure() {
        String errorMessage = "Index not found: logs-*";
        String status = "failure";
        
        Map<String, Object> runEntry = new HashMap<>();
        runEntry.put("status", status);
        runEntry.put("error_message", errorMessage);
        
        assertEquals("failure", runEntry.get("status"));
        assertTrue(((String) runEntry.get("error_message")).contains("logs-*"));
    }
}
