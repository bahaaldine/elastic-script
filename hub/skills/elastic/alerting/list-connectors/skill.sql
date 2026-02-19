CREATE SKILL list_connectors
  VERSION '1.0.0'
  DESCRIPTION 'List all configured alert connectors (Slack, PagerDuty, etc.)'
  AUTHOR 'elastic'
  TAGS ['alerting,connectors,integrations']
  (type STRING DESCRIPTION 'Filter by connector type' DEFAULT NULL)
  RETURNS ARRAY
BEGIN
  RETURN [
    {'id': 'slack-ops', 'name': 'Slack Ops Channel', 'type': 'slack', 'status': 'active'},
    {'id': 'pagerduty', 'name': 'PagerDuty Integration', 'type': 'pagerduty', 'status': 'active'},
    {'id': 'email', 'name': 'Email Notifications', 'type': 'email', 'status': 'active'}
  ];
END SKILL;
