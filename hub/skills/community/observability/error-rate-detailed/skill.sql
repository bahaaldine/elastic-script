CREATE SKILL error_rate_detailed
  VERSION '1.0.0'
  DESCRIPTION 'Calculate error rate in a single ESQL query using WHERE on aggregation. Returns total count, error count, and error rate percentage, broken down by service. Uses TO_DOUBLE() to avoid integer division. Advanced counterpart to error_rate.'
  AUTHOR 'observability-team'
  TAGS ['observability', 'logs', 'errors', 'metrics', 'health', 'advanced']
  (
    index_pattern STRING DESCRIPTION 'Index pattern to search' DEFAULT 'logs-*',
    time_range STRING DESCRIPTION 'Time range to analyze' DEFAULT '7d'
  )
  RETURNS ARRAY
BEGIN
  DECLARE results ARRAY;

  -- Single query using WHERE on aggregation (ES 9.1+) to count total and errors
  -- in one pass. Groups by service for per-service error rates.
  -- Uses TO_DOUBLE() on denominator to avoid integer division truncation.
  SET results = ESQL_QUERY('
    FROM ' || index_pattern || '
    | WHERE @timestamp >= NOW() - ' || time_range || '
    | STATS
        total = COUNT(*),
        errors = COUNT(*) WHERE log.level == "ERROR",
        warnings = COUNT(*) WHERE log.level == "WARN"
      BY service.name
    | EVAL error_rate_pct = ROUND(errors * 100.0 / TO_DOUBLE(total), 2),
           warn_rate_pct = ROUND(warnings * 100.0 / TO_DOUBLE(total), 2)
    | KEEP service.name, total, errors, error_rate_pct, warnings, warn_rate_pct
    | SORT error_rate_pct DESC
  ');

  RETURN results;
END SKILL;
