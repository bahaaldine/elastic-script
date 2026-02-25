CREATE SKILL sfdc_case_volume_trends
  VERSION '1.0.0'
  DESCRIPTION 'Analyze support case volume trends over time. Shows case creation, resolution, and backlog trends.'
  AUTHOR 'moltler'
  TAGS ['sfdc', 'salesforce', 'case', 'support', 'analytics', 'trends']
  (
    time_period STRING DESCRIPTION 'Time period: last_7_days, last_30_days, last_90_days' DEFAULT 'last_30_days',
    group_by STRING DESCRIPTION 'Time grouping: day, week, month' DEFAULT 'day'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE created_data ARRAY;
  DECLARE closed_data ARRAY;
  DECLARE date_filter STRING;
  DECLARE date_trunc STRING;
  
  -- Set filters
  SET date_filter = CASE time_period
    WHEN 'last_7_days' THEN 'CreatedDate >= NOW() - INTERVAL 7 DAYS'
    WHEN 'last_30_days' THEN 'CreatedDate >= NOW() - INTERVAL 30 DAYS'
    WHEN 'last_90_days' THEN 'CreatedDate >= NOW() - INTERVAL 90 DAYS'
    ELSE 'CreatedDate >= NOW() - INTERVAL 30 DAYS'
  END;
  
  SET date_trunc = CASE group_by
    WHEN 'day' THEN 'day'
    WHEN 'week' THEN 'week'
    WHEN 'month' THEN 'month'
    ELSE 'day'
  END;
  
  -- Cases created over time
  SET created_data = ESQL_QUERY('FROM sfdc-cases-* 
    | WHERE ' || date_filter || '
    | STATS 
        created = COUNT(*),
        high_priority = SUM(CASE WHEN Priority IN ("High", "Critical") THEN 1 ELSE 0 END)
      BY DATE_TRUNC("' || date_trunc || '", CreatedDate) AS period
    | SORT period');
  
  -- Cases closed over time
  SET closed_data = ESQL_QUERY('FROM sfdc-cases-* 
    | WHERE ClosedDate IS NOT NULL AND ClosedDate >= NOW() - INTERVAL 30 DAYS
    | STATS closed = COUNT(*) BY DATE_TRUNC("' || date_trunc || '", ClosedDate) AS period
    | SORT period');
  
  -- Current open case stats
  DECLARE open_stats ARRAY;
  SET open_stats = ESQL_QUERY('FROM sfdc-cases-* 
    | WHERE IsClosed == false
    | STATS 
        total_open = COUNT(*),
        high_priority = SUM(CASE WHEN Priority IN ("High", "Critical") THEN 1 ELSE 0 END),
        escalated = SUM(CASE WHEN IsEscalated == true THEN 1 ELSE 0 END),
        avg_age_days = AVG(DATE_DIFF("day", CreatedDate, NOW()))
      BY Priority
    | SORT total_open DESC');
  
  RETURN {
    'period': time_period,
    'grouped_by': group_by,
    'created_trend': created_data,
    'closed_trend': closed_data,
    'current_backlog': open_stats,
    'generated_at': CURRENT_TIMESTAMP()
  };
END SKILL;
