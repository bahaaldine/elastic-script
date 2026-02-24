CREATE SKILL list_fleet_agents
  VERSION '1.0.0'
  DESCRIPTION 'List all Fleet agents'
  AUTHOR 'elastic'
  TAGS ['kibana', 'fleet', 'agents', 'monitoring']
  (
    per_page INTEGER DESCRIPTION 'Results per page' DEFAULT 20,
    kuery STRING DESCRIPTION 'KQL filter query' DEFAULT NULL
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = AGENT_LIST(per_page, 1, kuery);
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'success',
      'agents': result.data.items,
      'total': result.data.total,
      'online': result.data.statusSummary.online,
      'offline': result.data.statusSummary.offline,
      'error': result.data.statusSummary.error
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
