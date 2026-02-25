CREATE SKILL sfdc_stale_opportunities
  VERSION '1.0.0'
  DESCRIPTION 'Find opportunities with no activity in N days. Identifies deals that need attention to prevent pipeline rot.'
  AUTHOR 'moltler'
  TAGS ['sfdc', 'salesforce', 'opportunity', 'sales-ops', 'pipeline-hygiene', 'stale']
  (
    stale_days INTEGER DESCRIPTION 'Number of days without activity to consider stale' DEFAULT 14,
    min_amount INTEGER DESCRIPTION 'Minimum opportunity amount to include' DEFAULT 0,
    owner STRING DESCRIPTION 'Filter by opportunity owner name' DEFAULT NULL
  )
  RETURNS ARRAY
BEGIN
  DECLARE results ARRAY;
  DECLARE search_query STRING;
  DECLARE stale_date STRING;
  
  SET stale_date = 'NOW() - INTERVAL ' || TO_STRING(stale_days) || ' DAYS';
  
  SET search_query = 'FROM sfdc-opportunities-* 
    | WHERE IsClosed == false 
      AND Amount >= ' || TO_STRING(min_amount) || '
      AND LastActivityDate < ' || stale_date;
  
  IF owner IS NOT NULL THEN
    SET search_query = search_query || ' AND Owner.Name LIKE "*' || owner || '*"';
  END IF;
  
  SET search_query = search_query || '
    | SORT Amount DESC
    | LIMIT 50';
  
  SET results = ESQL_QUERY(search_query);
  
  -- Format with stale analysis
  DECLARE formatted ARRAY;
  SET formatted = [];
  
  FOR opp IN results LOOP
    DECLARE days_stale INTEGER;
    SET days_stale = DATE_DIFF('day', opp.LastActivityDate, CURRENT_DATE());
    
    SET formatted = ARRAY_APPEND(formatted, {
      'id': opp.Id,
      'name': opp.Name,
      'account': opp.Account.Name,
      'owner': opp.Owner.Name,
      'stage': opp.StageName,
      'amount': opp.Amount,
      'close_date': opp.CloseDate,
      'is_past_close': opp.CloseDate < CURRENT_DATE(),
      'last_activity': opp.LastActivityDate,
      'days_stale': days_stale,
      'risk_level': CASE 
        WHEN days_stale > 30 THEN 'critical'
        WHEN days_stale > 21 THEN 'high'
        WHEN days_stale > 14 THEN 'medium'
        ELSE 'low'
      END,
      'recommendation': CASE 
        WHEN days_stale > 30 THEN 'Immediate outreach required - deal at high risk'
        WHEN days_stale > 21 THEN 'Schedule call this week'
        WHEN days_stale > 14 THEN 'Send follow-up email'
        ELSE 'Monitor'
      END
    });
  END LOOP;
  
  RETURN formatted;
END SKILL;
