CREATE SKILL create_case
  VERSION '1.0.0'
  DESCRIPTION 'Create a new security investigation case'
  AUTHOR 'elastic'
  TAGS ['security', 'cases', 'investigation']
  (title STRING DESCRIPTION 'Case title', description STRING DESCRIPTION 'Case description', severity STRING DESCRIPTION 'Severity: critical, high, medium, low' DEFAULT 'medium')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'case_id': 'case-' || SUBSTRING(title, 1, 8),
    'title': title,
    'description': description,
    'severity': severity,
    'status': 'open',
    'created_at': CURRENT_TIMESTAMP()
  };
END SKILL;
