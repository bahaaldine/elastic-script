CREATE SKILL create_servicenow_incident
  VERSION '1.0.0'
  DESCRIPTION 'Create a ServiceNow incident'
  AUTHOR 'elastic'
  TAGS ['integrations', 'servicenow', 'incidents']
  (short_description STRING DESCRIPTION 'Incident title', description STRING DESCRIPTION 'Incident details', urgency INT DESCRIPTION 'Urgency 1-3' DEFAULT 2)
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'status': 'created',
    'platform': 'ServiceNow',
    'incident_number': 'INC0012345',
    'urgency': urgency
  };
END SKILL;
