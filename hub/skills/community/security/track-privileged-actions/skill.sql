CREATE SKILL track_privileged_actions
  VERSION '1.0.0'
  DESCRIPTION 'Track privileged user actions for security auditing.'
  AUTHOR 'security-team'
  TAGS ['security', 'audit', 'privileged', 'admin']
  (
    time_range STRING DESCRIPTION 'Time range to track' DEFAULT '24h',
    user_role STRING DESCRIPTION 'Role to track: admin, superuser' DEFAULT 'admin'
  )
  RETURNS ARRAY
BEGIN
  DECLARE actions ARRAY;
  
  SET actions = ESQL_QUERY('
    FROM auditbeat-*,logs-*
    | WHERE @timestamp > NOW() - INTERVAL ' || time_range || '
    | WHERE user.roles LIKE "*' || user_role || '*" OR user.name == "root" OR user.name == "admin"
    | STATS 
        action_count = COUNT(*),
        actions = VALUES(event.action)
      BY user.name, host.name
    | SORT action_count DESC
    | LIMIT 50
  ');
  
  RETURN actions;
END SKILL;
