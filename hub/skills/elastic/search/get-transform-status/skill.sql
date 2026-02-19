CREATE SKILL get_transform_status
  VERSION '1.0.0'
  DESCRIPTION 'Get status of a transform'
  AUTHOR 'elastic'
  TAGS ['search,transforms,status']
  (transform_id STRING DESCRIPTION 'Transform ID')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'transform_id': transform_id,
    'state': 'started',
    'documents_processed': 10000,
    'trigger_count': 100
  };
END SKILL;
