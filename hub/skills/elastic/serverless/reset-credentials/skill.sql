-- Reset credentials for a serverless project
CREATE SKILL reset_project_credentials(
  IN api_key STRING COMMENT 'Elastic Cloud API key',
  IN project_id STRING COMMENT 'Project ID',
  IN project_type STRING DEFAULT 'elasticsearch' COMMENT 'Project type: elasticsearch, observability, or security'
)
RETURNS DOCUMENT
COMMENT 'Resets and returns new credentials for a serverless project'
BEGIN
  DECLARE result DOCUMENT;
  
  IF project_type = 'elasticsearch' THEN
    SET result = SERVERLESS_RESET_ES_CREDENTIALS(api_key, project_id);
  ELSEIF project_type = 'observability' THEN
    SET result = SERVERLESS_RESET_OBSERVABILITY_CREDENTIALS(api_key, project_id);
  ELSEIF project_type = 'security' THEN
    SET result = SERVERLESS_RESET_SECURITY_CREDENTIALS(api_key, project_id);
  ELSE
    RETURN {'error': 'Invalid project_type. Must be: elasticsearch, observability, or security'};
  END IF;
  
  RETURN {
    'project_id': project_id,
    'project_type': project_type,
    'credentials': result.credentials,
    'status': 'reset',
    'message': 'Credentials reset successfully. Save your new credentials - they will not be shown again!'
  };
END SKILL;
