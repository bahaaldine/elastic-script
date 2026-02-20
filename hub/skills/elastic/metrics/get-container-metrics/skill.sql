CREATE SKILL get_container_metrics
  VERSION '1.0.0'
  DESCRIPTION 'Get metrics for Docker/Kubernetes containers'
  AUTHOR 'elastic'
  TAGS ['metrics', 'containers', 'kubernetes']
  (container_id STRING DESCRIPTION 'Container ID or name' DEFAULT NULL, limit INT DESCRIPTION 'Max results' DEFAULT 20)
  RETURNS ARRAY
BEGIN
  DECLARE query STRING;
  DECLARE result ARRAY;
  IF container_id IS NOT NULL THEN
    SET query = 'FROM metrics-* | WHERE container.id == "' || container_id || '" | SORT @timestamp DESC | LIMIT ' || limit;
  ELSE
    SET query = 'FROM metrics-* | WHERE container.id IS NOT NULL | STATS count = COUNT(*) BY container.id | LIMIT ' || limit;
  END IF;
  SET result = ESQL_QUERY(query);
  RETURN result;
END SKILL;
