CREATE SKILL send_teams_message
  VERSION '1.0.0'
  DESCRIPTION 'Send a Microsoft Teams message'
  AUTHOR 'elastic'
  TAGS ['integrations', 'teams', 'notifications']
  (webhook_url STRING DESCRIPTION 'Teams webhook URL', message STRING DESCRIPTION 'Message to send')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'status': 'sent',
    'platform': 'Microsoft Teams',
    'timestamp': CURRENT_TIMESTAMP()
  };
END SKILL;
