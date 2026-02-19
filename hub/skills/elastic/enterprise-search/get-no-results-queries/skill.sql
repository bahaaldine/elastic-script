CREATE SKILL get_no_results_queries
  VERSION '1.0.0'
  DESCRIPTION 'Get queries that returned no results'
  AUTHOR 'elastic'
  TAGS ['enterprise-search,queries,improvements']
  (app_name STRING DESCRIPTION 'Application name', limit INT DESCRIPTION 'Max results' DEFAULT 20)
  RETURNS ARRAY
BEGIN
  RETURN [
    {'query': 'misspeled word', 'count': 50},
    {'query': 'new feature x', 'count': 30}
  ];
END SKILL;
