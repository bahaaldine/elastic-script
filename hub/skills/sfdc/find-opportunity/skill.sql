CREATE SKILL sfdc_find_opportunity
  VERSION '1.0.0'
  DESCRIPTION 'Find Salesforce opportunities by name, account, stage, or ID. Returns matching opportunities with deal details.'
  AUTHOR 'moltler'
  TAGS ['sfdc', 'salesforce', 'opportunity', 'lookup', 'sales', 'pipeline']
  (
    query STRING DESCRIPTION 'Search query - opportunity name, account name, or Salesforce ID',
    stage STRING DESCRIPTION 'Filter by stage (e.g., "Negotiation", "Closed Won")' DEFAULT NULL,
    limit INTEGER DESCRIPTION 'Maximum results to return' DEFAULT 10
  )
  RETURNS ARRAY
BEGIN
  DECLARE results ARRAY;
  DECLARE search_query STRING;
  
  -- Build base query
  SET search_query = 'FROM sfdc-opportunities-* 
    | WHERE Name LIKE "*' || query || '*" 
       OR Account.Name LIKE "*' || query || '*"
       OR Id == "' || query || '"';
  
  -- Add stage filter if provided
  IF stage IS NOT NULL THEN
    SET search_query = search_query || ' AND StageName == "' || stage || '"';
  END IF;
  
  SET search_query = search_query || '
    | SORT CloseDate DESC
    | LIMIT ' || CAST(limit AS STRING);
  
  SET results = ESQL_QUERY(search_query);
  
  -- Format results
  DECLARE formatted ARRAY;
  SET formatted = [];
  
  FOR opp IN results LOOP
    SET formatted = ARRAY_APPEND(formatted, {
      'id': opp.Id,
      'name': opp.Name,
      'account': opp.Account.Name,
      'stage': opp.StageName,
      'amount': opp.Amount,
      'probability': opp.Probability,
      'close_date': opp.CloseDate,
      'type': opp.Type,
      'lead_source': opp.LeadSource,
      'owner': opp.Owner.Name,
      'next_step': opp.NextStep,
      'days_in_stage': DATE_DIFF('day', opp.LastStageChangeDate, CURRENT_DATE())
    });
  END LOOP;
  
  RETURN formatted;
END SKILL;
