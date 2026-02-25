-- Get status of a serverless project
CREATE SKILL get_serverless_project_status(
  IN api_key STRING COMMENT 'Elastic Cloud API key',
  IN project_id STRING COMMENT 'Project ID',
  IN project_type STRING DEFAULT 'elasticsearch' COMMENT 'Project type: elasticsearch, observability, or security'
)
RETURNS DOCUMENT
COMMENT 'Gets the current status of a serverless project'
BEGIN
  DECLARE status DOCUMENT;
  DECLARE project DOCUMENT;
  
  IF project_type = 'elasticsearch' THEN
    SET status = SERVERLESS_ES_PROJECT_STATUS(api_key, project_id);
    SET project = SERVERLESS_GET_ES_PROJECT(api_key, project_id);
  ELSEIF project_type = 'observability' THEN
    SET status = SERVERLESS_OBSERVABILITY_PROJECT_STATUS(api_key, project_id);
    SET project = SERVERLESS_GET_OBSERVABILITY_PROJECT(api_key, project_id);
  ELSEIF project_type = 'security' THEN
    SET status = SERVERLESS_SECURITY_PROJECT_STATUS(api_key, project_id);
    SET project = SERVERLESS_GET_SECURITY_PROJECT(api_key, project_id);
  ELSE
    RETURN {'error': 'Invalid project_type. Must be: elasticsearch, observability, or security'};
  END IF;
  
  RETURN {
    'project_id': project_id,
    'project_type': project_type,
    'name': project.name,
    'status': status,
    'endpoints': project.endpoints,
    'region': project.region_id
  };
END SKILL;
