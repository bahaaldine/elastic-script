CREATE SKILL get_search_analytics
  VERSION '1.0.0'
  DESCRIPTION 'Get search analytics for an application'
  AUTHOR 'elastic'
  TAGS ['enterprise-search', 'analytics', 'metrics']
  (app_name STRING DESCRIPTION 'Application name', days INT DESCRIPTION 'Number of days' DEFAULT 7)
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'app': app_name,
    'period_days': days,
    'total_queries': 15000,
    'avg_click_position': 2.3,
    'no_results_rate': 5.2
  };
END SKILL;
