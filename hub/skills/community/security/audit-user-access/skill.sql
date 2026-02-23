CREATE SKILL audit_user_access
  VERSION '1.0.0'
  DESCRIPTION 'Audit user access patterns and identify anomalies or policy violations.'
  AUTHOR 'security-team'
  TAGS ['security', 'audit', 'access', 'compliance']
  (
    user_name STRING DESCRIPTION 'Username to audit' DEFAULT NULL,
    time_range STRING DESCRIPTION 'Time range to audit' DEFAULT '24h'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE audit DOCUMENT;
  DECLARE access_logs ARRAY;
  DECLARE failed_logins ARRAY;
  DECLARE privilege_usage ARRAY;
  
  DECLARE query STRING;
  SET query = 'FROM logs-*,auditbeat-* | WHERE @timestamp > NOW() - INTERVAL ' || time_range;
  
  IF user_name IS NOT NULL THEN
    SET query = query || ' | WHERE user.name == "' || user_name || '"';
  END IF;
  
  -- Get access patterns
  SET access_logs = ESQL_QUERY(query || '
    | STATS access_count = COUNT(*), unique_hosts = COUNT_DISTINCT(host.name) BY user.name
    | SORT access_count DESC
    | LIMIT 20
  ');
  
  -- Get failed logins
  SET failed_logins = ESQL_QUERY(query || '
    | WHERE event.outcome == "failure" AND event.action == "authentication"
    | STATS failed_count = COUNT(*) BY user.name, source.ip
    | WHERE failed_count > 3
    | SORT failed_count DESC
  ');
  
  SET audit = {
    'user_name': user_name,
    'time_range': time_range,
    'access_summary': access_logs,
    'failed_logins': failed_logins,
    'has_anomalies': ARRAY_LENGTH(failed_logins) > 0,
    'audited_at': CURRENT_TIMESTAMP()
  };
  
  RETURN audit;
END SKILL;
