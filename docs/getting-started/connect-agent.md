# Connect Your AI Agent

The final step: connect your favorite AI assistant to Moltler and start querying your Elasticsearch data with natural language.

---

## Supported Agents

| Agent | Status | Config Location |
|-------|--------|-----------------|
| **Cursor** | ✅ Ready | `.cursor/mcp.json` |
| **Claude Desktop** | ✅ Ready | `claude_desktop_config.json` |
| **Cline** | ✅ Ready | VS Code settings |
| **Custom Agents** | ✅ Ready | Any MCP-compatible client |

---

## Cursor IDE

**Step 1:** Create or edit `.cursor/mcp.json` in your project:

```json
{
  "mcpServers": {
    "moltler": {
      "url": "http://localhost:9200/_escript/mcp",
      "headers": {
        "Authorization": "Basic ZWxhc3RpYy1hZG1pbjplbGFzdGljLXBhc3N3b3Jk"
      }
    }
  }
}
```

**Step 2:** Restart Cursor

**Step 3:** Start asking questions:

> "Find all ERROR logs from the last hour"

> "Show me the slowest API transactions"

> "Hunt for IP 192.168.1.100 in security events"

> "What's the error rate trend for the payment service?"

---

## Claude Desktop

**Step 1:** Find your config file:

- **macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows:** `%APPDATA%\Claude\claude_desktop_config.json`

**Step 2:** Add the Moltler server:

```json
{
  "mcpServers": {
    "moltler": {
      "command": "curl",
      "args": [
        "-X", "POST",
        "-H", "Content-Type: application/json",
        "-H", "Authorization: Basic ZWxhc3RpYy1hZG1pbjplbGFzdGljLXBhc3N3b3Jk",
        "http://localhost:9200/_escript/mcp"
      ]
    }
  }
}
```

**Step 3:** Restart Claude Desktop

**Step 4:** Ask Claude to help you:

> "I need to investigate a production incident. Can you find recent errors and correlate them with slow transactions?"

---

## Cline (VS Code Extension)

**Step 1:** Open VS Code Settings (JSON)

**Step 2:** Add MCP configuration:

```json
{
  "cline.mcpServers": {
    "moltler": {
      "url": "http://localhost:9200/_escript/mcp",
      "headers": {
        "Authorization": "Basic ZWxhc3RpYy1hZG1pbjplbGFzdGljLXBhc3N3b3Jk"
      }
    }
  }
}
```

**Step 3:** Restart VS Code

---

## For Production Clusters

Replace the default credentials with your own:

```bash
# Generate base64 credentials
echo -n "your-user:your-password" | base64
# Output: eW91ci11c2VyOnlvdXItcGFzc3dvcmQ=
```

Then use in your config:

```json
{
  "headers": {
    "Authorization": "Basic eW91ci11c2VyOnlvdXItcGFzc3dvcmQ="
  }
}
```

For HTTPS clusters:

```json
{
  "url": "https://your-cluster:9200/_escript/mcp"
}
```

---

## What Can You Ask?

Once connected, your AI agent has access to all installed skills. Here are examples by solution:

### Observability

| Ask this... | Agent uses... |
|-------------|---------------|
| "Find errors in production" | `get_recent_errors` |
| "Show slow transactions" | `get_slow_transactions` |
| "Correlate logs for trace abc123" | `correlate_logs` |
| "What's the error rate trend?" | `count_logs_by_level` |

### Security

| Ask this... | Agent uses... |
|-------------|---------------|
| "Hunt for IP 10.0.0.1" | `hunt_ioc` |
| "Find risky users" | `get_risky_users` |
| "Show failed logins" | `get_failed_logins` |
| "Search for lateral movement" | `search_security_events` |

### Search

| Ask this... | Agent uses... |
|-------------|---------------|
| "Find documents about pricing" | `semantic_search` |
| "Top categories in our catalog" | `top_values` |
| "Search with typo tolerance" | `fuzzy_search` |

---

## The Magic Moment

After connecting, try this conversation:

> **You:** "I got paged for a production incident. The payment service is having issues. Can you help me investigate?"
>
> **Agent:** *Uses `get_recent_errors` to find errors in payment service*
>
> "I found 47 errors in the payment service in the last hour. The most common error is 'Connection timeout to payment gateway'. Let me check if there are any slow transactions..."
>
> *Uses `get_slow_transactions` to find latency issues*
>
> "I see the payment gateway calls are averaging 12 seconds, which is 10x higher than normal. This correlates with the timeout errors. The issue started around 9:45 AM..."

**This is the power of Moltler:** Your AI assistant can investigate your Elasticsearch data just like a senior engineer would.

---

## Troubleshooting

### Agent can't connect

```bash
# Test the MCP endpoint directly
curl -u elastic:password http://localhost:9200/_escript/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc": "2.0", "method": "tools/list", "id": 1}'
```

### Skills not showing up

```bash
# Verify skills are installed
curl -u elastic:password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "SHOW SKILLS"}'
```

### Authentication errors

- Verify your base64 credentials are correct
- Check if your ES cluster requires API keys instead of basic auth

---

## Next Steps

| I want to... | Go here |
|--------------|---------|
| Browse all skills | [MoltlerHub](https://hub.moltler.dev) |
| Build my own skills | [Creating Skills](../skills/creating-skills.md) |
| Understand MCP | [MCP Integration](../mcp.md) |
