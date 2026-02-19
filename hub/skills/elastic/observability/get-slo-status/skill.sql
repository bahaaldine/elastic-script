CREATE SKILL get_slo_status
  VERSION '1.0.0'
  DESCRIPTION 'Get SLO (Service Level Objective) status for a service'
  AUTHOR 'elastic'
  TAGS ['observability,slo,reliability']
  (service STRING DESCRIPTION 'Service name', slo_target FLOAT DESCRIPTION 'Target SLO percentage' DEFAULT 99.9)
  RETURNS DOCUMENT
BEGIN
  DECLARE total_result ARRAY;
  DECLARE error_result ARRAY;
  DECLARE total INT;
  DECLARE errors INT;
  DECLARE availability FLOAT;
  DECLARE status STRING;
  DECLARE error_budget FLOAT;
  
  SET total_result = ESQL_QUERY('FROM logs-* | WHERE service == "' || service || '" | STATS count = COUNT(*)');
  SET error_result = ESQL_QUERY('FROM logs-* | WHERE service == "' || service || '" AND level == "ERROR" | STATS count = COUNT(*)');
  SET total = total_result[0]['count'];
  SET errors = error_result[0]['count'];
  
  IF total > 0 THEN
    SET availability = ROUND((1 - (errors * 1.0 / total)) * 100, 3);
  ELSE
    SET availability = 100.0;
  END IF;
  
  IF availability >= slo_target THEN
    SET status = 'meeting';
  ELSE
    SET status = 'breaching';
  END IF;
  
  SET error_budget = ROUND(availability - slo_target, 3);
  
  RETURN {
    'service': service,
    'availability': availability,
    'slo_target': slo_target,
    'status': status,
    'error_budget_remaining': error_budget
  };
END SKILL;
