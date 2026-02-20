CREATE SKILL get_top_queries
  VERSION '1.0.0'
  DESCRIPTION 'Get most popular search queries'
  AUTHOR 'elastic'
  TAGS ['enterprise-search', 'queries', 'analytics']
  (app_name STRING DESCRIPTION 'Application name', limit INT DESCRIPTION 'Max results' DEFAULT 20)
  RETURNS ARRAY
BEGIN
  RETURN [
    {'query': 'pricing', 'count': 500, 'clicks': 450},
    {'query': 'documentation', 'count': 400, 'clicks': 380},
    {'query': 'api reference', 'count': 300, 'clicks': 290}
  ];
END SKILL;
