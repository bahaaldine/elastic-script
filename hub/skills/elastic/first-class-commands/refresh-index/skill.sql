CREATE SKILL refresh_index_cmd
  VERSION '1.0.0'
  DESCRIPTION 'Refresh an index using the first-class REFRESH command. Makes recently indexed documents searchable.'
  AUTHOR 'moltler'
  TAGS ['elasticsearch', 'refresh', 'first-class', 'command', 'index']
  (
    index_name STRING DESCRIPTION 'Index name or pattern to refresh'
  )
  RETURNS DOCUMENT
BEGIN
  -- Use first-class REFRESH command
  REFRESH index_name;
  
  RETURN {
    'status': 'refreshed',
    'index': index_name
  };
END SKILL;
