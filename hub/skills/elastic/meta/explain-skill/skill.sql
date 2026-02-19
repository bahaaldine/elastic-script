CREATE SKILL explain_skill
  VERSION '1.0.0'
  DESCRIPTION 'Get detailed explanation of a specific skill including all parameters, return type, and usage examples. Use this when you found a skill and want to understand exactly how to use it.'
  AUTHOR 'elastic'
  TAGS ['meta', 'discovery', 'documentation', 'help']
  (
    skill_name STRING DESCRIPTION 'Name of the skill to explain (e.g., count_logs_by_level)'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE skill_query STRING;
  DECLARE skill_info ARRAY;
  
  SET skill_query = 'FROM .escript_skills | WHERE name == "' || skill_name || '" | LIMIT 1';
  SET skill_info = ESQL_QUERY(skill_query);
  
  IF ARRAY_LENGTH(skill_info) == 0 THEN
    RETURN {
      'error': 'Skill not found',
      'skill_name': skill_name,
      'suggestion': 'Use search_skills to find available skills'
    };
  END IF;
  
  RETURN skill_info[0];
END SKILL;
