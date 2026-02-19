CREATE SKILL recommend_skills
  VERSION '1.0.0'
  DESCRIPTION 'Get skill recommendations based on your goal or context. Describe what you are trying to achieve and get a prioritized list of relevant skills. Great for complex tasks that may require multiple skills.'
  AUTHOR 'elastic'
  TAGS ['meta', 'discovery', 'recommendations', 'ai']
  (
    goal STRING DESCRIPTION 'Describe your goal or what you are investigating (e.g., investigate production incident, analyze user behavior)'
  )
  RETURNS ARRAY
BEGIN
  DECLARE recommendations ARRAY;
  DECLARE goal_lower STRING;
  
  SET goal_lower = LOWER(goal);
  
  IF INSTR(goal_lower, 'error') > 0 OR INSTR(goal_lower, 'incident') > 0 OR INSTR(goal_lower, 'debug') > 0 THEN
    SET recommendations = ESQL_QUERY('FROM .escript_skills | WHERE tags LIKE "*observability*" OR tags LIKE "*logs*" OR tags LIKE "*error*" | KEEP name, description | LIMIT 10');
  ELSEIF INSTR(goal_lower, 'security') > 0 OR INSTR(goal_lower, 'threat') > 0 OR INSTR(goal_lower, 'attack') > 0 THEN
    SET recommendations = ESQL_QUERY('FROM .escript_skills | WHERE tags LIKE "*security*" | KEEP name, description | LIMIT 10');
  ELSEIF INSTR(goal_lower, 'search') > 0 OR INSTR(goal_lower, 'find') > 0 OR INSTR(goal_lower, 'query') > 0 THEN
    SET recommendations = ESQL_QUERY('FROM .escript_skills | WHERE tags LIKE "*search*" | KEEP name, description | LIMIT 10');
  ELSE
    SET recommendations = ESQL_QUERY('FROM .escript_skills | KEEP name, description | LIMIT 10');
  END IF;
  
  RETURN recommendations;
END SKILL;
