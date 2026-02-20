CREATE SKILL run_inference
  VERSION '1.0.0'
  DESCRIPTION 'Run inference using a deployed ML model for text classification, embedding, or NER'
  AUTHOR 'elastic'
  TAGS ['ml', 'inference', 'nlp']
  (model_id STRING DESCRIPTION 'Model ID to use for inference', input_text STRING DESCRIPTION 'Text to process')
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  SET result = INFERENCE(model_id, input_text);
  RETURN result;
END SKILL;
