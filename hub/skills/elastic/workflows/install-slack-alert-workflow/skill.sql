CREATE SKILL install_slack_alert_workflow
  VERSION '1.0.0'
  DESCRIPTION 'Install a workflow that sends Slack notifications when triggered. Useful for alerting integrations.'
  AUTHOR 'elastic'
  TAGS ['workflows', 'slack', 'alerting', 'notifications']
  (
    slack_webhook_url STRING DESCRIPTION 'Slack webhook URL (get from Slack app settings)',
    default_channel STRING DESCRIPTION 'Default Slack channel' DEFAULT '#alerts',
    kibana_url STRING DESCRIPTION 'Kibana URL' DEFAULT NULL
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE workflow_yaml STRING;
  DECLARE result DOCUMENT;
  
  SET workflow_yaml = '
name: Slack Alert Notification
description: Send alert notifications to Slack channel
enabled: true
tags:
  - alerting
  - slack
  - notifications

consts:
  slack_webhook: "' || slack_webhook_url || '"
  default_channel: "' || default_channel || '"

inputs:
  - name: message
    type: string
    description: Alert message to send
    required: true
  - name: severity
    type: string
    description: Alert severity (critical, high, medium, low)
    default: medium
  - name: channel
    type: string
    description: Slack channel to post to
    default: "' || default_channel || '"

triggers:
  - type: manual
  - type: alert

steps:
  - name: format_message
    type: data.set
    with:
      emoji: |
        {% case inputs.severity %}
          {% when "critical" %}🔴
          {% when "high" %}🟠
          {% when "medium" %}🟡
          {% else %}🟢
        {% endcase %}
      formatted_message: "{{ emoji }} [{{ inputs.severity | upcase }}] {{ inputs.message }}"

  - name: send_slack
    type: http
    with:
      url: "{{ consts.slack_webhook }}"
      method: POST
      headers:
        Content-Type: application/json
      body:
        channel: "{{ inputs.channel }}"
        username: Elastic Alerts
        icon_emoji: ":elastic:"
        text: "{{ steps.format_message.output.formatted_message }}"
        attachments:
          - color: |
              {% case inputs.severity %}
                {% when "critical" %}danger
                {% when "high" %}warning
                {% else %}good
              {% endcase %}
            fields:
              - title: Severity
                value: "{{ inputs.severity | upcase }}"
                short: true
              - title: Time
                value: "{{ \"now\" | date: \"%Y-%m-%d %H:%M:%S\" }}"
                short: true
    on-failure:
      retry:
        max-attempts: 3
        delay: 5s

  - name: log_result
    type: console
    with:
      message: "Slack notification sent to {{ inputs.channel }}: {{ inputs.message }}"
';

  IF kibana_url IS NOT NULL THEN
    SET result = WORKFLOW_CREATE(workflow_yaml, kibana_url);
  ELSE
    SET result = WORKFLOW_CREATE(workflow_yaml);
  END IF;
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'installed',
      'workflow_name': 'Slack Alert Notification',
      'workflow_id': result.id,
      'message': 'Slack alert workflow installed successfully',
      'usage': 'Trigger with: RUN SKILL trigger_workflow(workflow_id => ''' || result.id || ''', inputs => {''message'': ''Test alert'', ''severity'': ''high''})'
    };
  ELSE
    RETURN {
      'status': 'failed',
      'error': result.error
    };
  END IF;
END SKILL;
