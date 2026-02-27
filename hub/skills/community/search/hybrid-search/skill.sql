CREATE SKILL hybrid_search
  VERSION '1.1.0'
  DESCRIPTION 'Full-text search using MATCH() for BM25 relevance ranking. Returns matching documents sorted by relevance. For true hybrid search combining BM25 + vector similarity, use FORK/FUSE on ES 9.1+ (see multi-aggregation-fork skill).'
  AUTHOR 'search-team'
  TAGS ['search', 'hybrid', 'fulltext', 'match']
  (
    query STRING DESCRIPTION 'Search query text',
    index_pattern STRING DESCRIPTION 'Index pattern to search' DEFAULT 'content-*',
    text_field STRING DESCRIPTION 'Text field to search' DEFAULT 'content',
    k INTEGER DESCRIPTION 'Number of results' DEFAULT 10
  )
  RETURNS ARRAY
BEGIN
  DECLARE results ARRAY;

  SET results = ESQL_QUERY('
    FROM ' || index_pattern || '
    | WHERE MATCH(' || text_field || ', "' || query || '")
    | KEEP title, ' || text_field || ', @timestamp
    | LIMIT ' || k || '
  ');

  RETURN results;
END SKILL;
