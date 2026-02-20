CREATE SKILL list_agent_policies
  VERSION '1.0.0'
  DESCRIPTION 'List all agent policies'
  AUTHOR 'elastic'
  TAGS ['fleet', 'policies', 'management']
  ()
  RETURNS ARRAY
BEGIN
  RETURN [
    {'policy_id': 'production', 'name': 'Production Policy', 'agents': 10, 'integrations': 5},
    {'policy_id': 'development', 'name': 'Development Policy', 'agents': 3, 'integrations': 3}
  ];
END SKILL;
