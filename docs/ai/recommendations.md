# AI Recommendations

Get proactive suggestions to improve your Moltler setup.

## Overview

Moltler continuously analyzes your usage and provides recommendations:

```sql
SHOW RECOMMENDATIONS;
```

Output:

```
┌─────────────────────────────────────────────────────────────────────┐
│ Recommendations for your Moltler instance                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│ 🎯 HIGH IMPACT                                                      │
│                                                                     │
│ 1. Create skill for "high error rate" alert                        │
│    You've manually responded to this alert 15 times this month.    │
│    [Generate Skill]                                                 │
│                                                                     │
│ 2. Add caching to check_api_health skill                           │
│    This skill is called 200+ times/hour with same parameters.      │
│    Potential savings: 80% fewer API calls.                         │
│    [Apply Improvement]                                              │
│                                                                     │
│ 📊 OPTIMIZATION                                                     │
│                                                                     │
│ 3. Combine related skills into a pack                              │
│    Skills: check_db, check_cache, check_api are always used        │
│    together. Consider creating a health_checks pack.               │
│    [Create Pack]                                                    │
│                                                                     │
│ 4. Update analyze_logs to v2.0.0                                   │
│    New version has 40% better performance and fixes 3 bugs.        │
│    [View Changelog] [Update]                                        │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Recommendation Types

### Skill Suggestions

Based on your alert patterns:

```sql
SHOW RECOMMENDATIONS
WHERE type = 'skill_suggestion';
```

```
Suggested Skills:

1. Handle "disk-space-low" alerts
   Pattern detected: Manual cleanup 12 times/month
   Suggested actions: Delete old logs, archive data
   
2. Handle "certificate-expiring" alerts
   Pattern detected: Manual renewal every 90 days
   Suggested actions: Auto-renew, notify team
```

### Performance Improvements

```sql
SHOW RECOMMENDATIONS
WHERE type = 'performance';
```

```
Performance Improvements:

1. Add pagination to list_all_issues
   Current: Fetches 10,000+ records at once
   Recommendation: Use cursor-based pagination
   Impact: 5x faster, 90% less memory

2. Cache connector queries
   Current: 500 identical queries/hour to GitHub
   Recommendation: Cache for 5 minutes
   Impact: 95% fewer API calls
```

### Security Recommendations

```sql
SHOW RECOMMENDATIONS
WHERE type = 'security';
```

```
Security Recommendations:

1. Rotate API keys older than 90 days
   Found: 3 keys not rotated since creation
   
2. Add input validation to process_user_input
   Risk: Potential injection vulnerability
   
3. Use secrets manager for database password
   Current: Hardcoded in connector config
```

### Cost Optimization

```sql
SHOW RECOMMENDATIONS
WHERE type = 'cost';
```

```
Cost Optimization:

1. Use faster model for simple decisions
   Current: gpt-4 for all agent decisions
   Recommendation: gpt-3.5-turbo for routine checks
   Savings: ~$30/month

2. Reduce polling frequency
   Current: health check every 1 minute
   Recommendation: 5 minutes (based on change frequency)
   Savings: 80% fewer executions
```

---

## Applying Recommendations

### Quick Apply

```sql
APPLY RECOMMENDATION 1;
```

### Preview First

```sql
SHOW RECOMMENDATION 1 DETAILS;
APPLY RECOMMENDATION 1 PREVIEW;
APPLY RECOMMENDATION 1;
```

### Apply All High Impact

```sql
APPLY RECOMMENDATIONS
WHERE impact = 'high'
PREVIEW;
```

---

## Recommendation Settings

### Configure Sources

```sql
SET RECOMMENDATIONS CONFIG {
    "analyze_alerts": true,
    "analyze_executions": true,
    "analyze_costs": true,
    "analyze_security": true,
    "min_pattern_occurrences": 5
};
```

### Set Notification Preferences

```sql
SET RECOMMENDATIONS NOTIFY {
    "high_impact": "slack:#moltler-recs",
    "security": "email:security@company.com",
    "weekly_digest": "email:team@company.com"
};
```

### Dismiss Recommendations

```sql
DISMISS RECOMMENDATION 3;
DISMISS RECOMMENDATION 3 REASON 'Not applicable to our use case';
```

---

## Proactive Suggestions

### Ask for Suggestions

```sql
SUGGEST SKILL FOR 'reducing deployment failures';
```

Output:

```
Based on your deployment patterns, I suggest:

1. pre_deploy_validation skill
   - Check service dependencies
   - Verify configuration
   - Run smoke tests
   
2. deployment_monitor skill
   - Watch error rates post-deploy
   - Auto-rollback if issues detected
   
3. deploy_notification skill
   - Notify team of deployment
   - Include changelog summary

Would you like me to generate any of these? [1/2/3/all]
```

### Suggest Agent

```sql
SUGGEST AGENT FOR 'incident response';
```

### Suggest Connectors

```sql
SUGGEST CONNECTORS FOR 'DevOps workflow';
```

---

## What's Next?

<div class="grid cards" markdown>

-   :material-book:{ .lg .middle } __Examples__

    See real-world implementations.

    [:octicons-arrow-right-24: Examples](../examples/observability-skills.md)

-   :material-tools:{ .lg .middle } __Tools__

    CLI and SDK reference.

    [:octicons-arrow-right-24: CLI](../tools/cli.md)

</div>
