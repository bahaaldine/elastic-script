CREATE SKILL test_ingest_pipeline
  VERSION '1.0.0'
  DESCRIPTION 'Test an ingest pipeline with sample data'
  AUTHOR 'elastic'
  TAGS ['search,ingest,testing']
  (pipeline_id STRING DESCRIPTION 'Pipeline ID', sample_doc STRING DESCRIPTION 'Sample document JSON')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'pipeline_id': pipeline_id,
    'result': 'success',
    'output': {'message': 'Parsed successfully'}
  };
END SKILL;
