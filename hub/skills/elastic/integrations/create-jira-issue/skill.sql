CREATE SKILL create_jira_issue
  VERSION '1.0.0'
  DESCRIPTION 'Create a Jira issue/ticket'
  AUTHOR 'elastic'
  TAGS ['integrations,jira,ticketing']
  (project STRING DESCRIPTION 'Jira project key', summary STRING DESCRIPTION 'Issue summary', description STRING DESCRIPTION 'Issue description' DEFAULT '', priority STRING DESCRIPTION 'Priority: highest, high, medium, low, lowest' DEFAULT 'medium')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'status': 'created',
    'project': project,
    'summary': summary,
    'priority': priority,
    'key': project || '-123'
  };
END SKILL;
