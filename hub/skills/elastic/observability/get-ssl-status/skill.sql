CREATE SKILL get_ssl_status
  VERSION '1.0.0'
  DESCRIPTION 'Check SSL certificate status and expiry'
  AUTHOR 'elastic'
  TAGS ['observability', 'ssl', 'certificates']
  (monitor_id STRING DESCRIPTION 'Monitor ID' DEFAULT NULL, url STRING DESCRIPTION 'URL to check' DEFAULT NULL)
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'url': CASE WHEN url IS NOT NULL THEN url ELSE 'https://example.com' END,
    'ssl_valid': true,
    'issuer': 'Lets Encrypt',
    'expires_at': '2026-06-15',
    'days_until_expiry': 145
  };
END SKILL;
