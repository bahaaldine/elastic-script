CREATE SKILL search_skills
  VERSION '1.0.0'
  DESCRIPTION 'Search for skills by keyword or capability. Use when you know what you want to do but not which skill to use. Examples: search for logs, find errors, security alerts.'
  AUTHOR 'elastic'
  TAGS ['meta', 'discovery', 'search', 'help']
  (
    query STRING DESCRIPTION 'Search query - describe what you want to do (e.g., analyze logs, check security)'
  )
  RETURNS ARRAY
BEGIN
  DECLARE search_query STRING;
  DECLARE result ARRAY;
  
  SET search_query = 'FROM .escript_skills | WHERE name LIKE "*' || LOWER(query) || '*" OR description LIKE "*' || LOWER(query) || '*" OR tags LIKE "*' || LOWER(query) || '*" | KEEP name, description, tags | LIMIT 20';
  
  SET result = ESQL_QUERY(search_query);
  RETURN result;
END SKILL;
