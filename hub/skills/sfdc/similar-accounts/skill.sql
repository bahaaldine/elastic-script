CREATE SKILL sfdc_similar_accounts
  VERSION '1.0.0'
  DESCRIPTION 'Find accounts similar to a reference account using vector similarity search. Great for prospecting and expansion plays.'
  AUTHOR 'moltler'
  TAGS ['sfdc', 'salesforce', 'account', 'ai', 'similar', 'vector-search', 'prospecting']
  (
    account_id STRING DESCRIPTION 'Reference Salesforce Account ID to find similar accounts',
    limit INTEGER DESCRIPTION 'Number of similar accounts to return' DEFAULT 10,
    exclude_customers BOOLEAN DESCRIPTION 'Exclude existing customers (Type = Customer)' DEFAULT FALSE
  )
  RETURNS ARRAY
BEGIN
  -- Get reference account
  DECLARE ref_account ARRAY;
  SET ref_account = ESQL_QUERY('FROM sfdc-accounts-* | WHERE Id == "' || account_id || '" | LIMIT 1');
  
  IF ARRAY_LENGTH(ref_account) == 0 THEN
    RETURN [{ 'error': 'Account not found', 'account_id': account_id }];
  END IF;
  
  DECLARE ref DOCUMENT;
  SET ref = ref_account[0];
  
  -- Build search vector from account attributes
  DECLARE search_text STRING;
  SET search_text = COALESCE(ref.Industry, '') || ' ' || 
                    COALESCE(ref.Description, '') || ' ' ||
                    COALESCE(TO_STRING(ref.NumberOfEmployees), '') || ' employees ' ||
                    COALESCE(ref.BillingCountry, '');
  
  -- Use semantic search to find similar accounts
  DECLARE similar ARRAY;
  DECLARE exclude_filter STRING;
  
  SET exclude_filter = 'Id != "' || account_id || '"';
  IF exclude_customers THEN
    SET exclude_filter = exclude_filter || ' AND Type != "Customer"';
  END IF;
  
  -- Query using ELSER or configured embedding model
  SET similar = ESQL_QUERY('FROM sfdc-accounts-* METADATA _score
    | WHERE ' || exclude_filter || '
    | WHERE MATCH(Description, "' || REPLACE(search_text, '"', '') || '")
       OR Industry == "' || COALESCE(ref.Industry, 'NONE') || '"
    | SORT _score DESC
    | LIMIT ' || TO_STRING(limit));
  
  -- Format results with similarity reasoning
  DECLARE results ARRAY;
  SET results = [];
  
  FOR acct IN similar LOOP
    DECLARE similarity_reasons ARRAY;
    SET similarity_reasons = [];
    
    IF acct.Industry == ref.Industry THEN
      SET similarity_reasons = ARRAY_APPEND(similarity_reasons, 'Same industry: ' || ref.Industry);
    END IF;
    
    IF acct.NumberOfEmployees IS NOT NULL AND ref.NumberOfEmployees IS NOT NULL THEN
      DECLARE size_ratio DECIMAL;
      SET size_ratio = acct.NumberOfEmployees * 1.0 / ref.NumberOfEmployees;
      IF size_ratio >= 0.5 AND size_ratio <= 2.0 THEN
        SET similarity_reasons = ARRAY_APPEND(similarity_reasons, 'Similar company size');
      END IF;
    END IF;
    
    IF acct.BillingCountry == ref.BillingCountry THEN
      SET similarity_reasons = ARRAY_APPEND(similarity_reasons, 'Same country: ' || ref.BillingCountry);
    END IF;
    
    SET results = ARRAY_APPEND(results, {
      'id': acct.Id,
      'name': acct.Name,
      'industry': acct.Industry,
      'type': acct.Type,
      'employees': acct.NumberOfEmployees,
      'annual_revenue': acct.AnnualRevenue,
      'country': acct.BillingCountry,
      'website': acct.Website,
      'owner': acct.Owner.Name,
      'similarity_reasons': similarity_reasons,
      'similarity_score': acct._score
    });
  END LOOP;
  
  RETURN results;
END SKILL;
