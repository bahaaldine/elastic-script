CREATE SKILL dedupe_alerts
  VERSION '1.0.0'
  DESCRIPTION 'Deduplicate similar alerts to reduce noise and alert fatigue.'
  AUTHOR 'sre-team'
  TAGS ['alerting', 'deduplication', 'noise-reduction']
  (
    time_window STRING DESCRIPTION 'Deduplication time window' DEFAULT '5m',
    group_by STRING DESCRIPTION 'Field to group alerts by' DEFAULT 'rule.name'
  )
  RETURNS ARRAY
BEGIN
  DECLARE deduplicated ARRAY;
  
  SET deduplicated = ESQL_QUERY('
    FROM .alerts-*
    | WHERE @timestamp > NOW() - INTERVAL ' || time_window || '
    | STATS 
        alert_count = COUNT(*),
        first_seen = MIN(@timestamp),
        last_seen = MAX(@timestamp),
        severities = VALUES(kibana.alert.severity)
      BY rule.name, service.name
    | WHERE alert_count > 1
    | SORT alert_count DESC
  ');
  
  RETURN deduplicated;
END SKILL;
