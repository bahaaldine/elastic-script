-- Create a new Security serverless project (SIEM)
CREATE SKILL create_security_project(
  IN api_key STRING COMMENT 'Elastic Cloud API key',
  IN project_name STRING COMMENT 'Name for the new project',
  IN region_id STRING DEFAULT 'aws-us-east-1' COMMENT 'Region ID'
)
RETURNS DOCUMENT
COMMENT 'Creates a new Elastic Security serverless project for SIEM/threat detection'
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = SERVERLESS_CREATE_SECURITY_PROJECT(api_key, project_name, region_id, {});
  
  RETURN {
    'project_id': result.id,
    'name': result.name,
    'type': 'security',
    'region': result.region_id,
    'endpoints': result.endpoints,
    'credentials': result.credentials,
    'kibana_url': result.endpoints.kibana,
    'status': 'created',
    'message': 'Security project created. Access Kibana to configure SIEM rules and detections.'
  };
END SKILL;
