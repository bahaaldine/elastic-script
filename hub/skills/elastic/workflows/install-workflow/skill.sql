CREATE SKILL install_workflow
  VERSION '1.0.0'
  DESCRIPTION 'Install a new Elastic Workflow from YAML definition. Creates the workflow in Kibana.'
  AUTHOR 'elastic'
  TAGS ['workflows', 'automation', 'kibana', 'installation']
  (
    yaml STRING DESCRIPTION 'Workflow YAML definition',
    kibana_url STRING DESCRIPTION 'Kibana URL (defaults to KIBANA_URL env var or localhost:5601)' DEFAULT NULL
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  IF yaml IS NULL OR LENGTH(yaml) = 0 THEN
    RETURN {
      'success': FALSE,
      'error': 'YAML definition is required'
    };
  END IF;
  
  IF yaml NOT LIKE '%name:%' THEN
    RETURN {
      'success': FALSE,
      'error': 'Invalid workflow YAML: missing required "name" field'
    };
  END IF;
  
  IF yaml NOT LIKE '%steps:%' THEN
    RETURN {
      'success': FALSE,
      'error': 'Invalid workflow YAML: missing required "steps" field'
    };
  END IF;
  
  IF kibana_url IS NOT NULL THEN
    SET result = WORKFLOW_CREATE(yaml, kibana_url);
  ELSE
    SET result = WORKFLOW_CREATE(yaml);
  END IF;
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'installed',
      'workflow_id': result.id,
      'name': result.name,
      'message': 'Workflow installed successfully',
      'next_steps': [
        'View in Kibana: Management → Workflows',
        'Trigger with: RUN SKILL trigger_workflow(workflow_id => ''' || result.id || ''')'
      ]
    };
  ELSE
    RETURN {
      'status': 'failed',
      'error': result.error,
      'message': 'Failed to install workflow'
    };
  END IF;
END SKILL;
