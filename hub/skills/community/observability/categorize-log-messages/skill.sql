CREATE SKILL categorize_log_messages
  VERSION '1.0.0'
  DESCRIPTION 'Automatically group similar log messages into patterns using CATEGORIZE(). Surfaces dominant log patterns without manual regex. Much more useful than grouping by exact message text. Advanced counterpart to get_log_patterns.'
  AUTHOR 'observability-team'
  TAGS ['observability', 'logs', 'patterns', 'categorize', 'advanced']
  (
    index_pattern STRING DESCRIPTION 'Index pattern to analyze' DEFAULT 'logs-*',
    limit INT DESCRIPTION 'Max number of patterns to return' DEFAULT 20
  )
  RETURNS ARRAY
BEGIN
  DECLARE results ARRAY;

  -- CATEGORIZE() auto-groups similar messages into regex-like patterns.
  -- This is far more useful than STATS BY message which groups by exact text.
  SET results = ESQL_QUERY('
    FROM ' || index_pattern || '
    | STATS count = COUNT(*), sample = SAMPLE(message, 1) BY category = CATEGORIZE(message)
    | SORT count DESC
    | LIMIT ' || limit || '
  ');

  RETURN results;
END SKILL;
