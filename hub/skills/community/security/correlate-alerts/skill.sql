CREATE SKILL correlate_alerts
  VERSION '1.0.0'
  DESCRIPTION 'Correlate security alerts across multiple sources to identify attack patterns.'
  AUTHOR 'security-team'
  TAGS ['security', 'correlation', 'threat-detection']
  (
    time_window STRING DESCRIPTION 'Correlation time window' DEFAULT '1h',
    min_alerts INTEGER DESCRIPTION 'Minimum alerts to correlate' DEFAULT 3
  )
  RETURNS ARRAY
BEGIN
  DECLARE correlations ARRAY;
  
  SET correlations = ESQL_QUERY('
    FROM .alerts-security.*
    | WHERE @timestamp > NOW() - INTERVAL ' || time_window || '
    | STATS 
        alert_count = COUNT(*),
        rule_names = VALUES(rule.name),
        tactics = VALUES(threat.tactic.name)
      BY host.name, user.name
    | WHERE alert_count >= ' || min_alerts || '
    | SORT alert_count DESC
    | LIMIT 20
  ');
  
  RETURN correlations;
END SKILL;
