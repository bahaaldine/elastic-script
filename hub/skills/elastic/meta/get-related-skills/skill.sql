CREATE SKILL get_related_skills
  VERSION '1.0.0'
  DESCRIPTION 'Find skills related to a given skill. Use this to discover complementary skills that work well together, or to find alternative approaches to the same task.'
  AUTHOR 'elastic'
  TAGS ['meta', 'discovery', 'navigation', 'help']
  (
    skill_name STRING DESCRIPTION 'Name of the skill to find related skills for'
  )
  RETURNS ARRAY
BEGIN
  DECLARE skill_info ARRAY;
  DECLARE related ARRAY;
  
  SET skill_info = ESQL_QUERY('FROM .escript_skills | WHERE name == "' || skill_name || '" | KEEP tags | LIMIT 1');
  
  IF ARRAY_LENGTH(skill_info) == 0 THEN
    RETURN [];
  END IF;
  
  SET related = ESQL_QUERY('FROM .escript_skills | WHERE name != "' || skill_name || '" | KEEP name, description, tags | LIMIT 10');
  
  RETURN related;
END SKILL;
