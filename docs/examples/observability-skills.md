# Observability Skills

Real-world examples of observability skills for monitoring and alerting.

## Log Analysis Skills

### Analyze Error Patterns

```sql
CREATE SKILL analyze_error_patterns
VERSION '1.0.0'
DESCRIPTION 'Identifies recurring error patterns in logs'
PARAMETERS (
    index_pattern STRING DEFAULT 'logs-*',
    time_range STRING DEFAULT '24h',
    min_occurrences NUMBER DEFAULT 10
)
RETURNS DOCUMENT
BEGIN
    DECLARE errors CURSOR FOR
        FROM index_pattern
        | WHERE log.level == 'ERROR'
        | WHERE @timestamp > NOW() - INTERVAL time_range
        | STATS count = COUNT(*), 
                first_seen = MIN(@timestamp),
                last_seen = MAX(@timestamp)
          BY error.type, error.message
        | WHERE count >= min_occurrences
        | SORT count DESC
        | LIMIT 20;
    
    DECLARE patterns ARRAY = [];
    
    OPEN errors;
    FOR e IN errors LOOP
        SET patterns = ARRAY_APPEND(patterns, {
            "type": e.error.type,
            "message": SUBSTR(e.error.message, 1, 200),
            "count": e.count,
            "first_seen": e.first_seen,
            "last_seen": e.last_seen,
            "frequency_per_hour": ROUND(e.count * 3600 / 
                DATE_DIFF('second', e.first_seen, e.last_seen))
        });
    END LOOP;
    CLOSE errors;
    
    RETURN {
        "analyzed_at": CURRENT_TIMESTAMP(),
        "time_range": time_range,
        "total_patterns": ARRAY_LENGTH(patterns),
        "patterns": patterns
    };
END SKILL;

-- Test
TEST SKILL analyze_error_patterns
WITH index_pattern = 'logs-*', time_range = '1h'
EXPECT total_patterns >= 0;
```

### Detect Log Anomalies

```sql
CREATE SKILL detect_log_anomalies
VERSION '1.0.0'
DESCRIPTION 'Detects unusual log patterns using statistical analysis'
PARAMETERS (
    index_pattern STRING DEFAULT 'logs-*',
    baseline_hours NUMBER DEFAULT 24,
    threshold_stddev NUMBER DEFAULT 3
)
RETURNS ARRAY
BEGIN
    -- Get baseline
    DECLARE baseline CURSOR FOR
        FROM index_pattern
        | WHERE @timestamp > NOW() - INTERVAL baseline_hours || 'h'
        | WHERE @timestamp < NOW() - INTERVAL '1h'
        | EVAL hour = DATE_TRUNC('hour', @timestamp)
        | STATS count = COUNT(*) BY hour, log.level
        | STATS avg_count = AVG(count), 
                stddev_count = STDDEV(count) 
          BY log.level;
    
    -- Get current hour
    DECLARE current CURSOR FOR
        FROM index_pattern
        | WHERE @timestamp > NOW() - INTERVAL '1h'
        | STATS count = COUNT(*) BY log.level;
    
    DECLARE anomalies ARRAY = [];
    DECLARE baselines = {};
    
    OPEN baseline;
    FOR b IN baseline LOOP
        SET baselines[b.log.level] = {
            "avg": b.avg_count,
            "stddev": b.stddev_count
        };
    END LOOP;
    CLOSE baseline;
    
    OPEN current;
    FOR c IN current LOOP
        DECLARE base = baselines[c.log.level];
        IF base IS NOT NULL THEN
            DECLARE deviation = ABS(c.count - base.avg) / 
                               CASE WHEN base.stddev > 0 THEN base.stddev ELSE 1 END;
            
            IF deviation > threshold_stddev THEN
                SET anomalies = ARRAY_APPEND(anomalies, {
                    "level": c.log.level,
                    "current_count": c.count,
                    "expected_count": ROUND(base.avg),
                    "deviation_stddev": ROUND(deviation, 2),
                    "direction": CASE WHEN c.count > base.avg 
                                 THEN 'increase' ELSE 'decrease' END
                });
            END IF;
        END IF;
    END LOOP;
    CLOSE current;
    
    RETURN anomalies;
END SKILL;
```

---

## Metric Analysis Skills

### Check Service SLOs

```sql
CREATE SKILL check_service_slos
VERSION '1.0.0'
DESCRIPTION 'Checks if services are meeting their SLOs'
PARAMETERS (
    services ARRAY DEFAULT ['api', 'web', 'worker'],
    availability_target NUMBER DEFAULT 99.9,
    latency_p99_target_ms NUMBER DEFAULT 500
)
RETURNS DOCUMENT
BEGIN
    DECLARE results ARRAY = [];
    
    FOR service IN services LOOP
        DECLARE metrics CURSOR FOR
            FROM metrics-apm-*
            | WHERE service.name == service
            | WHERE @timestamp > NOW() - INTERVAL '1h'
            | STATS 
                total = COUNT(*),
                errors = SUM(CASE WHEN event.outcome == 'failure' THEN 1 ELSE 0 END),
                p99_latency = PERCENTILE(transaction.duration.us, 99);
        
        OPEN metrics;
        DECLARE m;
        FETCH metrics INTO m;
        CLOSE metrics;
        
        IF m IS NOT NULL AND m.total > 0 THEN
            DECLARE availability = (m.total - m.errors) * 100.0 / m.total;
            DECLARE p99_ms = m.p99_latency / 1000;
            
            SET results = ARRAY_APPEND(results, {
                "service": service,
                "availability": ROUND(availability, 2),
                "availability_target": availability_target,
                "availability_met": availability >= availability_target,
                "latency_p99_ms": ROUND(p99_ms, 2),
                "latency_target_ms": latency_p99_target_ms,
                "latency_met": p99_ms <= latency_p99_target_ms,
                "slo_met": availability >= availability_target 
                          AND p99_ms <= latency_p99_target_ms
            });
        END IF;
    END LOOP;
    
    DECLARE all_met = ARRAY_EVERY(results, r => r.slo_met);
    
    RETURN {
        "checked_at": CURRENT_TIMESTAMP(),
        "overall_status": CASE WHEN all_met THEN 'healthy' ELSE 'degraded' END,
        "services": results,
        "services_meeting_slo": ARRAY_LENGTH(ARRAY_FILTER(results, r => r.slo_met)),
        "total_services": ARRAY_LENGTH(results)
    };
END SKILL;
```

### Monitor Resource Usage

```sql
CREATE SKILL monitor_resource_usage
VERSION '1.0.0'
DESCRIPTION 'Monitors CPU, memory, and disk usage'
PARAMETERS (
    host_pattern STRING DEFAULT '*',
    cpu_threshold NUMBER DEFAULT 80,
    memory_threshold NUMBER DEFAULT 85,
    disk_threshold NUMBER DEFAULT 90
)
RETURNS DOCUMENT
BEGIN
    DECLARE resources CURSOR FOR
        FROM metrics-system-*
        | WHERE host.name LIKE host_pattern
        | WHERE @timestamp > NOW() - INTERVAL '5m'
        | STATS 
            cpu = AVG(system.cpu.total.pct) * 100,
            memory = AVG(system.memory.used.pct) * 100,
            disk = MAX(system.filesystem.used.pct) * 100
          BY host.name;
    
    DECLARE hosts ARRAY = [];
    DECLARE alerts ARRAY = [];
    
    OPEN resources;
    FOR r IN resources LOOP
        DECLARE host_status = {
            "host": r.host.name,
            "cpu_pct": ROUND(r.cpu, 1),
            "memory_pct": ROUND(r.memory, 1),
            "disk_pct": ROUND(r.disk, 1),
            "issues": []
        };
        
        IF r.cpu > cpu_threshold THEN
            SET host_status.issues = ARRAY_APPEND(host_status.issues, 
                'CPU: ' || ROUND(r.cpu) || '%');
        END IF;
        
        IF r.memory > memory_threshold THEN
            SET host_status.issues = ARRAY_APPEND(host_status.issues,
                'Memory: ' || ROUND(r.memory) || '%');
        END IF;
        
        IF r.disk > disk_threshold THEN
            SET host_status.issues = ARRAY_APPEND(host_status.issues,
                'Disk: ' || ROUND(r.disk) || '%');
        END IF;
        
        SET hosts = ARRAY_APPEND(hosts, host_status);
        
        IF ARRAY_LENGTH(host_status.issues) > 0 THEN
            SET alerts = ARRAY_APPEND(alerts, host_status);
        END IF;
    END LOOP;
    CLOSE resources;
    
    RETURN {
        "checked_at": CURRENT_TIMESTAMP(),
        "total_hosts": ARRAY_LENGTH(hosts),
        "hosts_with_issues": ARRAY_LENGTH(alerts),
        "status": CASE WHEN ARRAY_LENGTH(alerts) = 0 
                  THEN 'healthy' ELSE 'warning' END,
        "alerts": alerts,
        "all_hosts": hosts
    };
END SKILL;
```

---

## Alert Management Skills

### Create Alert Summary

```sql
CREATE SKILL create_alert_summary
VERSION '1.0.0'
DESCRIPTION 'Creates a summary of current alerts'
PARAMETERS (
    time_range STRING DEFAULT '1h'
)
RETURNS DOCUMENT
BEGIN
    DECLARE alerts CURSOR FOR
        FROM alerts-*
        | WHERE @timestamp > NOW() - INTERVAL time_range
        | WHERE alert.status == 'active'
        | STATS count = COUNT(*) BY alert.severity, alert.name
        | SORT alert.severity, count DESC;
    
    DECLARE summary = {
        "critical": [],
        "high": [],
        "medium": [],
        "low": []
    };
    DECLARE total = 0;
    
    OPEN alerts;
    FOR a IN alerts LOOP
        SET summary[a.alert.severity] = ARRAY_APPEND(
            summary[a.alert.severity],
            {"name": a.alert.name, "count": a.count}
        );
        SET total = total + a.count;
    END LOOP;
    CLOSE alerts;
    
    RETURN {
        "generated_at": CURRENT_TIMESTAMP(),
        "time_range": time_range,
        "total_alerts": total,
        "by_severity": {
            "critical": ARRAY_LENGTH(summary.critical),
            "high": ARRAY_LENGTH(summary.high),
            "medium": ARRAY_LENGTH(summary.medium),
            "low": ARRAY_LENGTH(summary.low)
        },
        "details": summary
    };
END SKILL;
```

### Deduplicate Alerts

```sql
CREATE SKILL deduplicate_alerts
VERSION '1.0.0'
DESCRIPTION 'Groups related alerts to reduce noise'
PARAMETERS (
    time_window STRING DEFAULT '5m',
    similarity_threshold NUMBER DEFAULT 0.8
)
RETURNS ARRAY
BEGIN
    DECLARE alerts CURSOR FOR
        FROM alerts-*
        | WHERE @timestamp > NOW() - INTERVAL time_window
        | WHERE alert.status == 'active'
        | SORT @timestamp DESC;
    
    DECLARE groups ARRAY = [];
    DECLARE seen = {};
    
    OPEN alerts;
    FOR a IN alerts LOOP
        -- Simple grouping by alert name
        DECLARE key = a.alert.name || ':' || a.host.name;
        
        IF NOT DOCUMENT_CONTAINS(seen, key) THEN
            SET seen[key] = TRUE;
            
            -- Count similar alerts
            DECLARE similar_count = 0;
            -- (In practice, use more sophisticated similarity)
            
            SET groups = ARRAY_APPEND(groups, {
                "representative": a,
                "group_size": similar_count + 1,
                "first_occurrence": a.@timestamp
            });
        END IF;
    END LOOP;
    CLOSE alerts;
    
    RETURN groups;
END SKILL;
```

---

## Reporting Skills

### Generate Daily Report

```sql
CREATE SKILL generate_daily_report
VERSION '1.0.0'
DESCRIPTION 'Generates a daily observability report'
RETURNS DOCUMENT
BEGIN
    -- Get SLO status
    DECLARE slos = CALL check_service_slos();
    
    -- Get resource usage
    DECLARE resources = CALL monitor_resource_usage();
    
    -- Get error patterns
    DECLARE errors = CALL analyze_error_patterns(time_range => '24h');
    
    -- Get alert summary
    DECLARE alerts = CALL create_alert_summary(time_range => '24h');
    
    DECLARE report = {
        "report_date": CURRENT_DATE(),
        "generated_at": CURRENT_TIMESTAMP(),
        
        "executive_summary": {
            "overall_status": CASE 
                WHEN slos.overall_status = 'healthy' 
                     AND resources.status = 'healthy'
                     AND alerts.by_severity.critical = 0
                THEN 'healthy'
                WHEN alerts.by_severity.critical > 0
                THEN 'critical'
                ELSE 'degraded'
            END,
            "services_meeting_slo": slos.services_meeting_slo || '/' || slos.total_services,
            "hosts_with_issues": resources.hosts_with_issues,
            "total_alerts": alerts.total_alerts,
            "critical_alerts": alerts.by_severity.critical
        },
        
        "slo_compliance": slos,
        "resource_usage": resources,
        "error_patterns": errors,
        "alert_summary": alerts
    };
    
    RETURN report;
END SKILL;
```

---

## Skill Pack

### Observability Essentials Pack

```sql
CREATE SKILL PACK observability_essentials
VERSION '1.0.0'
DESCRIPTION 'Essential observability skills for any team'
AUTHOR 'moltler-team'
SKILLS [
    analyze_error_patterns@1.0.0,
    detect_log_anomalies@1.0.0,
    check_service_slos@1.0.0,
    monitor_resource_usage@1.0.0,
    create_alert_summary@1.0.0,
    deduplicate_alerts@1.0.0,
    generate_daily_report@1.0.0
];
```

---

## What's Next?

<div class="grid cards" markdown>

-   :material-fire:{ .lg .middle } __Incident Response__

    Skills for handling incidents.

    [:octicons-arrow-right-24: Incident Response](incident-response.md)

-   :material-pipe:{ .lg .middle } __Data Pipelines__

    Skills for data processing.

    [:octicons-arrow-right-24: Data Pipelines](data-pipeline.md)

</div>
