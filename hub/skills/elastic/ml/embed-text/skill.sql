CREATE SKILL embed_text
  VERSION '1.0.0'
  DESCRIPTION 'Generate vector embeddings for text using a deployed embedding model'
  AUTHOR 'elastic'
  TAGS ['ml', 'embeddings', 'semantic']
  (text STRING DESCRIPTION 'Text to generate embeddings for', model_id STRING DESCRIPTION 'Embedding model ID' DEFAULT 'e5-small')
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  SET result = INFERENCE_EMBED(model_id, text);
  RETURN result;
END SKILL;
