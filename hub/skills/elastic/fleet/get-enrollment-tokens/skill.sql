CREATE SKILL get_enrollment_tokens
  VERSION '1.0.0'
  DESCRIPTION 'List enrollment tokens for agent onboarding'
  AUTHOR 'elastic'
  TAGS ['fleet', 'enrollment', 'onboarding']
  ()
  RETURNS ARRAY
BEGIN
  RETURN [
    {'token_id': 'token-1', 'policy': 'production', 'active': true},
    {'token_id': 'token-2', 'policy': 'development', 'active': true}
  ];
END SKILL;
