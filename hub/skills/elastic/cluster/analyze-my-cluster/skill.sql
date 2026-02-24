CREATE SKILL analyze_my_cluster
  VERSION '1.0.0'
  DESCRIPTION 'Analyze your Elasticsearch cluster and get actionable insights. Checks health, finds issues, and recommends skills to help.'
  AUTHOR 'elastic'
  TAGS ['cluster', 'analysis', 'health', 'getting-started', 'recommendations']
  ()
  RETURNS DOCUMENT
BEGIN
  DECLARE analysis DOCUMENT;
  DECLARE health DOCUMENT;
  DECLARE issues ARRAY;
  DECLARE quick_wins ARRAY;
  
  SET issues = [];
  SET quick_wins = [];
  
  -- Get cluster health
  SET health = RUN SKILL get_cluster_health();
  
  -- Check for cluster health issues
  IF health.status == 'red' THEN
    SET issues = ARRAY_APPEND(issues, {
      'severity': 'critical',
      'icon': '🔴',
      'issue': 'Cluster health is RED - some data may be unavailable',
      'action': 'RUN SKILL get_unassigned_shards()'
    });
  ELSEIF health.status == 'yellow' THEN
    SET issues = ARRAY_APPEND(issues, {
      'severity': 'warning',
      'icon': '🟡',
      'issue': 'Cluster health is YELLOW - replica shards unassigned',
      'action': 'RUN SKILL explain_allocation()'
    });
  END IF;
  
  -- Check for recent errors in logs
  DECLARE error_check ARRAY;
  SET error_check = ESQL_QUERY('
    FROM logs-*
    | WHERE @timestamp > NOW() - INTERVAL 1 HOUR
    | WHERE log.level == "ERROR" OR log.level == "FATAL"
    | STATS error_count = COUNT(*)
  ');
  
  IF ARRAY_LENGTH(error_check) > 0 AND error_check[0].error_count > 0 THEN
    SET quick_wins = ARRAY_APPEND(quick_wins, {
      'icon': '⚠️',
      'finding': error_check[0].error_count || ' errors in the last hour',
      'action': 'RUN SKILL get_recent_errors()',
      'priority': 'high'
    });
  END IF;
  
  -- Check for security alerts
  DECLARE alert_check ARRAY;
  SET alert_check = ESQL_QUERY('
    FROM .alerts-security.*
    | WHERE @timestamp > NOW() - INTERVAL 24 HOURS
    | WHERE kibana.alert.status == "active"
    | STATS alert_count = COUNT(*)
  ');
  
  IF ARRAY_LENGTH(alert_check) > 0 AND alert_check[0].alert_count > 0 THEN
    SET quick_wins = ARRAY_APPEND(quick_wins, {
      'icon': '🚨',
      'finding': alert_check[0].alert_count || ' active security alerts',
      'action': 'RUN SKILL get_security_alerts()',
      'priority': 'high'
    });
  END IF;
  
  -- Check for slow transactions
  DECLARE latency_check ARRAY;
  SET latency_check = ESQL_QUERY('
    FROM apm-*
    | WHERE @timestamp > NOW() - INTERVAL 1 HOUR
    | WHERE transaction.duration.us > 5000000
    | STATS slow_count = COUNT(*)
  ');
  
  IF ARRAY_LENGTH(latency_check) > 0 AND latency_check[0].slow_count > 0 THEN
    SET quick_wins = ARRAY_APPEND(quick_wins, {
      'icon': '🐢',
      'finding': latency_check[0].slow_count || ' slow transactions (>5s)',
      'action': 'RUN SKILL get_slow_transactions()',
      'priority': 'medium'
    });
  END IF;
  
  -- Check for failed logins
  DECLARE login_check ARRAY;
  SET login_check = ESQL_QUERY('
    FROM auditbeat-*,logs-*
    | WHERE @timestamp > NOW() - INTERVAL 24 HOURS
    | WHERE event.outcome == "failure" AND event.action == "authentication"
    | STATS failed_count = COUNT(*)
  ');
  
  IF ARRAY_LENGTH(login_check) > 0 AND login_check[0].failed_count > 10 THEN
    SET quick_wins = ARRAY_APPEND(quick_wins, {
      'icon': '🔐',
      'finding': login_check[0].failed_count || ' failed logins in 24h',
      'action': 'RUN SKILL failed_logins()',
      'priority': 'medium'
    });
  END IF;
  
  -- Get index summary
  DECLARE indices ARRAY;
  SET indices = ESQL_QUERY('SHOW INFO INDICES | STATS total_indices = COUNT(*)');
  
  SET analysis = {
    'cluster_health': health.status,
    'total_indices': indices[0].total_indices,
    'issues': issues,
    'quick_wins': quick_wins,
    'summary': CASE 
      WHEN ARRAY_LENGTH(issues) > 0 THEN 'Found ' || ARRAY_LENGTH(issues) || ' issues to address'
      WHEN ARRAY_LENGTH(quick_wins) > 0 THEN 'Cluster healthy, but found ' || ARRAY_LENGTH(quick_wins) || ' items to review'
      ELSE 'Cluster looks healthy! No immediate issues found.'
    END,
    'next_step': CASE
      WHEN ARRAY_LENGTH(issues) > 0 THEN issues[0].action
      WHEN ARRAY_LENGTH(quick_wins) > 0 THEN quick_wins[0].action
      ELSE 'RUN SKILL what_can_i_do() to explore available skills'
    END
  };
  
  RETURN analysis;
END SKILL;
