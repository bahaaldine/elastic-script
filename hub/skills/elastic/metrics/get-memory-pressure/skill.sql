CREATE SKILL get_memory_pressure
  VERSION '1.0.0'
  DESCRIPTION 'Find hosts with high memory pressure'
  AUTHOR 'elastic'
  TAGS ['metrics,memory,alerts']
  (threshold INT DESCRIPTION 'Memory usage threshold percent' DEFAULT 80)
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM metrics-* | WHERE metric_name == "memory" AND value > ' || threshold || ' | STATS max_memory = MAX(value), avg_memory = AVG(value) BY host | SORT max_memory DESC | LIMIT 20');
  RETURN result;
END SKILL;
