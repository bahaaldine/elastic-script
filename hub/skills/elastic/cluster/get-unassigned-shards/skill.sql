CREATE SKILL get_unassigned_shards
  VERSION '1.0.0'
  DESCRIPTION 'Get list of unassigned shards and reasons'
  AUTHOR 'elastic'
  TAGS ['cluster', 'shards', 'troubleshooting']
  ()
  RETURNS ARRAY
BEGIN
  RETURN [];
END SKILL;
