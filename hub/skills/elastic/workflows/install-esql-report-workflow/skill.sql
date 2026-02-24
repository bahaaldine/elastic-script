CREATE SKILL install_esql_report_workflow
  VERSION '1.0.0'
  DESCRIPTION 'Install a scheduled ES|QL reporting workflow. Runs queries on schedule and outputs results.'
  AUTHOR 'elastic'
  TAGS ['workflows', 'esql', 'reporting', 'scheduled']
  (
    workflow_name STRING DESCRIPTION 'Name for this report workflow' DEFAULT 'Daily ESQL Report',
    esql_query STRING DESCRIPTION 'ES|QL query to run' DEFAULT 'FROM logs-* | STATS count = COUNT(*) BY log.level | SORT count DESC',
    schedule STRING DESCRIPTION 'Schedule interval (e.g., 1d, 6h, 30m)' DEFAULT '1d',
    kibana_url STRING DESCRIPTION 'Kibana URL' DEFAULT NULL
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE workflow_yaml STRING;
  DECLARE result DOCUMENT;
  
  SET workflow_yaml = '
name: "' || workflow_name || '"
description: Scheduled ES|QL report - runs query and outputs results
enabled: true
tags:
  - reporting
  - esql
  - scheduled

inputs:
  - name: query
    type: string
    description: ES|QL query to execute
    default: "' || REPLACE(esql_query, '"', '\"') || '"

triggers:
  - type: manual
  - type: scheduled
    with:
      every: "' || schedule || '"

steps:
  - name: run_esql_query
    type: elasticsearch.esql.query
    with:
      format: json
      query: "{{ inputs.query }}"

  - name: log_results
    type: console
    with:
      message: |
        === ES|QL Report: ' || workflow_name || ' ===
        Query: {{ inputs.query }}
        Rows returned: {{ steps.run_esql_query.output.values | size }}
        
        Results:
        {% for row in steps.run_esql_query.output.values %}
        {{ row | json }}
        {% endfor %}

  - name: store_count
    type: data.set
    with:
      report_name: "' || workflow_name || '"
      row_count: "{{ steps.run_esql_query.output.values | size }}"
      generated_at: "{{ \"now\" | date: \"%Y-%m-%dT%H:%M:%SZ\" }}"
';

  IF kibana_url IS NOT NULL THEN
    SET result = WORKFLOW_CREATE(workflow_yaml, kibana_url);
  ELSE
    SET result = WORKFLOW_CREATE(workflow_yaml);
  END IF;
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'installed',
      'workflow_name': workflow_name,
      'workflow_id': result.id,
      'schedule': schedule,
      'esql_query': esql_query,
      'message': 'ES|QL report workflow installed successfully',
      'usage': 'Trigger manually: RUN SKILL trigger_workflow(workflow_id => ''' || result.id || ''')'
    };
  ELSE
    RETURN {
      'status': 'failed',
      'error': result.error
    };
  END IF;
END SKILL;
