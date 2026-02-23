CREATE SKILL generate_compliance_report
  VERSION '1.0.0'
  DESCRIPTION 'Generate compliance reports for security standards (SOC2, PCI, HIPAA).'
  AUTHOR 'security-team'
  TAGS ['security', 'compliance', 'reporting', 'soc2', 'pci']
  (
    standard STRING DESCRIPTION 'Compliance standard: soc2, pci, hipaa' DEFAULT 'soc2',
    time_range STRING DESCRIPTION 'Reporting period' DEFAULT '30d'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE report DOCUMENT;
  DECLARE access_controls ARRAY;
  DECLARE encryption_status ARRAY;
  DECLARE incident_summary ARRAY;
  
  -- Access control checks
  SET access_controls = ESQL_QUERY('
    FROM auditbeat-*
    | WHERE @timestamp > NOW() - INTERVAL ' || time_range || '
    | WHERE event.category == "authentication"
    | STATS 
        total_logins = COUNT(*),
        failed_logins = COUNT(*) WHERE event.outcome == "failure",
        mfa_used = COUNT(*) WHERE user.authentication.mfa == true
      BY service.name
  ');
  
  -- Incident summary
  SET incident_summary = ESQL_QUERY('
    FROM .alerts-security.*
    | WHERE @timestamp > NOW() - INTERVAL ' || time_range || '
    | STATS 
        total_alerts = COUNT(*),
        critical_alerts = COUNT(*) WHERE kibana.alert.severity == "critical",
        resolved = COUNT(*) WHERE kibana.alert.status == "closed"
  ');
  
  SET report = {
    'standard': standard,
    'period': time_range,
    'generated_at': CURRENT_TIMESTAMP(),
    'access_controls': access_controls,
    'incident_summary': incident_summary,
    'status': 'generated'
  };
  
  RETURN report;
END SKILL;
