CREATE SKILL what_can_i_do
  VERSION '1.0.0'
  DESCRIPTION 'Discover what Moltler skills are available for your data. Analyzes your cluster and recommends relevant skills based on what indices and data patterns exist.'
  AUTHOR 'elastic'
  TAGS ['meta', 'discovery', 'getting-started', 'recommendations']
  ()
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  DECLARE indices ARRAY;
  DECLARE categories DOCUMENT;
  DECLARE recommendations ARRAY;
  DECLARE available_skills ARRAY;
  
  -- Get all indices
  SET indices = ESQL_QUERY('SHOW INFO INDICES | KEEP name, status | LIMIT 100');
  
  -- Categorize indices by type
  DECLARE obs_indices ARRAY;
  DECLARE security_indices ARRAY;
  DECLARE apm_indices ARRAY;
  DECLARE search_indices ARRAY;
  
  SET obs_indices = [];
  SET security_indices = [];
  SET apm_indices = [];
  SET search_indices = [];
  
  DECLARE i INTEGER;
  FOR i IN 1..ARRAY_LENGTH(indices) LOOP
    DECLARE idx_name STRING;
    SET idx_name = indices[i].name;
    
    IF idx_name LIKE 'logs-%' OR idx_name LIKE 'metrics-%' OR idx_name LIKE 'filebeat-%' THEN
      SET obs_indices = ARRAY_APPEND(obs_indices, idx_name);
    ELSEIF idx_name LIKE '.alerts-security%' OR idx_name LIKE 'auditbeat-%' OR idx_name LIKE '.siem%' THEN
      SET security_indices = ARRAY_APPEND(security_indices, idx_name);
    ELSEIF idx_name LIKE 'apm-%' OR idx_name LIKE 'traces-%' THEN
      SET apm_indices = ARRAY_APPEND(apm_indices, idx_name);
    ELSEIF idx_name LIKE 'search-%' OR idx_name LIKE 'content-%' OR idx_name NOT LIKE '.%' THEN
      SET search_indices = ARRAY_APPEND(search_indices, idx_name);
    END IF;
  END LOOP;
  
  SET categories = {
    'observability': obs_indices,
    'security': security_indices,
    'apm': apm_indices,
    'search': search_indices
  };
  
  -- Build recommendations based on data
  SET recommendations = [];
  
  IF ARRAY_LENGTH(obs_indices) > 0 THEN
    SET recommendations = ARRAY_APPEND(recommendations, {
      'category': 'Observability',
      'icon': '📊',
      'message': 'You have ' || ARRAY_LENGTH(obs_indices) || ' log/metrics indices',
      'skills': [
        {'name': 'get_recent_errors', 'description': 'Find recent errors in your logs'},
        {'name': 'search_logs', 'description': 'Search across all your logs'},
        {'name': 'high_cpu_hosts', 'description': 'Find hosts with high CPU usage'},
        {'name': 'service_health', 'description': 'Check health of a service'}
      ],
      'quick_start': 'RUN SKILL get_recent_errors()'
    });
  END IF;
  
  IF ARRAY_LENGTH(security_indices) > 0 THEN
    SET recommendations = ARRAY_APPEND(recommendations, {
      'category': 'Security',
      'icon': '🛡️',
      'message': 'You have ' || ARRAY_LENGTH(security_indices) || ' security indices',
      'skills': [
        {'name': 'get_security_alerts', 'description': 'View active security alerts'},
        {'name': 'hunt_ioc', 'description': 'Hunt for indicators of compromise'},
        {'name': 'failed_logins', 'description': 'Find failed login attempts'},
        {'name': 'threat_summary', 'description': 'Get threat intelligence summary'}
      ],
      'quick_start': 'RUN SKILL get_security_alerts()'
    });
  END IF;
  
  IF ARRAY_LENGTH(apm_indices) > 0 THEN
    SET recommendations = ARRAY_APPEND(recommendations, {
      'category': 'APM',
      'icon': '⚡',
      'message': 'You have ' || ARRAY_LENGTH(apm_indices) || ' APM/trace indices',
      'skills': [
        {'name': 'list_services', 'description': 'List all monitored services'},
        {'name': 'get_slow_transactions', 'description': 'Find slow transactions'},
        {'name': 'get_error_groups', 'description': 'Group errors by type'},
        {'name': 'get_service_dependencies', 'description': 'See service dependencies'}
      ],
      'quick_start': 'RUN SKILL list_services()'
    });
  END IF;
  
  IF ARRAY_LENGTH(search_indices) > 0 THEN
    SET recommendations = ARRAY_APPEND(recommendations, {
      'category': 'Search',
      'icon': '🔍',
      'message': 'You have ' || ARRAY_LENGTH(search_indices) || ' content indices',
      'skills': [
        {'name': 'search_documents', 'description': 'Search your content'},
        {'name': 'semantic_search', 'description': 'Semantic/vector search'},
        {'name': 'get_field_stats', 'description': 'Analyze field statistics'},
        {'name': 'list_indices', 'description': 'List all indices'}
      ],
      'quick_start': 'RUN SKILL list_indices()'
    });
  END IF;
  
  -- Get total available skills
  SET available_skills = RUN SKILL list_all_skills();
  
  SET result = {
    'total_skills_available': ARRAY_LENGTH(available_skills),
    'your_data': categories,
    'recommendations': recommendations,
    'getting_started': 'Pick a skill from recommendations above and run it!',
    'need_help': 'RUN SKILL explain_skill(''skill_name'') to learn more about any skill'
  };
  
  RETURN result;
END SKILL;
