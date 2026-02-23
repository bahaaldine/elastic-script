CREATE SKILL correlate_logs
  VERSION '1.0.0'
  DESCRIPTION 'Correlate logs across multiple services using trace IDs or request IDs.'
  AUTHOR 'sre-team'
  TAGS ['observability', 'logs', 'correlation', 'distributed-tracing']
  (
    trace_id STRING DESCRIPTION 'Trace ID to correlate' DEFAULT NULL,
    time_range STRING DESCRIPTION 'Time range to search' DEFAULT '1h',
    service_names ARRAY DESCRIPTION 'Services to include' DEFAULT NULL
  )
  RETURNS ARRAY
BEGIN
  DECLARE correlated ARRAY;
  DECLARE query STRING;
  
  SET query = 'FROM logs-* | WHERE @timestamp > NOW() - INTERVAL ' || time_range;
  
  IF trace_id IS NOT NULL THEN
    SET query = query || ' | WHERE trace.id == "' || trace_id || '"';
  END IF;
  
  SET correlated = ESQL_QUERY(query || '
    | KEEP @timestamp, service.name, message, log.level, trace.id, span.id
    | SORT @timestamp ASC
    | LIMIT 100
  ');
  
  RETURN correlated;
END SKILL;
