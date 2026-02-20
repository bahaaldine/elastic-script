CREATE SKILL list_trained_models
  VERSION '1.0.0'
  DESCRIPTION 'List all trained ML models including NLP models and custom models'
  AUTHOR 'elastic'
  TAGS ['ml', 'models', 'inference']
  (type STRING DESCRIPTION 'Filter by model type' DEFAULT NULL)
  RETURNS ARRAY
BEGIN
  RETURN [
    {'model_id': 'elser_v2', 'type': 'sparse_embedding', 'status': 'deployed'},
    {'model_id': 'e5-small', 'type': 'text_embedding', 'status': 'deployed'},
    {'model_id': 'lang_ident', 'type': 'classification', 'status': 'available'}
  ];
END SKILL;
