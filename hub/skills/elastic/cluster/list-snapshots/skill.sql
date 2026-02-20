CREATE SKILL list_snapshots
  VERSION '1.0.0'
  DESCRIPTION 'List snapshots in a repository'
  AUTHOR 'elastic'
  TAGS ['cluster', 'snapshots', 'backup']
  (repository STRING DESCRIPTION 'Repository name' DEFAULT 'default')
  RETURNS ARRAY
BEGIN
  RETURN [
    {'snapshot': 'daily-2026-01-22', 'state': 'SUCCESS', 'indices': 50, 'start_time': '2026-01-22T00:00:00Z'},
    {'snapshot': 'daily-2026-01-21', 'state': 'SUCCESS', 'indices': 50, 'start_time': '2026-01-21T00:00:00Z'}
  ];
END SKILL;
