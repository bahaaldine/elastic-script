# Incident Response Examples

Automated incident detection, notification, and remediation with elastic-script.

## Basic Alert Handler

Detect and notify on critical errors:

```sql
CREATE PROCEDURE check_critical_errors()
BEGIN
    -- Check for critical errors in last 5 minutes
    DECLARE errors CURSOR FOR ESQL_QUERY('
        FROM logs-*
        | WHERE level = "ERROR" AND @timestamp > NOW() - 5 minutes
        | STATS count = COUNT(*) BY service.name
        | WHERE count > 10
    ');
    
    FOR svc IN errors LOOP
        DECLARE service STRING = DOCUMENT_GET(svc, 'service.name');
        DECLARE count NUMBER = DOCUMENT_GET(svc, 'count');
        
        PRINT '🚨 ALERT: ' || service || ' has ' || count || ' errors';
        
        -- Notify via Slack
        SLACK_SEND('#alerts', '🚨 *' || service || '* has ' || count || ' errors in the last 5 minutes');
    END LOOP;
END PROCEDURE;
```

---

## Full Incident Workflow

Complete incident lifecycle management:

```sql
CREATE PROCEDURE handle_incident(service STRING, description STRING, severity STRING)
BEGIN
    DECLARE incident_id STRING = 'INC-' || SUBSTR(CURRENT_TIMESTAMP(), 1, 10);
    
    PRINT '=== Creating Incident: ' || incident_id || ' ===';
    
    -- 1. Log the incident
    DECLARE incident DOCUMENT = {
        "incident_id": incident_id,
        "service": service,
        "description": description,
        "severity": severity,
        "status": "open",
        "created_at": CURRENT_TIMESTAMP()
    };
    ES_INDEX('incidents', incident_id, incident);
    
    -- 2. Create PagerDuty incident
    DECLARE pd_key STRING;
    TRY
        SET pd_key = PAGERDUTY_CREATE_INCIDENT(
            '[' || severity || '] ' || service || ': ' || description,
            severity,
            service
        );
        PRINT 'PagerDuty incident created: ' || pd_key;
    CATCH error
        PRINT 'Warning: PagerDuty notification failed: ' || error;
    END TRY;
    
    -- 3. Notify Slack
    DECLARE slack_message STRING = '🚨 *New Incident: ' || incident_id || '*\n'
        || '• Service: ' || service || '\n'
        || '• Severity: ' || severity || '\n'
        || '• Description: ' || description;
    
    TRY
        SLACK_SEND('#incidents', slack_message);
    CATCH error
        PRINT 'Warning: Slack notification failed: ' || error;
    END TRY;
    
    -- 4. Gather context
    PRINT 'Gathering incident context...';
    DECLARE recent_errors CURSOR FOR ESQL_QUERY('
        FROM logs-*
        | WHERE service.name = "' || service || '" AND level = "ERROR"
        | SORT @timestamp DESC
        | LIMIT 10
    ');
    
    DECLARE context_messages ARRAY = [];
    FOR error IN recent_errors LOOP
        SET context_messages = ARRAY_APPEND(context_messages, 
            DOCUMENT_GET(error, '@timestamp') || ': ' || DOCUMENT_GET(error, 'message'));
    END LOOP;
    
    -- 5. Update incident with context
    ES_UPDATE('incidents', incident_id, {
        "pagerduty_key": pd_key,
        "context": {
            "recent_errors": context_messages
        }
    });
    
    RETURN incident_id;
END PROCEDURE;
```

---

## Auto-Remediation

Automated recovery actions:

```sql
CREATE PROCEDURE auto_remediate(service STRING, issue_type STRING)
BEGIN
    PRINT '=== Auto-Remediation for ' || service || ' ===';
    PRINT 'Issue type: ' || issue_type;
    
    DECLARE action_taken STRING = 'none';
    DECLARE success BOOLEAN = FALSE;
    
    TRY
        IF issue_type = 'high_memory' THEN
            -- Restart pods with high memory
            PRINT 'Action: Restarting deployment...';
            K8S_RESTART_DEPLOYMENT('production', service);
            SET action_taken = 'deployment_restart';
            SET success = TRUE;
            
        ELSEIF issue_type = 'high_load' THEN
            -- Scale up the service
            PRINT 'Action: Scaling up deployment...';
            DECLARE current_pods ARRAY = K8S_GET_PODS('production');
            DECLARE current_count NUMBER = ARRAY_LENGTH(current_pods);
            DECLARE new_count NUMBER = current_count + 2;
            
            K8S_SCALE_DEPLOYMENT('production', service, new_count);
            SET action_taken = 'scale_up_to_' || new_count;
            SET success = TRUE;
            
        ELSEIF issue_type = 'connection_pool_exhausted' THEN
            -- Restart to reset connections
            PRINT 'Action: Rolling restart to reset connections...';
            K8S_RESTART_DEPLOYMENT('production', service);
            SET action_taken = 'connection_reset';
            SET success = TRUE;
            
        ELSE
            PRINT 'No automatic remediation available for: ' || issue_type;
            SET action_taken = 'manual_required';
        END IF;
        
    CATCH error
        PRINT 'Remediation failed: ' || error;
        SET action_taken = 'failed: ' || error;
    END TRY;
    
    -- Log remediation attempt
    DECLARE log DOCUMENT = {
        "service": service,
        "issue_type": issue_type,
        "action_taken": action_taken,
        "success": success,
        "timestamp": CURRENT_TIMESTAMP()
    };
    ES_INDEX('remediation-logs', NULL, log);
    
    -- Notify team
    IF success THEN
        SLACK_SEND('#incidents', '✅ Auto-remediation for *' || service || '*: ' || action_taken);
    ELSE
        SLACK_SEND('#incidents', '❌ Auto-remediation failed for *' || service || '* - manual intervention required');
    END IF;
    
    RETURN action_taken;
END PROCEDURE;
```

---

## Escalation Workflow

Progressive escalation based on severity and time:

```sql
CREATE PROCEDURE escalate_incident(incident_id STRING)
BEGIN
    -- Get incident details
    DECLARE incident DOCUMENT = ES_GET('incidents', incident_id);
    DECLARE status STRING = DOCUMENT_GET(incident, 'status');
    DECLARE severity STRING = DOCUMENT_GET(incident, 'severity');
    DECLARE created DATE = DOCUMENT_GET(incident, 'created_at');
    DECLARE escalation_level NUMBER = DOCUMENT_GET(incident, 'escalation_level');
    
    IF escalation_level = NULL THEN
        SET escalation_level = 0;
    END IF;
    
    IF status = 'resolved' THEN
        PRINT 'Incident already resolved, no escalation needed';
        RETURN 'resolved';
    END IF;
    
    -- Calculate age in minutes
    DECLARE age_minutes NUMBER = DATE_DIFF(CURRENT_TIMESTAMP(), created) * 24 * 60;
    
    PRINT 'Incident ' || incident_id || ' age: ' || age_minutes || ' minutes';
    PRINT 'Current escalation level: ' || escalation_level;
    
    -- Determine if escalation is needed
    DECLARE escalate BOOLEAN = FALSE;
    
    IF severity = 'critical' AND age_minutes > 15 AND escalation_level < 3 THEN
        SET escalate = TRUE;
    ELSEIF severity = 'high' AND age_minutes > 30 AND escalation_level < 2 THEN
        SET escalate = TRUE;
    ELSEIF severity = 'medium' AND age_minutes > 60 AND escalation_level < 1 THEN
        SET escalate = TRUE;
    END IF;
    
    IF escalate THEN
        SET escalation_level = escalation_level + 1;
        
        -- Notify escalation
        DECLARE escalation_message STRING = '⬆️ *Escalation Level ' || escalation_level || '*\n'
            || 'Incident: ' || incident_id || '\n'
            || 'Severity: ' || severity || '\n'
            || 'Age: ' || age_minutes || ' minutes\n'
            || 'Awaiting response...';
        
        -- Different channels for different levels
        IF escalation_level = 1 THEN
            SLACK_SEND('#oncall-primary', escalation_message);
        ELSEIF escalation_level = 2 THEN
            SLACK_SEND('#oncall-secondary', escalation_message);
            SLACK_SEND('#engineering-leads', escalation_message);
        ELSE
            SLACK_SEND('#incidents-critical', escalation_message);
            SLACK_SEND('#leadership', escalation_message);
        END IF;
        
        -- Update incident
        ES_UPDATE('incidents', incident_id, {
            "escalation_level": escalation_level,
            "last_escalation": CURRENT_TIMESTAMP()
        });
        
        PRINT 'Escalated to level ' || escalation_level;
    ELSE
        PRINT 'No escalation needed at this time';
    END IF;
    
    RETURN escalation_level;
END PROCEDURE;
```

---

## Post-Incident Report Generation

Generate incident summary with AI:

```sql
CREATE PROCEDURE generate_postmortem(incident_id STRING)
BEGIN
    -- Get incident data
    DECLARE incident DOCUMENT = ES_GET('incidents', incident_id);
    DECLARE service STRING = DOCUMENT_GET(incident, 'service');
    DECLARE description STRING = DOCUMENT_GET(incident, 'description');
    DECLARE created DATE = DOCUMENT_GET(incident, 'created_at');
    DECLARE resolved DATE = DOCUMENT_GET(incident, 'resolved_at');
    
    -- Get related logs
    DECLARE logs CURSOR FOR ESQL_QUERY('
        FROM logs-*
        | WHERE service.name = "' || service || '"
          AND @timestamp >= "' || created || '"
          AND @timestamp <= "' || resolved || '"
          AND level IN ("ERROR", "WARN")
        | SORT @timestamp
        | LIMIT 50
    ');
    
    DECLARE log_entries ARRAY = [];
    FOR log IN logs LOOP
        DECLARE entry STRING = DOCUMENT_GET(log, '@timestamp') 
            || ' [' || DOCUMENT_GET(log, 'level') || '] '
            || DOCUMENT_GET(log, 'message');
        SET log_entries = ARRAY_APPEND(log_entries, entry);
    END LOOP;
    
    -- Generate postmortem with AI
    DECLARE prompt STRING = 'Generate a post-incident report (postmortem) for this incident:

Incident ID: ' || incident_id || '
Service: ' || service || '
Description: ' || description || '
Start Time: ' || created || '
End Time: ' || resolved || '

Related log entries:
' || ARRAY_JOIN(log_entries, '\n') || '

Please include:
1. Executive Summary
2. Timeline of Events  
3. Root Cause Analysis
4. Impact Assessment
5. Action Items for Prevention

Format as Markdown.';
    
    DECLARE postmortem STRING = LLM_COMPLETE(prompt);
    
    -- Store postmortem
    ES_UPDATE('incidents', incident_id, {
        "postmortem": postmortem,
        "postmortem_generated_at": CURRENT_TIMESTAMP()
    });
    
    PRINT '=== Post-Incident Report ===';
    PRINT postmortem;
    
    RETURN postmortem;
END PROCEDURE;
```

---

## Usage

```sql
-- Create an incident
CALL handle_incident('api-gateway', 'Connection timeout errors spiking', 'high');

-- Attempt auto-remediation
CALL auto_remediate('api-gateway', 'connection_pool_exhausted');

-- Check for escalation (run periodically)
CALL escalate_incident('INC-2026-01-09');

-- Generate post-incident report
CALL generate_postmortem('INC-2026-01-09');
```
