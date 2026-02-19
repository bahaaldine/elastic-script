CREATE SKILL send_slack_message
  VERSION '1.0.0'
  DESCRIPTION 'Send a message to a Slack channel'
  AUTHOR 'elastic'
  TAGS ['integrations,slack,notifications']
  (channel STRING DESCRIPTION 'Slack channel name or ID', message STRING DESCRIPTION 'Message to send')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'status': 'sent',
    'channel': channel,
    'message': message,
    'timestamp': CURRENT_TIMESTAMP()
  };
END SKILL;
