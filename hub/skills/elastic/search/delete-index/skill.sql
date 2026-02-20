CREATE SKILL delete_index
  VERSION '1.0.0'
  DESCRIPTION 'Delete an index'
  AUTHOR 'elastic'
  TAGS ['search', 'indices', 'delete']
  (index_name STRING DESCRIPTION 'Index name to delete')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'acknowledged': true,
    'index': index_name
  };
END SKILL;
