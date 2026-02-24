CREATE SKILL workflow_status
  VERSION '1.0.0'
  DESCRIPTION 'Get the execution status of a workflow run. Returns step results, timing, and any errors.'
  AUTHOR 'elastic'
  TAGS ['workflows', 'automation', 'kibana', 'monitoring']
  (
    execution_id STRING DESCRIPTION 'Workflow execution ID returned from trigger_workflow',
    kibana_url STRING DESCRIPTION 'Kibana URL (defaults to KIBANA_URL env var or localhost:5601)' DEFAULT NULL
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  IF kibana_url IS NOT NULL THEN
    SET result = WORKFLOW_STATUS(execution_id, kibana_url);
  ELSE
    SET result = WORKFLOW_STATUS(execution_id);
  END IF;
  
  IF result.found = FALSE THEN
    RETURN {
      'found': FALSE,
      'execution_id': execution_id,
      'message': 'Execution not found'
    };
  END IF;
  
  RETURN {
    'execution_id': execution_id,
    'workflow_id': result.workflow_id,
    'status': result.status,
    'started_at': result.started_at,
    'completed_at': result.completed_at,
    'duration_ms': result.duration_ms,
    'steps': result.steps,
    'error': result.error
  };
END SKILL;
