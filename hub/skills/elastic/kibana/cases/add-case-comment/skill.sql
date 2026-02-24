CREATE SKILL add_case_comment
  VERSION '1.0.0'
  DESCRIPTION 'Add a comment to an existing Kibana case'
  AUTHOR 'elastic'
  TAGS ['kibana', 'cases', 'incident-management', 'comment']
  (
    case_id STRING DESCRIPTION 'The ID of the case',
    comment STRING DESCRIPTION 'The comment text to add'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  SET result = CASE_COMMENT_ADD(case_id, comment, 'cases');
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'added',
      'case_id': case_id,
      'comment_id': result.data.id,
      'message': 'Comment added successfully'
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
