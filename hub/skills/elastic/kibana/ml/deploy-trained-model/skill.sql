CREATE SKILL deploy_trained_model
  VERSION '1.0.0'
  DESCRIPTION 'Deploy a trained ML model for inference'
  AUTHOR 'elastic'
  TAGS ['kibana', 'ml', 'trained-models', 'deploy', 'inference']
  (
    model_id STRING DESCRIPTION 'ID of the trained model to deploy'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = ML_TRAINED_MODEL_DEPLOY(model_id);
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'deployed',
      'model_id': model_id,
      'message': 'Model deployment started successfully'
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
