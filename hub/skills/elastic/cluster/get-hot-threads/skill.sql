CREATE SKILL get_hot_threads
  VERSION '1.0.0'
  DESCRIPTION 'Get hot threads from nodes for debugging'
  AUTHOR 'elastic'
  TAGS ['cluster', 'debugging', 'performance']
  (node STRING DESCRIPTION 'Node name or _all' DEFAULT '_all')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'node': node,
    'hot_threads': ['GC thread', 'Index writer thread'],
    'cpu_percent': 45
  };
END SKILL;
