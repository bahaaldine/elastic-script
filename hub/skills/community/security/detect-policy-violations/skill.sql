CREATE SKILL detect_policy_violations
  VERSION '1.0.0'
  DESCRIPTION 'Detect security policy violations based on configured rules.'
  AUTHOR 'security-team'
  TAGS ['security', 'policy', 'compliance', 'violations']
  (
    policy_type STRING DESCRIPTION 'Type: access, data, network' DEFAULT 'access',
    time_range STRING DESCRIPTION 'Time range to check' DEFAULT '24h'
  )
  RETURNS ARRAY
BEGIN
  DECLARE violations ARRAY;
  
  -- Detect various policy violations
  SET violations = ESQL_QUERY('
    FROM logs-*,.alerts-security.*
    | WHERE @timestamp > NOW() - INTERVAL ' || time_range || '
    | WHERE event.outcome == "failure" OR kibana.alert.severity IN ("high", "critical")
    | STATS 
        violation_count = COUNT(*),
        first_seen = MIN(@timestamp),
        last_seen = MAX(@timestamp)
      BY user.name, rule.name
    | WHERE violation_count > 0
    | SORT violation_count DESC
    | LIMIT 50
  ');
  
  RETURN violations;
END SKILL;
