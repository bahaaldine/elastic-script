CREATE SKILL sfdc_closing_this_month
  VERSION '1.0.0'
  DESCRIPTION 'Get all opportunities expected to close this month. Includes commit vs best case analysis.'
  AUTHOR 'moltler'
  TAGS ['sfdc', 'salesforce', 'opportunity', 'sales-ops', 'forecast', 'commit']
  (
    owner STRING DESCRIPTION 'Filter by opportunity owner name' DEFAULT NULL,
    min_probability INTEGER DESCRIPTION 'Minimum probability percentage' DEFAULT 0
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE results ARRAY;
  DECLARE search_query STRING;
  
  SET search_query = 'FROM sfdc-opportunities-* 
    | WHERE IsClosed == false 
      AND CloseDate >= DATE_TRUNC("month", NOW())
      AND CloseDate < DATE_TRUNC("month", NOW()) + INTERVAL 1 MONTH
      AND Probability >= ' || TO_STRING(min_probability);
  
  IF owner IS NOT NULL THEN
    SET search_query = search_query || ' AND Owner.Name LIKE "*' || owner || '*"';
  END IF;
  
  SET search_query = search_query || '
    | SORT CloseDate, Amount DESC';
  
  SET results = ESQL_QUERY(search_query);
  
  -- Categorize by forecast category
  DECLARE commit_deals ARRAY;
  DECLARE best_case_deals ARRAY;
  DECLARE pipeline_deals ARRAY;
  DECLARE commit_amount DECIMAL;
  DECLARE best_case_amount DECIMAL;
  DECLARE pipeline_amount DECIMAL;
  
  SET commit_deals = [];
  SET best_case_deals = [];
  SET pipeline_deals = [];
  SET commit_amount = 0;
  SET best_case_amount = 0;
  SET pipeline_amount = 0;
  
  FOR opp IN results LOOP
    DECLARE deal DOCUMENT;
    SET deal = {
      'id': opp.Id,
      'name': opp.Name,
      'account': opp.Account.Name,
      'owner': opp.Owner.Name,
      'amount': opp.Amount,
      'probability': opp.Probability,
      'stage': opp.StageName,
      'close_date': opp.CloseDate,
      'days_until_close': DATE_DIFF('day', CURRENT_DATE(), opp.CloseDate),
      'next_step': opp.NextStep
    };
    
    IF opp.Probability >= 90 OR opp.ForecastCategory == 'Commit' THEN
      SET commit_deals = ARRAY_APPEND(commit_deals, deal);
      SET commit_amount = commit_amount + opp.Amount;
    ELSEIF opp.Probability >= 70 OR opp.ForecastCategory == 'Best Case' THEN
      SET best_case_deals = ARRAY_APPEND(best_case_deals, deal);
      SET best_case_amount = best_case_amount + opp.Amount;
    ELSE
      SET pipeline_deals = ARRAY_APPEND(pipeline_deals, deal);
      SET pipeline_amount = pipeline_amount + opp.Amount;
    END IF;
  END LOOP;
  
  RETURN {
    'month': DATE_FORMAT(CURRENT_DATE(), 'yyyy-MM'),
    'summary': {
      'commit': {
        'count': ARRAY_LENGTH(commit_deals),
        'amount': commit_amount
      },
      'best_case': {
        'count': ARRAY_LENGTH(best_case_deals),
        'amount': best_case_amount
      },
      'pipeline': {
        'count': ARRAY_LENGTH(pipeline_deals),
        'amount': pipeline_amount
      },
      'total': {
        'count': ARRAY_LENGTH(results),
        'amount': commit_amount + best_case_amount + pipeline_amount
      }
    },
    'commit_deals': commit_deals,
    'best_case_deals': best_case_deals,
    'pipeline_deals': pipeline_deals,
    'generated_at': CURRENT_TIMESTAMP()
  };
END SKILL;
