# Security Skills

**Hunt threats faster.** Pre-built skills for SIEM, threat hunting, and risk assessment.

---

## Quick Start

```bash
# 1. Install security skills
./hub/moltler-cli.sh install --all --category security

# 2. Hunt for an IOC
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "RUN SKILL hunt_ioc(ioc => '\''192.168.1.100'\'')"}'
```

---

## Common Scenarios

### "Is this IP malicious?"

```sql
RUN SKILL hunt_ioc(ioc => '192.168.1.100', ioc_type => 'ip');
```

Searches all security indices for any activity involving this IP.

### "Who are our riskiest users?"

```sql
-- Get users with high risk scores
RUN SKILL get_risky_users(threshold => 70);

-- Get specific user's risk
RUN SKILL get_user_risk_score(username => 'jsmith');
```

### "What happened on this host?"

```sql
-- Get host risk score
RUN SKILL get_host_risk_score(hostname => 'workstation-42');

-- Get process events
RUN SKILL get_process_events(hostname => 'workstation-42', limit => 50);

-- Get file events
RUN SKILL get_file_events(hostname => 'workstation-42');

-- Get network connections
RUN SKILL get_network_events(hostname => 'workstation-42');
```

### "Are there suspicious logins?"

```sql
-- Get authentication summary
RUN SKILL get_authentication_summary(time_range => '24h');

-- Search for failed logins
RUN SKILL search_security_events(query => 'failed login');
```

### "What DNS queries are suspicious?"

```sql
RUN SKILL get_dns_queries(
  hostname => 'workstation-42',
  exclude_domains => ['microsoft.com', 'google.com']
);
```

---

## Available Skills

### Threat Hunting

| Skill | Description |
|-------|-------------|
| `hunt_ioc` | Search for IOC (IP, hash, domain) |
| `search_security_events` | Full-text search security data |
| `get_process_events` | Process execution on a host |
| `get_file_events` | File system activity |
| `get_network_events` | Network connections |
| `get_dns_queries` | DNS query analysis |

### Risk Assessment

| Skill | Description |
|-------|-------------|
| `get_risky_users` | Users above risk threshold |
| `get_risky_hosts` | Hosts above risk threshold |
| `get_user_risk_score` | Specific user's risk details |
| `get_host_risk_score` | Specific host's risk details |

### Authentication

| Skill | Description |
|-------|-------------|
| `get_authentication_summary` | Login success/failure overview |
| `suspicious_activity` | Detect anomalous behavior |

### Case Management

| Skill | Description |
|-------|-------------|
| `list_cases` | List open security cases |
| `create_case` | Create a new case |
| `list_detections` | Active detection rules |

---

## Real-World Workflow

**Scenario: Alert fired - potential data exfiltration**

```sql
-- Step 1: What triggered the alert?
RUN SKILL search_security_events(
  query => 'data transfer large file',
  time_range => '1h'
);
-- Result: User jsmith transferred 2GB to external IP

-- Step 2: What's this user's risk score?
RUN SKILL get_user_risk_score(username => 'jsmith');
-- Result: Risk score 85 (high) - multiple anomalies detected

-- Step 3: What else has this user done?
RUN SKILL get_process_events(
  username => 'jsmith',
  time_range => '24h',
  limit => 50
);
-- Result: Ran compression tools, used cloud sync software

-- Step 4: Where did the data go?
RUN SKILL get_network_events(
  username => 'jsmith',
  direction => 'outbound'
);
-- Result: Large transfers to IP 203.0.113.50

-- Step 5: Is this IP known bad?
RUN SKILL hunt_ioc(ioc => '203.0.113.50');
-- Result: No previous activity, but IP is in threat intel feed

-- Step 6: Create a case
RUN SKILL create_case(
  title => 'Potential data exfiltration - jsmith',
  severity => 'high',
  description => 'User transferred 2GB to suspicious external IP'
);
```

**Investigation time: 3 minutes instead of 20.**

---

## Threat Hunt Templates

### Hunt: Lateral Movement

```sql
-- Find RDP/SSH to unusual destinations
RUN SKILL search_security_events(
  query => 'event.action:login AND destination.ip:10.*'
);

-- Find admin tool usage
RUN SKILL get_process_events(
  process_name => 'psexec OR wmic OR powershell'
);
```

### Hunt: Persistence Mechanisms

```sql
-- Scheduled tasks created
RUN SKILL search_security_events(
  query => 'event.action:scheduled_task_created'
);

-- Registry modifications
RUN SKILL search_security_events(
  query => 'registry.path:*\\Run\\*'
);
```

### Hunt: Data Staging

```sql
-- Large file compression
RUN SKILL get_process_events(
  process_name => '7z OR rar OR zip',
  time_range => '7d'
);

-- Unusual archive locations
RUN SKILL get_file_events(
  file_extension => 'zip OR rar OR 7z',
  path_pattern => '*\\temp\\*'
);
```

---

## Combine with AI

Connect to an AI assistant for natural language hunting:

```
User: "Is there any suspicious activity from user jsmith?"

AI: Let me investigate...
    [Runs get_user_risk_score(username => 'jsmith')]
    [Runs get_process_events(username => 'jsmith')]
    [Runs get_network_events(username => 'jsmith')]

AI: "User jsmith has a risk score of 85 (high). I found:
     - 3 failed login attempts from unusual IP
     - PowerShell execution with encoded commands
     - Large outbound transfer to external IP
     
     Recommend creating a case for investigation."
```

See [MCP Integration](../mcp.md) to connect your AI assistant.

---

## Build Custom Skills

Create hunting skills for your environment:

```sql
CREATE SKILL hunt_lateral_movement
  VERSION '1.0.0'
  DESCRIPTION 'Detect potential lateral movement in the network'
  (time_range STRING DEFAULT '24h')
  RETURNS DOCUMENT
BEGIN
  DECLARE rdp_sessions ARRAY;
  DECLARE admin_tools ARRAY;
  DECLARE risk_score INT;

  SET rdp_sessions = ESQL_QUERY('FROM security-* | WHERE event.action == "rdp_connection" | STATS count=COUNT() BY source.ip, destination.ip');
  SET admin_tools = ESQL_QUERY('FROM security-* | WHERE process.name IN ("psexec", "wmic", "winrm") | LIMIT 50');
  
  SET risk_score = CASE 
    WHEN ARRAY_LENGTH(admin_tools) > 10 THEN 90
    WHEN ARRAY_LENGTH(admin_tools) > 5 THEN 70
    ELSE 30
  END;

  RETURN {
    'rdp_sessions': rdp_sessions,
    'admin_tool_usage': admin_tools,
    'risk_score': risk_score,
    'recommendation': CASE WHEN risk_score > 70 THEN 'Investigate immediately' ELSE 'Monitor' END
  };
END SKILL;
```

---

## Next Steps

- [Browse all security skills](../moltlerhub/index.md)
- [Create custom skills](../skills/creating-skills.md)
- [Connect AI assistant](../mcp.md)
