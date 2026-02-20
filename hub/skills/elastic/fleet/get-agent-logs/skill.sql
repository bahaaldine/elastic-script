CREATE SKILL get_agent_logs
  VERSION '1.0.0'
  DESCRIPTION 'Get logs from a specific agent'
  AUTHOR 'elastic'
  TAGS ['fleet', 'agents', 'logs']
  (agent_id STRING DESCRIPTION 'Agent ID', limit INT DESCRIPTION 'Max log lines' DEFAULT 100)
  RETURNS ARRAY
BEGIN
  RETURN [
    {'timestamp': '2026-01-22T10:00:00Z', 'level': 'INFO', 'message': 'Agent started'},
    {'timestamp': '2026-01-22T10:00:05Z', 'level': 'INFO', 'message': 'Connected to Fleet Server'}
  ];
END SKILL;
