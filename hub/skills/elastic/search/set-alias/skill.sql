CREATE SKILL set_alias
  VERSION '1.0.0'
  DESCRIPTION 'Create or update an index alias'
  AUTHOR 'elastic'
  TAGS ['search,aliases,management']
  (alias_name STRING DESCRIPTION 'Alias name', index_name STRING DESCRIPTION 'Index to alias')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'acknowledged': true,
    'alias': alias_name,
    'index': index_name
  };
END SKILL;
