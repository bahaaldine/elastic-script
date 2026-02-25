CREATE SKILL sfdc_pipeline_summary
  VERSION '1.0.0'
  DESCRIPTION 'Get current sales pipeline summary grouped by stage. Shows total opportunities, amount, and weighted pipeline per stage.'
  AUTHOR 'moltler'
  TAGS ['sfdc', 'salesforce', 'pipeline', 'analytics', 'sales', 'forecast']
  (
    owner STRING DESCRIPTION 'Filter by opportunity owner name' DEFAULT NULL,
    min_amount INTEGER DESCRIPTION 'Minimum opportunity amount to include' DEFAULT 0
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE pipeline_data ARRAY;
  DECLARE search_query STRING;
  
  -- Build query for open pipeline
  SET search_query = 'FROM sfdc-opportunities-* 
    | WHERE IsClosed == false 
      AND Amount >= ' || CAST(min_amount AS STRING);
  
  IF owner IS NOT NULL THEN
    SET search_query = search_query || ' AND Owner.Name LIKE "*' || owner || '*"';
  END IF;
  
  SET search_query = search_query || '
    | STATS 
        count = COUNT(*),
        total_amount = SUM(Amount),
        avg_amount = AVG(Amount),
        avg_probability = AVG(Probability)
      BY StageName
    | SORT total_amount DESC';
  
  SET pipeline_data = ESQL_QUERY(search_query);
  
  -- Calculate totals and weighted pipeline
  DECLARE total_count INTEGER;
  DECLARE total_amount DECIMAL;
  DECLARE weighted_pipeline DECIMAL;
  DECLARE stages ARRAY;
  
  SET total_count = 0;
  SET total_amount = 0;
  SET weighted_pipeline = 0;
  SET stages = [];
  
  FOR stage IN pipeline_data LOOP
    SET total_count = total_count + stage.count;
    SET total_amount = total_amount + stage.total_amount;
    SET weighted_pipeline = weighted_pipeline + (stage.total_amount * stage.avg_probability / 100);
    
    SET stages = ARRAY_APPEND(stages, {
      'stage': stage.StageName,
      'count': stage.count,
      'total_amount': stage.total_amount,
      'avg_amount': ROUND(stage.avg_amount, 2),
      'avg_probability': ROUND(stage.avg_probability, 1),
      'weighted_amount': ROUND(stage.total_amount * stage.avg_probability / 100, 2)
    });
  END LOOP;
  
  RETURN {
    'summary': {
      'total_opportunities': total_count,
      'total_pipeline': total_amount,
      'weighted_pipeline': ROUND(weighted_pipeline, 2),
      'avg_deal_size': CASE WHEN total_count > 0 THEN ROUND(total_amount / total_count, 2) ELSE 0 END
    },
    'by_stage': stages,
    'generated_at': CURRENT_TIMESTAMP()
  };
END SKILL;
