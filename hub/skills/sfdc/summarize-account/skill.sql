CREATE SKILL sfdc_summarize_account
  VERSION '1.0.0'
  DESCRIPTION 'Generate an AI-powered summary of an account relationship including opportunities, cases, and activity history.'
  AUTHOR 'moltler'
  TAGS ['sfdc', 'salesforce', 'account', 'ai', 'summary', 'llm']
  (
    account_id STRING DESCRIPTION 'Salesforce Account ID',
    model_id STRING DESCRIPTION 'Inference endpoint for summarization' DEFAULT '.elser-2-elasticsearch'
  )
  RETURNS DOCUMENT
BEGIN
  -- Get account details
  DECLARE account ARRAY;
  SET account = ESQL_QUERY('FROM sfdc-accounts-* | WHERE Id == "' || account_id || '" | LIMIT 1');
  
  IF ARRAY_LENGTH(account) == 0 THEN
    RETURN { 'error': 'Account not found', 'account_id': account_id };
  END IF;
  
  DECLARE acct DOCUMENT;
  SET acct = account[0];
  
  -- Get recent opportunities
  DECLARE opportunities ARRAY;
  SET opportunities = ESQL_QUERY('FROM sfdc-opportunities-* 
    | WHERE Account.Id == "' || account_id || '"
    | SORT CloseDate DESC
    | LIMIT 10');
  
  -- Get open cases
  DECLARE cases ARRAY;
  SET cases = ESQL_QUERY('FROM sfdc-cases-* 
    | WHERE Account.Id == "' || account_id || '" AND IsClosed == false
    | SORT CreatedDate DESC
    | LIMIT 5');
  
  -- Get recent activities
  DECLARE activities ARRAY;
  SET activities = ESQL_QUERY('FROM sfdc-tasks-* 
    | WHERE Account.Id == "' || account_id || '"
    | SORT ActivityDate DESC
    | LIMIT 10');
  
  -- Calculate key metrics
  DECLARE total_revenue DECIMAL;
  DECLARE open_pipeline DECIMAL;
  DECLARE win_count INTEGER;
  DECLARE loss_count INTEGER;
  
  SET total_revenue = 0;
  SET open_pipeline = 0;
  SET win_count = 0;
  SET loss_count = 0;
  
  FOR opp IN opportunities LOOP
    IF opp.IsWon == true THEN
      SET total_revenue = total_revenue + opp.Amount;
      SET win_count = win_count + 1;
    ELSEIF opp.IsClosed == false THEN
      SET open_pipeline = open_pipeline + opp.Amount;
    ELSE
      SET loss_count = loss_count + 1;
    END IF;
  END LOOP;
  
  -- Build context for LLM
  DECLARE context STRING;
  SET context = 'Account: ' || acct.Name || '
Industry: ' || COALESCE(acct.Industry, 'Unknown') || '
Type: ' || COALESCE(acct.Type, 'Unknown') || '
Annual Revenue: $' || COALESCE(TO_STRING(acct.AnnualRevenue), 'Unknown') || '
Employees: ' || COALESCE(TO_STRING(acct.NumberOfEmployees), 'Unknown') || '
Owner: ' || acct.Owner.Name || '

Revenue from this account: $' || TO_STRING(total_revenue) || '
Open pipeline: $' || TO_STRING(open_pipeline) || '
Won deals: ' || TO_STRING(win_count) || '
Lost deals: ' || TO_STRING(loss_count) || '
Open support cases: ' || TO_STRING(ARRAY_LENGTH(cases)) || '
Recent activities: ' || TO_STRING(ARRAY_LENGTH(activities));
  
  -- Generate AI summary
  DECLARE prompt STRING;
  SET prompt = 'You are a sales analyst. Based on the following account data, provide a brief executive summary (3-4 sentences) covering: 
1. Account health and engagement level
2. Revenue potential and current pipeline
3. Any concerns or recommended actions

Account Data:
' || context;

  DECLARE ai_response DOCUMENT;
  SET ai_response = INFERENCE_CHAT(model_id, prompt, { 'temperature': 0.3, 'max_tokens': 300 });
  
  RETURN {
    'account': {
      'id': acct.Id,
      'name': acct.Name,
      'industry': acct.Industry,
      'type': acct.Type,
      'owner': acct.Owner.Name
    },
    'metrics': {
      'total_revenue': total_revenue,
      'open_pipeline': open_pipeline,
      'won_deals': win_count,
      'lost_deals': loss_count,
      'open_cases': ARRAY_LENGTH(cases),
      'recent_activities': ARRAY_LENGTH(activities)
    },
    'ai_summary': ai_response.content,
    'recent_opportunities': opportunities,
    'open_cases': cases,
    'generated_at': CURRENT_TIMESTAMP()
  };
END SKILL;
