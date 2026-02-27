# "I Know Kung Fu" Demo

> The 30-second demo that shows instant Elasticsearch mastery in Cursor.

## The Pitch

**Before:** "I don't know how to query Elasticsearch, write ES|QL, or build observability workflows."

**After (30 seconds later):** "I know kung fu."

## Setup (One-Time, 10 seconds)

### In Cursor

1. Press `Cmd+Shift+J` (Mac) or `Ctrl+Shift+J` (Windows/Linux)
2. Click **Rules** → **Add Rule** → **Remote Rule (GitHub)**
3. Paste: `https://github.com/bahaaldine/moltler`
4. Done.

That's it. No npm install. No configuration. No API keys needed for the demo.

## The Demo Script (20 seconds)

### Scene 1: The Setup

Open Cursor chat and type:

```
What can you do with Elasticsearch?
```

**Agent responds** with the full capability matrix - 320+ functions across observability, security, search, ML, and more.

### Scene 2: The "I Know Kung Fu" Moment

Type:

```
/kung-fu
```

**Agent shows** the instant mastery message with all capabilities unlocked.

### Scene 3: Prove It Works

Type any of these:

```
Check my cluster health
```
→ Agent runs `ES_CLUSTER_HEALTH()` and explains the results

```
Find errors in the last hour
```
→ Agent writes and executes ES|QL query

```
What services are throwing the most exceptions?
```
→ Agent builds an aggregation query and analyzes results

### Scene 4: Advanced Capabilities

```
If CPU goes above 80%, send a Slack alert
```
→ Agent creates an alert rule with Slack action

```
Hunt for this IP across all security indices: 10.0.0.50
```
→ Agent runs threat hunting query

```
Summarize today's incidents using AI
```
→ Agent uses LLM to analyze and summarize

## Live Demo Checklist

- [ ] Elasticsearch running (can use `./scripts/quick-start.sh`)
- [ ] Cursor with Moltler rules installed
- [ ] Sample data loaded (quick-start.sh does this automatically)

### Quick Start for Demo

```bash
# Clone and start everything
git clone https://github.com/bahaaldine/moltler.git
cd moltler
./scripts/quick-start.sh

# In another terminal, open in Cursor
cursor .
```

## Key Talking Points

### 1. Zero Learning Curve
"You didn't read any documentation. You didn't learn ES|QL syntax. You just asked."

### 2. 320+ Capabilities Instantly
"One GitHub URL gave your AI agent 320 Elasticsearch functions, 260 pre-built skills, and 10 workflow categories."

### 3. Production-Ready
"These aren't toy examples. These are the same functions used in production observability and security workflows."

### 4. Open Standard
"This uses the open Agent Skills standard. Works in Cursor today, but the skills are portable to any agent that supports the standard."

## Demo Variations

### Security Team Demo
```
Show me risky users
Hunt for IOC: evil.com
List failed authentications in the last 24 hours
Create a detection rule for brute force attempts
```

### SRE/DevOps Demo
```
Is anything unhealthy in my cluster?
Show me slow requests in the last hour
What's causing the spike in error rate?
Scale down the non-critical deployments
```

### Data Engineering Demo
```
Create a new index with this mapping: {...}
Reindex from old-index to new-index
Set up ILM policy for 30-day retention
Show me the ingest pipeline status
```

## Video Script (60 seconds)

```
[0:00] "Watch this."

[0:03] *Opens Cursor settings, adds GitHub URL*

[0:08] "I just gave my AI agent 320 Elasticsearch capabilities."

[0:12] *Types: "Check my cluster health"*

[0:15] *Agent responds with cluster status*

[0:20] "No documentation. No training. It just works."

[0:25] *Types: "Find errors in the last hour"*

[0:28] *Agent writes ES|QL, executes, shows results*

[0:35] "ES|QL queries? Done."

[0:38] *Types: "If error rate exceeds 100/minute, page the on-call"*

[0:42] *Agent creates alert with PagerDuty action*

[0:48] "Alerts? Done."

[0:50] *Types: "Summarize today's incidents"*

[0:53] *Agent uses LLM to analyze and summarize*

[0:58] "I know kung fu."

[1:00] *End card: github.com/bahaaldine/moltler*
```

## After the Demo

Direct them to:
- **Try it:** `https://github.com/bahaaldine/moltler`
- **Browse skills:** `https://hub.moltler.dev`
- **Documentation:** `https://bahaaldine.github.io/moltler`

## FAQ During Demo

**Q: Does this work with my Elasticsearch cluster?**
A: Yes, set `ES_URL`, `ES_USERNAME`, `ES_PASSWORD` in your environment.

**Q: Does this work with Elastic Cloud?**
A: Yes, use your Cloud ID and API key.

**Q: What about security?**
A: The agent uses your credentials. It has the same permissions you do.

**Q: Can I add my own skills?**
A: Yes! Create a `SKILL.md` in `.agents/skills/your-skill/` and it's instantly available.
