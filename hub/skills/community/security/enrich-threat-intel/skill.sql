CREATE SKILL enrich_threat_intel
  VERSION '1.0.0'
  DESCRIPTION 'Enrich security events with threat intelligence from configured feeds.'
  AUTHOR 'security-team'
  TAGS ['security', 'threat-intel', 'enrichment']
  (
    indicator STRING DESCRIPTION 'IP, domain, or hash to look up',
    indicator_type STRING DESCRIPTION 'Type: ip, domain, file_hash' DEFAULT 'ip'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE enrichment DOCUMENT;
  DECLARE matches ARRAY;
  
  -- Search threat intel index
  SET matches = ESQL_QUERY('
    FROM threat-intel-*
    | WHERE indicator.value == "' || indicator || '"
    | KEEP indicator.type, threat.indicator.type, threat.indicator.description, 
           threat.indicator.provider, threat.indicator.confidence
    | LIMIT 5
  ');
  
  SET enrichment = {
    'indicator': indicator,
    'indicator_type': indicator_type,
    'matches': matches,
    'is_malicious': ARRAY_LENGTH(matches) > 0,
    'enriched_at': CURRENT_TIMESTAMP()
  };
  
  RETURN enrichment;
END SKILL;
