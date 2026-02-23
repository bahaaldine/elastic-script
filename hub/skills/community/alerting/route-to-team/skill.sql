CREATE SKILL route_to_team
  VERSION '1.0.0'
  DESCRIPTION 'Route alerts to the appropriate team based on service ownership.'
  AUTHOR 'sre-team'
  TAGS ['alerting', 'routing', 'teams']
  (
    alert_id STRING DESCRIPTION 'Alert ID to route',
    override_team STRING DESCRIPTION 'Override automatic team assignment' DEFAULT NULL
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE routing DOCUMENT;
  DECLARE alert_info ARRAY;
  DECLARE assigned_team STRING;
  
  -- Get alert details to determine service
  SET alert_info = ESQL_QUERY('
    FROM .alerts-*
    | WHERE kibana.alert.uuid == "' || alert_id || '"
    | KEEP service.name, rule.name, kibana.alert.severity
    | LIMIT 1
  ');
  
  -- In production, this would look up team ownership
  IF override_team IS NOT NULL THEN
    SET assigned_team = override_team;
  ELSE
    SET assigned_team = 'platform-team';
  END IF;
  
  SET routing = {
    'alert_id': alert_id,
    'assigned_team': assigned_team,
    'alert_info': alert_info,
    'routed_at': CURRENT_TIMESTAMP()
  };
  
  RETURN routing;
END SKILL;
