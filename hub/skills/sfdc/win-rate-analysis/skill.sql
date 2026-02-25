CREATE SKILL sfdc_win_rate_analysis
  VERSION '1.0.0'
  DESCRIPTION 'Analyze win/loss rates by owner, lead source, or industry. Shows conversion metrics and trends.'
  AUTHOR 'moltler'
  TAGS ['sfdc', 'salesforce', 'win-rate', 'analytics', 'sales', 'performance']
  (
    group_by STRING DESCRIPTION 'Group results by: owner, lead_source, industry, type' DEFAULT 'owner',
    time_period STRING DESCRIPTION 'Time period: this_month, this_quarter, this_year, last_90_days' DEFAULT 'this_quarter'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE results ARRAY;
  DECLARE date_filter STRING;
  DECLARE group_field STRING;
  
  -- Set date filter
  SET date_filter = CASE time_period
    WHEN 'this_month' THEN 'CloseDate >= DATE_TRUNC("month", NOW())'
    WHEN 'this_quarter' THEN 'CloseDate >= DATE_TRUNC("quarter", NOW())'
    WHEN 'this_year' THEN 'CloseDate >= DATE_TRUNC("year", NOW())'
    WHEN 'last_90_days' THEN 'CloseDate >= NOW() - INTERVAL 90 DAYS'
    ELSE 'CloseDate >= DATE_TRUNC("quarter", NOW())'
  END;
  
  -- Set group field
  SET group_field = CASE group_by
    WHEN 'owner' THEN 'Owner.Name'
    WHEN 'lead_source' THEN 'LeadSource'
    WHEN 'industry' THEN 'Account.Industry'
    WHEN 'type' THEN 'Type'
    ELSE 'Owner.Name'
  END;
  
  -- Query closed opportunities
  SET results = ESQL_QUERY('FROM sfdc-opportunities-* 
    | WHERE IsClosed == true AND ' || date_filter || '
    | STATS 
        total = COUNT(*),
        won = SUM(CASE WHEN IsWon == true THEN 1 ELSE 0 END),
        lost = SUM(CASE WHEN IsWon == false THEN 1 ELSE 0 END),
        won_amount = SUM(CASE WHEN IsWon == true THEN Amount ELSE 0 END),
        lost_amount = SUM(CASE WHEN IsWon == false THEN Amount ELSE 0 END),
        avg_cycle_days = AVG(CASE WHEN IsWon == true THEN DATE_DIFF("day", CreatedDate, CloseDate) ELSE NULL END)
      BY ' || group_field || '
    | SORT won_amount DESC');
  
  -- Format results with win rate calculation
  DECLARE formatted ARRAY;
  SET formatted = [];
  DECLARE total_won INTEGER;
  DECLARE total_lost INTEGER;
  SET total_won = 0;
  SET total_lost = 0;
  
  FOR row IN results LOOP
    DECLARE win_rate DECIMAL;
    SET win_rate = CASE WHEN row.total > 0 THEN ROUND(row.won * 100.0 / row.total, 1) ELSE 0 END;
    
    SET total_won = total_won + row.won;
    SET total_lost = total_lost + row.lost;
    
    SET formatted = ARRAY_APPEND(formatted, {
      'group': row[group_field],
      'total_deals': row.total,
      'won': row.won,
      'lost': row.lost,
      'win_rate_pct': win_rate,
      'won_amount': row.won_amount,
      'lost_amount': row.lost_amount,
      'avg_cycle_days': ROUND(row.avg_cycle_days, 0)
    });
  END LOOP;
  
  RETURN {
    'period': time_period,
    'grouped_by': group_by,
    'overall': {
      'total_won': total_won,
      'total_lost': total_lost,
      'overall_win_rate': CASE WHEN (total_won + total_lost) > 0 
        THEN ROUND(total_won * 100.0 / (total_won + total_lost), 1) 
        ELSE 0 END
    },
    'breakdown': formatted,
    'generated_at': CURRENT_TIMESTAMP()
  };
END SKILL;
