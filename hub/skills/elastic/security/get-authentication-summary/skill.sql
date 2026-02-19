CREATE SKILL get_authentication_summary
  VERSION '1.0.0'
  DESCRIPTION 'Get authentication success/failure summary'
  AUTHOR 'elastic'
  TAGS ['security,authentication,summary']
  (time_range STRING DESCRIPTION 'Time range to analyze' DEFAULT '24h')
  RETURNS DOCUMENT
BEGIN
  DECLARE success_result ARRAY;
  DECLARE failure_result ARRAY;
  SET success_result = ESQL_QUERY('FROM security-* | WHERE event_type == "authentication" AND outcome == "success" | STATS count = COUNT(*)');
  SET failure_result = ESQL_QUERY('FROM security-* | WHERE event_type == "authentication" AND outcome == "failure" | STATS count = COUNT(*)');
  RETURN {
    'successful_logins': success_result[0]['count'],
    'failed_logins': failure_result[0]['count'],
    'failure_rate': ROUND((failure_result[0]['count'] * 100.0) / (success_result[0]['count'] + failure_result[0]['count'] + 1), 2)
  };
END SKILL;
