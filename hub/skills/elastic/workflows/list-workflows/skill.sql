CREATE SKILL list_workflows
  VERSION '1.0.0'
  DESCRIPTION 'List all available Elastic Workflows in Kibana. Returns workflow names, IDs, descriptions, and trigger types.'
  AUTHOR 'elastic'
  TAGS ['workflows', 'automation', 'kibana', 'discovery']
  (
    kibana_url STRING DESCRIPTION 'Kibana URL (defaults to KIBANA_URL env var or localhost:5601)' DEFAULT NULL
  )
  RETURNS ARRAY
BEGIN
  DECLARE workflows ARRAY;
  DECLARE summary ARRAY;
  
  IF kibana_url IS NOT NULL THEN
    SET workflows = WORKFLOW_LIST(kibana_url);
  ELSE
    SET workflows = WORKFLOW_LIST();
  END IF;
  
  SET summary = [];
  
  FOR workflow IN workflows LOOP
    SET summary = ARRAY_APPEND(summary, {
      'id': workflow.id,
      'name': workflow.name,
      'description': workflow.description,
      'enabled': workflow.enabled,
      'tags': workflow.tags,
      'triggers': workflow.triggers
    });
  END LOOP;
  
  RETURN summary;
END SKILL;
