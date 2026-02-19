CREATE SKILL logs_by_service
  VERSION '1.0.0'
  DESCRIPTION 'Get log volume breakdown by service. Use this to understand which services are generating the most logs, identify noisy services, or detect services with unusual activity.'
  AUTHOR 'elastic'
  TAGS ['observability', 'logs', 'services', 'aggregation']
  (
    index_pattern STRING DESCRIPTION 'Index pattern to search' DEFAULT 'logs-*'
  )
  RETURNS ARRAY
BEGIN
  DECLARE query STRING;
  DECLARE result ARRAY;
  
  SET query = 'FROM ' || index_pattern || ' | STATS count = COUNT(*) BY service | SORT count DESC | LIMIT 20';
  SET result = ESQL_QUERY(query);
  
  RETURN result;
END SKILL;
