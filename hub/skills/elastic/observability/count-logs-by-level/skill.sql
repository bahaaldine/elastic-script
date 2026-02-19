CREATE SKILL count_logs_by_level
  VERSION '1.0.0'
  DESCRIPTION 'Count logs grouped by severity level (DEBUG, INFO, WARN, ERROR). Use this to get a quick overview of log distribution and identify if there are unusual spikes in errors or warnings.'
  AUTHOR 'elastic'
  TAGS ['observability', 'logs', 'analysis', 'aggregation']
  (
    index_pattern STRING DESCRIPTION 'Index pattern to search (e.g., logs-*, logs-production-*)' DEFAULT 'logs-*',
    time_range STRING DESCRIPTION 'Time range to analyze (e.g., 1h, 24h, 7d)' DEFAULT '24h'
  )
  RETURNS ARRAY
BEGIN
  DECLARE query STRING;
  DECLARE result ARRAY;
  
  SET query = 'FROM ' || index_pattern || ' | STATS count = COUNT(*) BY level | SORT count DESC';
  SET result = ESQL_QUERY(query);
  
  RETURN result;
END SKILL;
