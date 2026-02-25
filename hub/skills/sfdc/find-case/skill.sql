CREATE SKILL sfdc_find_case
  VERSION '1.0.0'
  DESCRIPTION 'Find Salesforce cases by case number, subject, account, or ID. Returns matching support cases with details.'
  AUTHOR 'moltler'
  TAGS ['sfdc', 'salesforce', 'case', 'support', 'lookup', 'customer-service']
  (
    query STRING DESCRIPTION 'Search query - case number, subject keywords, account name, or Salesforce ID',
    status STRING DESCRIPTION 'Filter by status (e.g., "Open", "Closed")' DEFAULT NULL,
    priority STRING DESCRIPTION 'Filter by priority (e.g., "High", "Critical")' DEFAULT NULL,
    limit INTEGER DESCRIPTION 'Maximum results to return' DEFAULT 10
  )
  RETURNS ARRAY
BEGIN
  DECLARE results ARRAY;
  DECLARE search_query STRING;
  
  -- Build base query
  SET search_query = 'FROM sfdc-cases-* 
    | WHERE CaseNumber LIKE "*' || query || '*" 
       OR Subject LIKE "*' || query || '*"
       OR Account.Name LIKE "*' || query || '*"
       OR Id == "' || query || '"';
  
  -- Add filters
  IF status IS NOT NULL THEN
    SET search_query = search_query || ' AND Status == "' || status || '"';
  END IF;
  
  IF priority IS NOT NULL THEN
    SET search_query = search_query || ' AND Priority == "' || priority || '"';
  END IF;
  
  SET search_query = search_query || '
    | SORT CreatedDate DESC
    | LIMIT ' || TO_STRING(limit);
  
  SET results = ESQL_QUERY(search_query);
  
  -- Format results
  DECLARE formatted ARRAY;
  SET formatted = [];
  
  FOR c IN results LOOP
    SET formatted = ARRAY_APPEND(formatted, {
      'id': c.Id,
      'case_number': c.CaseNumber,
      'subject': c.Subject,
      'account': c.Account.Name,
      'contact': c.Contact.Name,
      'status': c.Status,
      'priority': c.Priority,
      'type': c.Type,
      'origin': c.Origin,
      'owner': c.Owner.Name,
      'created_date': c.CreatedDate,
      'age_days': DATE_DIFF('day', c.CreatedDate, CURRENT_DATE()),
      'is_escalated': c.IsEscalated
    });
  END LOOP;
  
  RETURN formatted;
END SKILL;
