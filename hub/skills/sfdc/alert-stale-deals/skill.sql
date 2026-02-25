CREATE SKILL sfdc_alert_stale_deals
  VERSION '1.0.0'
  DESCRIPTION 'Create a workflow that monitors for stale opportunities and sends Slack alerts to deal owners.'
  AUTHOR 'moltler'
  TAGS ['sfdc', 'salesforce', 'workflow', 'alert', 'slack', 'automation']
  (
    stale_days INTEGER DESCRIPTION 'Days without activity to trigger alert' DEFAULT 14,
    slack_channel STRING DESCRIPTION 'Slack channel for alerts' DEFAULT '#sales-alerts',
    min_amount INTEGER DESCRIPTION 'Minimum deal amount to monitor' DEFAULT 10000
  )
  RETURNS DOCUMENT
BEGIN
  -- Find stale opportunities
  DECLARE stale_opps ARRAY;
  SET stale_opps = ESQL_QUERY('FROM sfdc-opportunities-* 
    | WHERE IsClosed == false 
      AND Amount >= ' || CAST(min_amount AS STRING) || '
      AND LastActivityDate < NOW() - INTERVAL ' || CAST(stale_days AS STRING) || ' DAYS
    | SORT Amount DESC
    | LIMIT 20');
  
  IF ARRAY_LENGTH(stale_opps) == 0 THEN
    RETURN {
      'status': 'no_action',
      'message': 'No stale opportunities found',
      'checked_at': CURRENT_TIMESTAMP()
    };
  END IF;
  
  -- Build alert message
  DECLARE alert_blocks ARRAY;
  SET alert_blocks = [
    {
      'type': 'header',
      'text': {
        'type': 'plain_text',
        'text': '⚠️ Stale Opportunities Alert'
      }
    },
    {
      'type': 'section',
      'text': {
        'type': 'mrkdwn',
        'text': '*' || CAST(ARRAY_LENGTH(stale_opps) AS STRING) || ' deals* have had no activity in ' || CAST(stale_days AS STRING) || '+ days'
      }
    }
  ];
  
  -- Add each stale opp to alert
  FOR opp IN stale_opps LOOP
    DECLARE days_stale INTEGER;
    SET days_stale = DATE_DIFF('day', opp.LastActivityDate, CURRENT_DATE());
    
    SET alert_blocks = ARRAY_APPEND(alert_blocks, {
      'type': 'section',
      'text': {
        'type': 'mrkdwn',
        'text': '*' || opp.Name || '* - $' || CAST(opp.Amount AS STRING) || '\n' ||
                'Owner: ' || opp.Owner.Name || ' | Stage: ' || opp.StageName || '\n' ||
                '🔴 ' || CAST(days_stale AS STRING) || ' days since last activity'
      }
    });
  END LOOP;
  
  -- Send Slack alert
  DECLARE slack_result DOCUMENT;
  SET slack_result = SLACK_SEND_BLOCKS(slack_channel, alert_blocks);
  
  -- Create workflow definition for scheduled monitoring
  DECLARE workflow_yaml STRING;
  SET workflow_yaml = 'name: sfdc-stale-deals-monitor
description: Monitor for stale Salesforce opportunities
schedule:
  interval: 1d
triggers:
  - type: schedule
    cron: "0 9 * * 1-5"  # 9 AM weekdays
steps:
  - name: find_stale_deals
    skill: sfdc_stale_opportunities
    params:
      stale_days: ' || CAST(stale_days AS STRING) || '
      min_amount: ' || CAST(min_amount AS STRING) || '
  - name: alert_if_found
    condition: "{{ steps.find_stale_deals.result | length > 0 }}"
    action: slack_send
    params:
      channel: "' || slack_channel || '"
      message: "Found {{ steps.find_stale_deals.result | length }} stale deals"';
  
  -- Deploy workflow
  DECLARE workflow_result DOCUMENT;
  SET workflow_result = WORKFLOW_CREATE('sfdc-stale-deals-monitor', workflow_yaml);
  
  RETURN {
    'status': 'success',
    'stale_deals_found': ARRAY_LENGTH(stale_opps),
    'slack_alert_sent': slack_result.ok,
    'workflow_created': workflow_result.success,
    'workflow_id': workflow_result.workflow_id,
    'deals': stale_opps,
    'created_at': CURRENT_TIMESTAMP()
  };
END SKILL;
