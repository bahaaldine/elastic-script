CREATE SKILL compare_deployments
  VERSION '1.0.0'
  DESCRIPTION 'Compare performance metrics between two deployments or versions.'
  AUTHOR 'apm-team'
  TAGS ['apm', 'deployment', 'comparison', 'performance']
  (
    service_name STRING DESCRIPTION 'Service name to compare',
    version_a STRING DESCRIPTION 'First version to compare',
    version_b STRING DESCRIPTION 'Second version to compare'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE comparison DOCUMENT;
  DECLARE metrics_a ARRAY;
  DECLARE metrics_b ARRAY;
  
  SET metrics_a = ESQL_QUERY('
    FROM apm-*
    | WHERE service.name == "' || service_name || '"
    | WHERE service.version == "' || version_a || '"
    | STATS 
        avg_latency = AVG(transaction.duration.us),
        p99_latency = PERCENTILE(transaction.duration.us, 99),
        error_rate = COUNT(*) WHERE transaction.result == "failure",
        total_requests = COUNT(*)
  ');
  
  SET metrics_b = ESQL_QUERY('
    FROM apm-*
    | WHERE service.name == "' || service_name || '"
    | WHERE service.version == "' || version_b || '"
    | STATS 
        avg_latency = AVG(transaction.duration.us),
        p99_latency = PERCENTILE(transaction.duration.us, 99),
        error_rate = COUNT(*) WHERE transaction.result == "failure",
        total_requests = COUNT(*)
  ');
  
  SET comparison = {
    'service_name': service_name,
    'version_a': version_a,
    'version_b': version_b,
    'metrics_a': metrics_a,
    'metrics_b': metrics_b,
    'compared_at': CURRENT_TIMESTAMP()
  };
  
  RETURN comparison;
END SKILL;
