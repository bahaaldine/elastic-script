CREATE SKILL date_histogram
  VERSION '1.0.0'
  DESCRIPTION 'Aggregate documents by time intervals'
  AUTHOR 'elastic'
  TAGS ['search', 'histogram', 'timeseries']
  (index_pattern STRING DESCRIPTION 'Index to aggregate', interval STRING DESCRIPTION 'Interval: 1h, 1d, 1w' DEFAULT '1d', field STRING DESCRIPTION 'Date field' DEFAULT '@timestamp')
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM ' || index_pattern || ' | STATS count = COUNT(*) BY DATE_TRUNC("' || interval || '", ' || field || ') | SORT ' || field);
  RETURN result;
END SKILL;
