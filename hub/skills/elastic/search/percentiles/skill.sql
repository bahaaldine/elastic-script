CREATE SKILL percentiles
  VERSION '1.0.0'
  DESCRIPTION 'Calculate percentile distributions for a numeric field'
  AUTHOR 'elastic'
  TAGS ['search,percentiles,statistics']
  (index_pattern STRING DESCRIPTION 'Index to analyze', field STRING DESCRIPTION 'Numeric field')
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM ' || index_pattern || ' | STATS p25 = PERCENTILE(' || field || ', 25), p50 = PERCENTILE(' || field || ', 50), p75 = PERCENTILE(' || field || ', 75), p90 = PERCENTILE(' || field || ', 90), p99 = PERCENTILE(' || field || ', 99)');
  RETURN result;
END SKILL;
