CREATE SKILL get_influencers
  VERSION '1.0.0'
  DESCRIPTION 'Get top influencers contributing to anomalies'
  AUTHOR 'elastic'
  TAGS ['ml,anomaly,influencers']
  (job_id STRING DESCRIPTION 'ML job ID', limit INT DESCRIPTION 'Max results' DEFAULT 10)
  RETURNS ARRAY
BEGIN
  RETURN [
    {'influencer_field': 'host', 'influencer_value': 'prod-web-01', 'score': 85},
    {'influencer_field': 'service', 'influencer_value': 'api-gateway', 'score': 72},
    {'influencer_field': 'user', 'influencer_value': 'admin', 'score': 65}
  ];
END SKILL;
