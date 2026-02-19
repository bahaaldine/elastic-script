CREATE SKILL list_all_skills
  VERSION '1.0.0'
  DESCRIPTION 'List all available skills with their descriptions. Use this first when exploring what Moltler can do, or when unsure which skill to use. Returns a categorized list of all skills.'
  AUTHOR 'elastic'
  TAGS ['meta', 'discovery', 'navigation', 'help']
  (
    category STRING DESCRIPTION 'Filter by category: meta, observability, search, security, ml, management. Leave empty for all categories.' DEFAULT NULL,
    include_details BOOLEAN DESCRIPTION 'Include parameter details for each skill' DEFAULT FALSE
  )
  RETURNS ARRAY
BEGIN
  DECLARE skills_query STRING;
  DECLARE result ARRAY;
  
  IF category IS NOT NULL THEN
    SET skills_query = 'FROM .escript_skills | WHERE tags LIKE "*' || category || '*" | KEEP name, description, tags | SORT name | LIMIT 100';
  ELSE
    SET skills_query = 'FROM .escript_skills | KEEP name, description, tags | SORT name | LIMIT 100';
  END IF;
  
  SET result = ESQL_QUERY(skills_query);
  RETURN result;
END SKILL;
