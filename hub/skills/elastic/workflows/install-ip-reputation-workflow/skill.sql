CREATE SKILL install_ip_reputation_workflow
  VERSION '1.0.0'
  DESCRIPTION 'Install the IP Reputation Check workflow from elastic/workflows. Checks IPs against AbuseIPDB and enriches with geolocation.'
  AUTHOR 'elastic'
  TAGS ['workflows', 'security', 'enrichment', 'threat-intel']
  (
    abuseipdb_api_key STRING DESCRIPTION 'Your AbuseIPDB API key (get from abuseipdb.com)',
    kibana_url STRING DESCRIPTION 'Kibana URL' DEFAULT NULL
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE workflow_yaml STRING;
  DECLARE result DOCUMENT;
  
  SET workflow_yaml = '
name: IP Reputation Check
description: Check IP address reputation using AbuseIPDB and enrich with geolocation data.
enabled: true
tags:
  - security
  - enrichment
  - threat-intel

consts:
  api_key: "' || abuseipdb_api_key || '"
  abuseipdb_base_url: https://api.abuseipdb.com/api/v2
  ipapi_base_url: http://ip-api.com/json

inputs:
  - name: ip_address
    type: string
    description: The IP address to check
    required: true

triggers:
  - type: manual

steps:
  - name: check_abuseipdb
    type: http
    with:
      url: "{{ consts.abuseipdb_base_url }}/check?ipAddress={{ inputs.ip_address }}&maxAgeInDays=90&verbose=true"
      method: GET
      headers:
        Key: "{{ consts.api_key }}"
        Accept: application/json
    on-failure:
      retry:
        max-attempts: 2
        delay: 3s
      continue: true

  - name: get_geolocation
    type: http
    with:
      url: "{{ consts.ipapi_base_url }}/{{ inputs.ip_address }}?fields=status,country,countryCode,regionName,city,isp,org,proxy,hosting"
      method: GET
    on-failure:
      retry:
        max-attempts: 2
        delay: 2s
      continue: true

  - name: format_results
    type: console
    with:
      message: |
        === IP Threat Intelligence Report ===
        IP Address: {{ inputs.ip_address }}
        
        AbuseIPDB Results:
        - Abuse Confidence Score: {{ steps.check_abuseipdb.output.data.data.abuseConfidenceScore }}%
        - Total Reports: {{ steps.check_abuseipdb.output.data.data.totalReports }}
        - Country: {{ steps.check_abuseipdb.output.data.data.countryCode }}
        - ISP: {{ steps.check_abuseipdb.output.data.data.isp }}
        
        Geolocation:
        - Country: {{ steps.get_geolocation.output.data.country }}
        - City: {{ steps.get_geolocation.output.data.city }}
        - ISP: {{ steps.get_geolocation.output.data.isp }}
        - Is Proxy: {{ steps.get_geolocation.output.data.proxy }}
        - Is Hosting: {{ steps.get_geolocation.output.data.hosting }}
';

  IF kibana_url IS NOT NULL THEN
    SET result = WORKFLOW_CREATE(workflow_yaml, kibana_url);
  ELSE
    SET result = WORKFLOW_CREATE(workflow_yaml);
  END IF;
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'installed',
      'workflow_name': 'IP Reputation Check',
      'workflow_id': result.id,
      'message': 'IP Reputation workflow installed successfully',
      'usage': 'Trigger with: RUN SKILL trigger_workflow(workflow_id => ''' || result.id || ''', inputs => {''ip_address'': ''8.8.8.8''})',
      'source': 'https://github.com/elastic/workflows'
    };
  ELSE
    RETURN {
      'status': 'failed',
      'error': result.error
    };
  END IF;
END SKILL;
