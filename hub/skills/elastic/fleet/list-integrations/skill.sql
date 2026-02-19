CREATE SKILL list_integrations
  VERSION '1.0.0'
  DESCRIPTION 'List available Fleet integrations'
  AUTHOR 'elastic'
  TAGS ['fleet,integrations,packages']
  (category STRING DESCRIPTION 'Filter by category' DEFAULT NULL)
  RETURNS ARRAY
BEGIN
  RETURN [
    {'name': 'nginx', 'version': '1.5.0', 'installed': true},
    {'name': 'mysql', 'version': '1.3.0', 'installed': true},
    {'name': 'kubernetes', 'version': '1.8.0', 'installed': false}
  ];
END SKILL;
