CREATE SKILL trigger_github_workflow
  VERSION '1.0.0'
  DESCRIPTION 'Trigger a GitHub Actions workflow'
  AUTHOR 'elastic'
  TAGS ['integrations,github,automation']
  (repo STRING DESCRIPTION 'Repository owner/name', workflow STRING DESCRIPTION 'Workflow filename or ID', ref STRING DESCRIPTION 'Branch or tag' DEFAULT 'main')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'status': 'triggered',
    'repository': repo,
    'workflow': workflow,
    'ref': ref
  };
END SKILL;
