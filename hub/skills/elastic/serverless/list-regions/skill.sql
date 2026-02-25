-- List all available serverless regions
CREATE SKILL list_serverless_regions(
  IN api_key STRING COMMENT 'Elastic Cloud API key'
)
RETURNS ARRAY
COMMENT 'Lists all available regions for serverless projects with cloud provider details'
BEGIN
  DECLARE regions ARRAY;
  
  SET regions = SERVERLESS_LIST_REGIONS(api_key);
  
  RETURN regions;
END SKILL;
