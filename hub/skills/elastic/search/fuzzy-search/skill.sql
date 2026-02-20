CREATE SKILL fuzzy_search
  VERSION '1.0.0'
  DESCRIPTION 'Search with typo tolerance using fuzzy matching'
  AUTHOR 'elastic'
  TAGS ['search', 'fuzzy', 'query']
  (query STRING DESCRIPTION 'Search query', index_pattern STRING DESCRIPTION 'Index pattern' DEFAULT '*', fuzziness INT DESCRIPTION 'Fuzziness level 0-2' DEFAULT 1)
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  SET result = ESQL_QUERY('FROM ' || index_pattern || ' | LIMIT 20');
  RETURN result;
END SKILL;
