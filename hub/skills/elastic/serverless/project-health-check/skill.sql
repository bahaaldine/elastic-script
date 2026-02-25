-- Comprehensive health check for all serverless projects
CREATE SKILL serverless_health_check(
  IN api_key STRING COMMENT 'Elastic Cloud API key'
)
RETURNS DOCUMENT
COMMENT 'Performs a health check on all serverless projects and returns status summary'
BEGIN
  DECLARE es_projects ARRAY;
  DECLARE obs_projects ARRAY;
  DECLARE sec_projects ARRAY;
  DECLARE project DOCUMENT;
  DECLARE status DOCUMENT;
  DECLARE healthy_count INTEGER;
  DECLARE warning_count INTEGER;
  DECLARE issues ARRAY;
  
  SET healthy_count = 0;
  SET warning_count = 0;
  SET issues = [];
  
  -- Check Elasticsearch projects
  SET es_projects = SERVERLESS_LIST_ES_PROJECTS(api_key);
  FOR project IN es_projects LOOP
    SET status = SERVERLESS_ES_PROJECT_STATUS(api_key, project.id);
    IF status.phase = 'running' OR status.phase = 'initializing' THEN
      SET healthy_count = healthy_count + 1;
    ELSE
      SET warning_count = warning_count + 1;
      SET issues = ARRAY_APPEND(issues, {
        'project_id': project.id,
        'name': project.name,
        'type': 'elasticsearch',
        'status': status.phase,
        'issue': 'Project not in running state'
      });
    END IF;
  END LOOP;
  
  -- Check Observability projects
  SET obs_projects = SERVERLESS_LIST_OBSERVABILITY_PROJECTS(api_key);
  FOR project IN obs_projects LOOP
    SET status = SERVERLESS_OBSERVABILITY_PROJECT_STATUS(api_key, project.id);
    IF status.phase = 'running' OR status.phase = 'initializing' THEN
      SET healthy_count = healthy_count + 1;
    ELSE
      SET warning_count = warning_count + 1;
      SET issues = ARRAY_APPEND(issues, {
        'project_id': project.id,
        'name': project.name,
        'type': 'observability',
        'status': status.phase,
        'issue': 'Project not in running state'
      });
    END IF;
  END LOOP;
  
  -- Check Security projects
  SET sec_projects = SERVERLESS_LIST_SECURITY_PROJECTS(api_key);
  FOR project IN sec_projects LOOP
    SET status = SERVERLESS_SECURITY_PROJECT_STATUS(api_key, project.id);
    IF status.phase = 'running' OR status.phase = 'initializing' THEN
      SET healthy_count = healthy_count + 1;
    ELSE
      SET warning_count = warning_count + 1;
      SET issues = ARRAY_APPEND(issues, {
        'project_id': project.id,
        'name': project.name,
        'type': 'security',
        'status': status.phase,
        'issue': 'Project not in running state'
      });
    END IF;
  END LOOP;
  
  RETURN {
    'total_projects': healthy_count + warning_count,
    'healthy': healthy_count,
    'warnings': warning_count,
    'overall_status': CASE WHEN warning_count = 0 THEN 'healthy' ELSE 'warning' END,
    'issues': issues,
    'breakdown': {
      'elasticsearch': ARRAY_LENGTH(es_projects),
      'observability': ARRAY_LENGTH(obs_projects),
      'security': ARRAY_LENGTH(sec_projects)
    }
  };
END SKILL;
