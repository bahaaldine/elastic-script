-- Create an IP-based traffic filter for serverless projects
CREATE SKILL create_ip_traffic_filter(
  IN api_key STRING COMMENT 'Elastic Cloud API key',
  IN filter_name STRING COMMENT 'Name for the traffic filter',
  IN allowed_ips ARRAY COMMENT 'Array of allowed IP ranges (CIDR notation)',
  IN region_id STRING DEFAULT 'aws-us-east-1' COMMENT 'Region for the filter'
)
RETURNS DOCUMENT
COMMENT 'Creates an IP-based traffic filter to restrict access to serverless projects'
BEGIN
  DECLARE rules ARRAY;
  DECLARE rule DOCUMENT;
  DECLARE ip STRING;
  DECLARE result DOCUMENT;
  
  SET rules = [];
  
  FOR ip IN allowed_ips LOOP
    SET rule = {'source': ip};
    SET rules = ARRAY_APPEND(rules, rule);
  END LOOP;
  
  SET result = SERVERLESS_CREATE_TRAFFIC_FILTER(api_key, filter_name, 'ip', rules, region_id);
  
  RETURN {
    'filter_id': result.id,
    'name': result.name,
    'type': 'ip',
    'region': result.region,
    'rules_count': ARRAY_LENGTH(rules),
    'status': 'created',
    'message': 'Traffic filter created. Associate it with projects to enforce IP restrictions.'
  };
END SKILL;
