CREATE SKILL sfdc_find_account
  VERSION '1.0.0'
  DESCRIPTION 'Find Salesforce accounts by name, domain, or ID. Returns matching accounts with key details.'
  AUTHOR 'moltler'
  TAGS ['sfdc', 'salesforce', 'account', 'lookup', 'crm']
  (
    query STRING DESCRIPTION 'Search query - account name, domain, or Salesforce ID',
    limit INTEGER DESCRIPTION 'Maximum results to return' DEFAULT 10
  )
  RETURNS ARRAY
BEGIN
  DECLARE results ARRAY;
  DECLARE search_query STRING;
  
  -- Build search query that matches name, domain, or ID
  SET search_query = 'FROM sfdc-accounts-* 
    | WHERE Name LIKE "*' || query || '*" 
       OR Website LIKE "*' || query || '*"
       OR Id == "' || query || '"
    | SORT LastModifiedDate DESC
    | LIMIT ' || CAST(limit AS STRING);
  
  SET results = ESQL_QUERY(search_query);
  
  -- Format results with key fields
  DECLARE formatted ARRAY;
  SET formatted = [];
  
  FOR account IN results LOOP
    SET formatted = ARRAY_APPEND(formatted, {
      'id': account.Id,
      'name': account.Name,
      'type': account.Type,
      'industry': account.Industry,
      'website': account.Website,
      'phone': account.Phone,
      'billing_city': account.BillingCity,
      'billing_country': account.BillingCountry,
      'owner': account.Owner.Name,
      'annual_revenue': account.AnnualRevenue,
      'employees': account.NumberOfEmployees,
      'last_modified': account.LastModifiedDate
    });
  END LOOP;
  
  RETURN formatted;
END SKILL;
