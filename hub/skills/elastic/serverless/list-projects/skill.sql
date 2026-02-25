-- List all serverless projects across all types (ES, Observability, Security)
CREATE SKILL list_serverless_projects(
  IN api_key STRING COMMENT 'Elastic Cloud API key'
)
RETURNS DOCUMENT
COMMENT 'Lists all serverless projects across Elasticsearch, Observability, and Security types'
BEGIN
  DECLARE es_projects ARRAY;
  DECLARE obs_projects ARRAY;
  DECLARE sec_projects ARRAY;
  
  SET es_projects = SERVERLESS_LIST_ES_PROJECTS(api_key);
  SET obs_projects = SERVERLESS_LIST_OBSERVABILITY_PROJECTS(api_key);
  SET sec_projects = SERVERLESS_LIST_SECURITY_PROJECTS(api_key);
  
  RETURN {
    'elasticsearch': es_projects,
    'observability': obs_projects,
    'security': sec_projects,
    'total_count': ARRAY_LENGTH(es_projects) + ARRAY_LENGTH(obs_projects) + ARRAY_LENGTH(sec_projects)
  };
END SKILL;
