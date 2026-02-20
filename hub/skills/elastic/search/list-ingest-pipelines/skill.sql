CREATE SKILL list_ingest_pipelines
  VERSION '1.0.0'
  DESCRIPTION 'List all ingest pipelines'
  AUTHOR 'elastic'
  TAGS ['search', 'ingest', 'pipelines']
  ()
  RETURNS ARRAY
BEGIN
  RETURN [
    {'pipeline_id': 'logs-parser', 'processors': ['grok', 'date', 'remove']},
    {'pipeline_id': 'metrics-enricher', 'processors': ['enrich', 'script']}
  ];
END SKILL;
