CREATE SKILL trigger_pagerduty
  VERSION '1.0.0'
  DESCRIPTION 'Trigger a PagerDuty incident'
  AUTHOR 'elastic'
  TAGS ['integrations', 'pagerduty', 'incidents']
  (service_key STRING DESCRIPTION 'PagerDuty service key', description STRING DESCRIPTION 'Incident description', severity STRING DESCRIPTION 'Severity: critical, error, warning, info' DEFAULT 'error')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'status': 'triggered',
    'incident_key': 'pd-' || SUBSTRING(service_key, 1, 8),
    'description': description,
    'severity': severity
  };
END SKILL;
