CREATE SKILL what_can_i_do
  VERSION '1.0.0'
  DESCRIPTION 'Discover what Moltler skills are available.'
  AUTHOR 'elastic'
  TAGS ['meta', 'discovery']
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = {
    'message': 'Welcome to Moltler! Here are skill categories you can use:',
    'observability': 'get_recent_errors, search_logs, high_cpu_hosts, count_logs_by_level',
    'security': 'hunt_ioc, failed_logins, threat_summary, get_security_alerts',
    'search': 'search_documents, semantic_search, list_indices',
    'cluster': 'cluster_health_check, node_stats, shard_allocation',
    'apm': 'list_services, get_slow_transactions, error_groups',
    'alerting': 'list_rules, create_alert, list_connectors',
    'quick_start': 'Try: RUN SKILL get_recent_errors() or RUN SKILL cluster_health_check()',
    'help': 'Use: RUN SKILL explain_skill(skill_name) to learn about any skill'
  };
  
  RETURN result;
END SKILL;
