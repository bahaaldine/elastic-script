CREATE SKILL sfdc_notify_deal_closed
  VERSION '1.0.0'
  DESCRIPTION 'Send celebratory Slack notification when a deal is marked as Closed Won. Perfect for team motivation.'
  AUTHOR 'moltler'
  TAGS ['sfdc', 'salesforce', 'workflow', 'notification', 'slack', 'closed-won', 'celebration']
  (
    opportunity_id STRING DESCRIPTION 'Salesforce Opportunity ID that was just closed',
    slack_channel STRING DESCRIPTION 'Slack channel for celebration' DEFAULT '#wins'
  )
  RETURNS DOCUMENT
BEGIN
  -- Get opportunity details
  DECLARE opp_data ARRAY;
  SET opp_data = ESQL_QUERY('FROM sfdc-opportunities-* 
    | WHERE Id == "' || opportunity_id || '" 
    | LIMIT 1');
  
  IF ARRAY_LENGTH(opp_data) == 0 THEN
    RETURN { 'error': 'Opportunity not found', 'opportunity_id': opportunity_id };
  END IF;
  
  DECLARE opp DOCUMENT;
  SET opp = opp_data[0];
  
  IF opp.IsWon != true THEN
    RETURN { 
      'error': 'Opportunity is not Closed Won', 
      'opportunity_id': opportunity_id,
      'stage': opp.StageName 
    };
  END IF;
  
  -- Build celebration message
  DECLARE amount_formatted STRING;
  SET amount_formatted = '$' || TO_STRING(opp.Amount);
  
  DECLARE message_blocks ARRAY;
  SET message_blocks = [
    {
      'type': 'header',
      'text': {
        'type': 'plain_text',
        'text': '🎉 Deal Closed Won!'
      }
    },
    {
      'type': 'section',
      'text': {
        'type': 'mrkdwn',
        'text': '*' || opp.Name || '*\n\n' ||
                '💰 *Amount:* ' || amount_formatted || '\n' ||
                '🏢 *Account:* ' || opp.Account.Name || '\n' ||
                '👤 *Closed by:* ' || opp.Owner.Name || '\n' ||
                '📅 *Close Date:* ' || DATE_FORMAT(opp.CloseDate, 'MMMM d, yyyy')
      }
    },
    {
      'type': 'section',
      'text': {
        'type': 'mrkdwn',
        'text': 'Congratulations to ' || opp.Owner.Name || ' on this win! 🏆'
      }
    }
  ];
  
  -- Add deal details
  IF opp.Type IS NOT NULL THEN
    SET message_blocks = ARRAY_APPEND(message_blocks, {
      'type': 'context',
      'elements': [
        {
          'type': 'mrkdwn',
          'text': 'Deal Type: ' || opp.Type || ' | Lead Source: ' || COALESCE(opp.LeadSource, 'N/A')
        }
      ]
    });
  END IF;
  
  -- Send Slack notification
  DECLARE slack_result DOCUMENT;
  SET slack_result = SLACK_SEND_BLOCKS(slack_channel, message_blocks);
  
  -- Log the win for analytics
  DECLARE log_entry DOCUMENT;
  SET log_entry = {
    '@timestamp': CURRENT_TIMESTAMP(),
    'event.type': 'deal_closed_won',
    'opportunity.id': opp.Id,
    'opportunity.name': opp.Name,
    'opportunity.amount': opp.Amount,
    'account.id': opp.Account.Id,
    'account.name': opp.Account.Name,
    'owner.name': opp.Owner.Name,
    'deal.type': opp.Type,
    'lead_source': opp.LeadSource
  };
  
  INDEX_DOCUMENT('sfdc-wins-log', log_entry);
  
  RETURN {
    'status': 'success',
    'message': 'Celebration notification sent!',
    'opportunity': {
      'id': opp.Id,
      'name': opp.Name,
      'amount': opp.Amount,
      'account': opp.Account.Name,
      'owner': opp.Owner.Name
    },
    'slack_sent': slack_result.ok,
    'channel': slack_channel,
    'notified_at': CURRENT_TIMESTAMP()
  };
END SKILL;
