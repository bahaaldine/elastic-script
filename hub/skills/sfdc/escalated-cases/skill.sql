CREATE SKILL sfdc_escalated_cases
  VERSION '1.0.0'
  DESCRIPTION 'Get all currently escalated support cases with account and owner details. Critical for support management.'
  AUTHOR 'moltler'
  TAGS ['sfdc', 'salesforce', 'case', 'support', 'escalation', 'critical']
  (
    include_closed BOOLEAN DESCRIPTION 'Include recently closed escalated cases' DEFAULT FALSE,
    days_back INTEGER DESCRIPTION 'Days back to look for closed escalations' DEFAULT 7
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE open_escalated ARRAY;
  DECLARE closed_escalated ARRAY;
  
  -- Get open escalated cases
  SET open_escalated = ESQL_QUERY('FROM sfdc-cases-* 
    | WHERE IsEscalated == true AND IsClosed == false
    | SORT Priority DESC, CreatedDate
    | LIMIT 50');
  
  -- Optionally get recently closed escalated cases
  IF include_closed THEN
    SET closed_escalated = ESQL_QUERY('FROM sfdc-cases-* 
      | WHERE IsEscalated == true 
        AND IsClosed == true 
        AND ClosedDate >= NOW() - INTERVAL ' || CAST(days_back AS STRING) || ' DAYS
      | SORT ClosedDate DESC
      | LIMIT 20');
  ELSE
    SET closed_escalated = [];
  END IF;
  
  -- Format open cases
  DECLARE formatted_open ARRAY;
  SET formatted_open = [];
  
  FOR c IN open_escalated LOOP
    SET formatted_open = ARRAY_APPEND(formatted_open, {
      'id': c.Id,
      'case_number': c.CaseNumber,
      'subject': c.Subject,
      'account': c.Account.Name,
      'contact': c.Contact.Name,
      'priority': c.Priority,
      'status': c.Status,
      'owner': c.Owner.Name,
      'created_date': c.CreatedDate,
      'age_days': DATE_DIFF('day', c.CreatedDate, CURRENT_DATE()),
      'escalation_reason': c.EscalationReason,
      'risk_level': CASE 
        WHEN c.Priority == 'Critical' AND DATE_DIFF('day', c.CreatedDate, CURRENT_DATE()) > 1 THEN 'critical'
        WHEN c.Priority IN ('Critical', 'High') THEN 'high'
        ELSE 'medium'
      END
    });
  END LOOP;
  
  -- Calculate stats
  DECLARE critical_count INTEGER;
  DECLARE high_count INTEGER;
  DECLARE avg_age DECIMAL;
  SET critical_count = 0;
  SET high_count = 0;
  SET avg_age = 0;
  
  FOR c IN formatted_open LOOP
    IF c.priority == 'Critical' THEN
      SET critical_count = critical_count + 1;
    ELSEIF c.priority == 'High' THEN
      SET high_count = high_count + 1;
    END IF;
    SET avg_age = avg_age + c.age_days;
  END LOOP;
  
  IF ARRAY_LENGTH(formatted_open) > 0 THEN
    SET avg_age = avg_age / ARRAY_LENGTH(formatted_open);
  END IF;
  
  RETURN {
    'summary': {
      'total_open_escalations': ARRAY_LENGTH(formatted_open),
      'critical': critical_count,
      'high': high_count,
      'avg_age_days': ROUND(avg_age, 1),
      'recently_closed': ARRAY_LENGTH(closed_escalated)
    },
    'open_escalations': formatted_open,
    'recently_closed': closed_escalated,
    'generated_at': CURRENT_TIMESTAMP()
  };
END SKILL;
