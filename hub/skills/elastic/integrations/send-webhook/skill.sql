CREATE SKILL send_webhook
  VERSION '1.0.0'
  DESCRIPTION 'Send a webhook POST request'
  AUTHOR 'elastic'
  TAGS ['integrations', 'webhook', 'automation']
  (url STRING DESCRIPTION 'Webhook URL', payload STRING DESCRIPTION 'JSON payload to send')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'status': 'sent',
    'url': url,
    'response_code': 200
  };
END SKILL;
