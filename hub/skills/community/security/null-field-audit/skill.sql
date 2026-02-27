CREATE SKILL null_field_audit
  VERSION '1.0.0'
  DESCRIPTION 'Audit an index for fields with high null rates. Addresses the ESQL NULL handling anti-pattern: WHERE field != "value" silently excludes NULL rows, which can cause missed detections in security queries. This skill surfaces fields where NULLs are prevalent so analysts can adjust their filters accordingly.'
  AUTHOR 'security-team'
  TAGS ['security', 'data-quality', 'null-handling', 'audit', 'advanced']
  (
    index_pattern STRING DESCRIPTION 'Index pattern to audit' DEFAULT 'security-*',
    field_to_audit STRING DESCRIPTION 'Field to check for null rate' DEFAULT 'user.name',
    group_field STRING DESCRIPTION 'Field to group the audit by' DEFAULT 'event.category'
  )
  RETURNS ARRAY
BEGIN
  DECLARE results ARRAY;

  -- Count total rows, non-null rows, and null rows per group.
  -- A high null rate means negative filters (WHERE field != X) on this field
  -- will silently drop those rows — a common source of missed security detections.
  -- The fix: use (WHERE field != "value" OR field IS NULL) in security queries.
  SET results = ESQL_QUERY('
    FROM ' || index_pattern || '
    | STATS
        total_docs = COUNT(*),
        non_null_count = COUNT(' || field_to_audit || ')
      BY ' || group_field || '
    | EVAL
        null_count = total_docs - non_null_count,
        null_rate_pct = ROUND((total_docs - non_null_count) * 100.0 / TO_DOUBLE(total_docs), 2)
    | KEEP ' || group_field || ', total_docs, non_null_count, null_count, null_rate_pct
    | SORT null_rate_pct DESC
  ');

  RETURN results;
END SKILL;
