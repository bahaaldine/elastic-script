-- Create a new Elasticsearch serverless project
CREATE SKILL create_elasticsearch_project(
  IN api_key STRING COMMENT 'Elastic Cloud API key',
  IN project_name STRING COMMENT 'Name for the new project',
  IN region_id STRING DEFAULT 'aws-us-east-1' COMMENT 'Region ID (e.g., aws-us-east-1, gcp-us-central1)',
  IN alias STRING DEFAULT '' COMMENT 'Optional project alias'
)
RETURNS DOCUMENT
COMMENT 'Creates a new Elasticsearch serverless project and returns credentials'
BEGIN
  DECLARE config DOCUMENT;
  DECLARE result DOCUMENT;
  
  SET config = {};
  IF alias != '' THEN
    SET config = {'alias': alias};
  END IF;
  
  SET result = SERVERLESS_CREATE_ES_PROJECT(api_key, project_name, region_id, config);
  
  RETURN {
    'project_id': result.id,
    'name': result.name,
    'region': result.region_id,
    'endpoints': result.endpoints,
    'credentials': result.credentials,
    'status': 'created',
    'message': 'Project created successfully. Save your credentials - they will not be shown again!'
  };
END SKILL;
