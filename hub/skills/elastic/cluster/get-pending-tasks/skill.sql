CREATE SKILL get_pending_tasks
  VERSION '1.0.0'
  DESCRIPTION 'Get pending cluster tasks'
  AUTHOR 'elastic'
  TAGS ['cluster,tasks,operations']
  ()
  RETURNS ARRAY
BEGIN
  RETURN [];
END SKILL;
