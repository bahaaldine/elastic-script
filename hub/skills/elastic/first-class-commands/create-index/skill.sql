CREATE SKILL create_index_cmd
  VERSION '1.0.0'
  DESCRIPTION 'Create an index with mappings using the first-class CREATE INDEX command.'
  AUTHOR 'moltler'
  TAGS ['elasticsearch', 'create', 'index', 'first-class', 'command', 'mappings']
  (
    index_name STRING DESCRIPTION 'Name for the new index',
    mappings DOCUMENT DESCRIPTION 'Index mappings definition' DEFAULT NULL
  )
  RETURNS DOCUMENT
BEGIN
  -- Use first-class CREATE INDEX command
  IF mappings IS NOT NULL THEN
    CREATE INDEX index_name WITH MAPPINGS mappings;
  ELSE
    CREATE INDEX index_name;
  END IF;
  
  RETURN {
    'status': 'created',
    'index': index_name
  };
END SKILL;
