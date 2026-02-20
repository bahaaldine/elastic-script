CREATE SKILL send_email
  VERSION '1.0.0'
  DESCRIPTION 'Send an email notification'
  AUTHOR 'elastic'
  TAGS ['integrations', 'email', 'notifications']
  (to STRING DESCRIPTION 'Recipient email address', subject STRING DESCRIPTION 'Email subject', body STRING DESCRIPTION 'Email body')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'status': 'sent',
    'to': to,
    'subject': subject,
    'sent_at': CURRENT_TIMESTAMP()
  };
END SKILL;
