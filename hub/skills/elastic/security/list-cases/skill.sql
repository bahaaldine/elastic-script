CREATE SKILL list_cases
  VERSION '1.0.0'
  DESCRIPTION 'List security investigation cases'
  AUTHOR 'elastic'
  TAGS ['security,cases,investigation']
  (status STRING DESCRIPTION 'Filter by status: open, closed, in-progress' DEFAULT NULL)
  RETURNS ARRAY
BEGIN
  RETURN [
    {'case_id': 'case-001', 'title': 'Suspicious Login Activity', 'status': 'open', 'severity': 'high'},
    {'case_id': 'case-002', 'title': 'Malware Investigation', 'status': 'in-progress', 'severity': 'critical'}
  ];
END SKILL;
