CREATE SKILL get_service_dependencies
  VERSION '1.0.0'
  DESCRIPTION 'Get upstream and downstream service dependencies'
  AUTHOR 'elastic'
  TAGS ['apm,dependencies,topology']
  (service STRING DESCRIPTION 'Service name to analyze')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'service': service,
    'upstream': ['api-gateway', 'load-balancer'],
    'downstream': ['database', 'cache', 'queue'],
    'note': 'Full dependency analysis requires trace data'
  };
END SKILL;
