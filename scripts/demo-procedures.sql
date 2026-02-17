-- =============================================================================
-- Moltler Demo Procedures
-- =============================================================================
-- A collection of example procedures demonstrating elastic-script capabilities
-- These are loaded automatically when running: ./scripts/quick-start.sh --moltler
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Hello World - The simplest procedure
-- -----------------------------------------------------------------------------
CREATE PROCEDURE hello_world()
BEGIN
  PRINT 'Hello from Moltler!';
  PRINT 'elastic-script is running inside Elasticsearch.';
END PROCEDURE;

-- -----------------------------------------------------------------------------
-- 2. Log Analysis - Query and summarize log data
-- -----------------------------------------------------------------------------
CREATE PROCEDURE analyze_logs(log_index STRING DEFAULT 'logs-sample')
BEGIN
  DECLARE results ARRAY;
  DECLARE error_count NUMBER;
  DECLARE total_count NUMBER;
  
  -- Get total log count
  SET results = ESQL_QUERY('FROM ' || log_index || ' | STATS total = COUNT(*)');
  SET total_count = DOCUMENT_GET(results[0], 'total');
  
  -- Get error count
  SET results = ESQL_QUERY('FROM ' || log_index || ' | WHERE level = "ERROR" | STATS errors = COUNT(*)');
  SET error_count = DOCUMENT_GET(results[0], 'errors');
  
  -- Get top error messages
  SET results = ESQL_QUERY('FROM ' || log_index || ' | WHERE level = "ERROR" | STATS count = COUNT(*) BY message | SORT count DESC | LIMIT 5');
  
  PRINT 'Log Analysis Summary';
  PRINT '====================';
  PRINT 'Total logs: ' || total_count;
  PRINT 'Error logs: ' || error_count;
  PRINT 'Error rate: ' || ROUND((error_count * 100.0 / total_count), 2) || '%';
  PRINT '';
  PRINT 'Top 5 Error Messages:';
  
  FOR i IN 1..ARRAY_LENGTH(results) LOOP
    PRINT '  - ' || DOCUMENT_GET(results[i-1], 'message') || ' (' || DOCUMENT_GET(results[i-1], 'count') || ' occurrences)';
  END LOOP;
  
  RETURN results;
END PROCEDURE;

-- -----------------------------------------------------------------------------
-- 3. User Statistics - Analyze user data
-- -----------------------------------------------------------------------------
CREATE PROCEDURE get_user_stats()
BEGIN
  DECLARE users ARRAY;
  DECLARE admins ARRAY;
  DECLARE active_users ARRAY;
  
  -- Get all users
  SET users = ESQL_QUERY('FROM users-sample | STATS total = COUNT(*)');
  
  -- Get admin count
  SET admins = ESQL_QUERY('FROM users-sample | WHERE role = "admin" | STATS count = COUNT(*)');
  
  -- Get active users (last 30 days)
  SET active_users = ESQL_QUERY('FROM users-sample | WHERE status = "active" | STATS count = COUNT(*)');
  
  RETURN {
    "total_users": DOCUMENT_GET(users[0], 'total'),
    "admin_count": DOCUMENT_GET(admins[0], 'count'),
    "active_users": DOCUMENT_GET(active_users[0], 'count')
  };
END PROCEDURE;

-- -----------------------------------------------------------------------------
-- 4. Metric Aggregation - Calculate system metrics
-- -----------------------------------------------------------------------------
CREATE PROCEDURE aggregate_metrics(metric_name STRING, time_range STRING DEFAULT '1h')
BEGIN
  DECLARE results ARRAY;
  DECLARE query STRING;
  
  SET query = 'FROM metrics-sample | WHERE metric_name = "' || metric_name || '" | STATS avg_value = AVG(value), max_value = MAX(value), min_value = MIN(value)';
  SET results = ESQL_QUERY(query);
  
  IF ARRAY_LENGTH(results) > 0 THEN
    RETURN {
      "metric": metric_name,
      "average": DOCUMENT_GET(results[0], 'avg_value'),
      "maximum": DOCUMENT_GET(results[0], 'max_value'),
      "minimum": DOCUMENT_GET(results[0], 'min_value')
    };
  ELSE
    RETURN {"error": "No data found for metric: " || metric_name};
  END IF;
END PROCEDURE;

-- -----------------------------------------------------------------------------
-- 5. Order Summary - E-commerce analytics
-- -----------------------------------------------------------------------------
CREATE PROCEDURE order_summary()
BEGIN
  DECLARE orders ARRAY;
  DECLARE by_status ARRAY;
  DECLARE top_products ARRAY;
  
  -- Total orders and revenue
  SET orders = ESQL_QUERY('FROM orders-sample | STATS order_count = COUNT(*), total_revenue = SUM(total)');
  
  -- Orders by status
  SET by_status = ESQL_QUERY('FROM orders-sample | STATS count = COUNT(*) BY status | SORT count DESC');
  
  -- Top selling products
  SET top_products = ESQL_QUERY('FROM orders-sample | STATS sold = COUNT(*) BY product_name | SORT sold DESC | LIMIT 5');
  
  PRINT 'Order Summary Report';
  PRINT '====================';
  PRINT 'Total Orders: ' || DOCUMENT_GET(orders[0], 'order_count');
  PRINT 'Total Revenue: $' || ROUND(DOCUMENT_GET(orders[0], 'total_revenue'), 2);
  PRINT '';
  PRINT 'Orders by Status:';
  
  FOR i IN 1..ARRAY_LENGTH(by_status) LOOP
    PRINT '  ' || DOCUMENT_GET(by_status[i-1], 'status') || ': ' || DOCUMENT_GET(by_status[i-1], 'count');
  END LOOP;
  
  RETURN {
    "total_orders": DOCUMENT_GET(orders[0], 'order_count'),
    "total_revenue": DOCUMENT_GET(orders[0], 'total_revenue'),
    "by_status": by_status,
    "top_products": top_products
  };
END PROCEDURE;

-- -----------------------------------------------------------------------------
-- 6. Security Audit - Analyze security events
-- -----------------------------------------------------------------------------
CREATE PROCEDURE security_audit(severity STRING DEFAULT 'all')
BEGIN
  DECLARE events ARRAY;
  DECLARE by_type ARRAY;
  DECLARE high_severity ARRAY;
  
  IF severity = 'all' THEN
    SET events = ESQL_QUERY('FROM security-events | STATS total = COUNT(*)');
    SET by_type = ESQL_QUERY('FROM security-events | STATS count = COUNT(*) BY event_type | SORT count DESC');
  ELSE
    SET events = ESQL_QUERY('FROM security-events | WHERE severity = "' || severity || '" | STATS total = COUNT(*)');
    SET by_type = ESQL_QUERY('FROM security-events | WHERE severity = "' || severity || '" | STATS count = COUNT(*) BY event_type | SORT count DESC');
  END IF;
  
  -- Always get high severity events for attention
  SET high_severity = ESQL_QUERY('FROM security-events | WHERE severity = "high" OR severity = "critical" | LIMIT 10');
  
  RETURN {
    "total_events": DOCUMENT_GET(events[0], 'total'),
    "events_by_type": by_type,
    "high_severity_events": high_severity
  };
END PROCEDURE;

-- -----------------------------------------------------------------------------
-- 7. Product Search - Full-text search with filters
-- -----------------------------------------------------------------------------
CREATE PROCEDURE search_products(search_term STRING, category STRING DEFAULT '', max_price NUMBER DEFAULT 0)
BEGIN
  DECLARE results ARRAY;
  DECLARE query STRING;
  
  SET query = 'FROM products-sample | WHERE name LIKE "*' || search_term || '*"';
  
  IF category != '' THEN
    SET query = query || ' AND category = "' || category || '"';
  END IF;
  
  IF max_price > 0 THEN
    SET query = query || ' AND price <= ' || max_price;
  END IF;
  
  SET query = query || ' | SORT price ASC | LIMIT 20';
  
  SET results = ESQL_QUERY(query);
  
  PRINT 'Found ' || ARRAY_LENGTH(results) || ' products matching "' || search_term || '"';
  
  RETURN results;
END PROCEDURE;

-- -----------------------------------------------------------------------------
-- 8. Generate Report - Dynamic report generation
-- -----------------------------------------------------------------------------
CREATE PROCEDURE generate_report(report_type STRING)
BEGIN
  DECLARE report DOCUMENT;
  DECLARE generated_at STRING;
  
  SET generated_at = CURRENT_TIMESTAMP();
  
  IF report_type = 'logs' THEN
    SET report = CALL analyze_logs('logs-sample');
  ELSEIF report_type = 'users' THEN
    SET report = CALL get_user_stats();
  ELSEIF report_type = 'orders' THEN
    SET report = CALL order_summary();
  ELSEIF report_type = 'security' THEN
    SET report = CALL security_audit('all');
  ELSE
    RETURN {"error": "Unknown report type. Available: logs, users, orders, security"};
  END IF;
  
  RETURN {
    "report_type": report_type,
    "generated_at": generated_at,
    "data": report
  };
END PROCEDURE;

-- -----------------------------------------------------------------------------
-- 9. System Health Check - Overall system status
-- -----------------------------------------------------------------------------
CREATE PROCEDURE health_check()
BEGIN
  DECLARE log_health DOCUMENT;
  DECLARE metric_health DOCUMENT;
  DECLARE security_health DOCUMENT;
  DECLARE status STRING;
  DECLARE issues ARRAY;
  
  SET issues = [];
  SET status = 'healthy';
  
  -- Check for recent errors
  DECLARE error_results ARRAY;
  SET error_results = ESQL_QUERY('FROM logs-sample | WHERE level = "ERROR" | STATS error_count = COUNT(*)');
  
  IF DOCUMENT_GET(error_results[0], 'error_count') > 10 THEN
    SET status = 'warning';
    SET issues = ARRAY_APPEND(issues, 'High error count in logs');
  END IF;
  
  -- Check for security incidents  
  DECLARE security_results ARRAY;
  SET security_results = ESQL_QUERY('FROM security-events | WHERE severity = "critical" | STATS critical_count = COUNT(*)');
  
  IF DOCUMENT_GET(security_results[0], 'critical_count') > 0 THEN
    SET status = 'critical';
    SET issues = ARRAY_APPEND(issues, 'Critical security events detected');
  END IF;
  
  RETURN {
    "status": status,
    "timestamp": CURRENT_TIMESTAMP(),
    "issues": issues,
    "checks": {
      "error_count": DOCUMENT_GET(error_results[0], 'error_count'),
      "critical_security_events": DOCUMENT_GET(security_results[0], 'critical_count')
    }
  };
END PROCEDURE;

-- -----------------------------------------------------------------------------
-- 10. Demo Workflow - Showcase async capabilities
-- -----------------------------------------------------------------------------
CREATE PROCEDURE demo_workflow()
BEGIN
  PRINT '=== Moltler Demo Workflow ===';
  PRINT '';
  
  -- Step 1: Health check
  PRINT 'Step 1: Running health check...';
  DECLARE health DOCUMENT;
  SET health = CALL health_check();
  PRINT '  Status: ' || DOCUMENT_GET(health, 'status');
  PRINT '';
  
  -- Step 2: Log analysis
  PRINT 'Step 2: Analyzing logs...';
  DECLARE log_result ARRAY;
  SET log_result = CALL analyze_logs('logs-sample');
  PRINT '  Found ' || ARRAY_LENGTH(log_result) || ' top error patterns';
  PRINT '';
  
  -- Step 3: User stats
  PRINT 'Step 3: Getting user statistics...';
  DECLARE user_stats DOCUMENT;
  SET user_stats = CALL get_user_stats();
  PRINT '  Total users: ' || DOCUMENT_GET(user_stats, 'total_users');
  PRINT '';
  
  -- Step 4: Order summary
  PRINT 'Step 4: Generating order summary...';
  DECLARE orders DOCUMENT;
  SET orders = CALL order_summary();
  PRINT '  Total revenue: $' || DOCUMENT_GET(orders, 'total_revenue');
  PRINT '';
  
  PRINT '=== Demo Complete ===';
  PRINT 'All procedures executed successfully!';
  
  RETURN {
    "success": TRUE,
    "health": health,
    "users": user_stats,
    "orders": orders
  };
END PROCEDURE;
