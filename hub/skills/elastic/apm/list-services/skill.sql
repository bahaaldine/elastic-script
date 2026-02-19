CREATE SKILL list_services
  VERSION '1.0.0'
  DESCRIPTION 'List all APM-monitored services with their health status'
  AUTHOR 'elastic'
  TAGS ['apm,services,observability']
  (environment STRING DESCRIPTION 'Filter by environment' DEFAULT NULL)
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM traces-apm* | STATS count = COUNT(*) BY service.name | SORT count DESC | LIMIT 50');
  RETURN result;
END SKILL;
