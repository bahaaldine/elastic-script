CREATE SKILL trigger_workflow
  VERSION '1.0.0'
  DESCRIPTION 'Trigger an Elastic Workflow by ID or name. Passes inputs to the workflow and returns execution details.'
  AUTHOR 'elastic'
  TAGS ['workflows', 'automation', 'kibana']
  (
    workflow_id STRING DESCRIPTION 'Workflow ID or name to trigger',
    inputs DOCUMENT DESCRIPTION 'Input parameters to pass to the workflow' DEFAULT {},
    kibana_url STRING DESCRIPTION 'Kibana URL (defaults to KIBANA_URL env var or localhost:5601)' DEFAULT NULL
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  IF kibana_url IS NOT NULL THEN
    SET result = WORKFLOW_TRIGGER(workflow_id, inputs, kibana_url);
  ELSE
    SET result = WORKFLOW_TRIGGER(workflow_id, inputs);
  END IF;
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'triggered',
      'workflow_id': workflow_id,
      'execution_id': result.execution_id,
      'message': 'Workflow triggered successfully',
      'details': result
    };
  ELSE
    RETURN {
      'status': 'failed',
      'workflow_id': workflow_id,
      'error': result.error,
      'message': 'Failed to trigger workflow'
    };
  END IF;
END SKILL;
