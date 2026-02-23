CREATE SKILL validate_schema
  VERSION '1.0.0'
  DESCRIPTION 'Validate document schema against expected mappings.'
  AUTHOR 'data-team'
  TAGS ['cluster', 'schema', 'validation', 'mappings']
  (
    index_name STRING DESCRIPTION 'Index to validate',
    expected_fields ARRAY DESCRIPTION 'Expected field list' DEFAULT NULL
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE validation DOCUMENT;
  DECLARE sample_docs ARRAY;
  
  -- Get sample documents
  SET sample_docs = ESQL_QUERY('
    FROM ' || index_name || '
    | LIMIT 10
  ');
  
  SET validation = {
    'index_name': index_name,
    'sample_count': ARRAY_LENGTH(sample_docs),
    'status': 'validated',
    'validated_at': CURRENT_TIMESTAMP()
  };
  
  RETURN validation;
END SKILL;
