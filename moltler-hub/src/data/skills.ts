// Auto-generated - do not edit
import type { Skill } from '@/lib/skills';

export const SKILLS: Skill[] = [
  {
    "name": "ab-chat",
    "displayName": "Chat",
    "description": "Send a chat message to an Agent Builder agent",
    "version": "1.0.0",
    "author": "elastic",
    "category": "agent-builder",
    "tags": [
      "agent-builder"
    ],
    "parameters": [
      {
        "name": "agent_id",
        "type": "STRING",
        "description": "Agent ID to chat with",
        "required": true
      },
      {
        "name": "message",
        "type": "STRING",
        "description": "Message to send to the agent",
        "required": true
      },
      {
        "name": "conversation_id",
        "type": "STRING",
        "description": "Existing conversation ID to continue (optional)",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/agent-builder/chat"
  },
  {
    "name": "ab-create-agent",
    "displayName": "Create Agent",
    "description": "Create a new Agent Builder agent in Kibana",
    "version": "1.0.0",
    "author": "elastic",
    "category": "agent-builder",
    "tags": [
      "agent-builder"
    ],
    "parameters": [
      {
        "name": "name",
        "type": "STRING",
        "description": "Agent name",
        "required": true
      },
      {
        "name": "description",
        "type": "STRING",
        "description": "Agent description",
        "required": true
      },
      {
        "name": "instructions",
        "type": "STRING",
        "description": "System instructions for the agent",
        "required": true
      },
      {
        "name": "model",
        "type": "STRING",
        "description": "LLM model to use",
        "required": true
      },
      {
        "name": "tools",
        "type": "ARRAY",
        "description": "Array of tool IDs to enable",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/agent-builder/create-agent"
  },
  {
    "name": "ab-create-tool",
    "displayName": "Create Tool",
    "description": "Create a new Agent Builder tool",
    "version": "1.0.0",
    "author": "elastic",
    "category": "agent-builder",
    "tags": [
      "agent-builder"
    ],
    "parameters": [
      {
        "name": "name",
        "type": "STRING",
        "description": "Tool name",
        "required": true
      },
      {
        "name": "description",
        "type": "STRING",
        "description": "Tool description for AI understanding",
        "required": true
      },
      {
        "name": "type",
        "type": "STRING",
        "description": "Tool type: elasticsearch, http, mcp",
        "required": true
      },
      {
        "name": "configuration",
        "type": "STRING",
        "description": "JSON configuration for the tool",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/agent-builder/create-tool"
  },
  {
    "name": "ab-delete-agent",
    "displayName": "Delete Agent",
    "description": "Delete an Agent Builder agent",
    "version": "1.0.0",
    "author": "elastic",
    "category": "agent-builder",
    "tags": [
      "agent-builder"
    ],
    "parameters": [
      {
        "name": "agent_id",
        "type": "STRING",
        "description": "Agent ID to delete",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/agent-builder/delete-agent"
  },
  {
    "name": "ab-execute-tool",
    "displayName": "Execute Tool",
    "description": "Execute an Agent Builder tool directly",
    "version": "1.0.0",
    "author": "elastic",
    "category": "agent-builder",
    "tags": [
      "agent-builder"
    ],
    "parameters": [
      {
        "name": "tool_id",
        "type": "STRING",
        "description": "Tool ID to execute",
        "required": true
      },
      {
        "name": "arguments",
        "type": "STRING",
        "description": "JSON arguments to pass to the tool",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/agent-builder/execute-tool"
  },
  {
    "name": "ab-get-agent",
    "displayName": "Get Agent",
    "description": "Get details of a specific Agent Builder agent",
    "version": "1.0.0",
    "author": "elastic",
    "category": "agent-builder",
    "tags": [
      "agent-builder"
    ],
    "parameters": [
      {
        "name": "agent_id",
        "type": "STRING",
        "description": "Agent ID to retrieve",
        "required": true
      }
    ],
    "returns": "full",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/agent-builder/get-agent"
  },
  {
    "name": "ab-get-agent-card",
    "displayName": "Get Agent Card",
    "description": "Get A2A agent card for interoperability",
    "version": "1.0.0",
    "author": "elastic",
    "category": "agent-builder",
    "tags": [
      "agent-builder"
    ],
    "parameters": [
      {
        "name": "LD",
        "type": "DOCUMENT",
        "description": "",
        "required": true
      },
      {
        "name": "agent_id",
        "type": "STRING",
        "description": "Agent ID to get A2A card for",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/agent-builder/get-agent-card"
  },
  {
    "name": "ab-get-conversation",
    "displayName": "Get Conversation",
    "description": "Get full conversation history with an Agent Builder agent",
    "version": "1.0.0",
    "author": "elastic",
    "category": "agent-builder",
    "tags": [
      "agent-builder"
    ],
    "parameters": [
      {
        "name": "conversation_id",
        "type": "STRING",
        "description": "Conversation ID to retrieve",
        "required": true
      }
    ],
    "returns": "all",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/agent-builder/get-conversation"
  },
  {
    "name": "ab-list-agents",
    "displayName": "List Agents",
    "description": "List all Agent Builder agents configured in Kibana",
    "version": "1.0.0",
    "author": "elastic",
    "category": "agent-builder",
    "tags": [
      "agent-builder"
    ],
    "parameters": [
      {
        "name": "space_id",
        "type": "STRING",
        "description": "Kibana space ID",
        "required": true
      }
    ],
    "returns": "agent",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/agent-builder/list-agents"
  },
  {
    "name": "ab-list-conversations",
    "displayName": "List Conversations",
    "description": "List all Agent Builder conversations",
    "version": "1.0.0",
    "author": "elastic",
    "category": "agent-builder",
    "tags": [
      "agent-builder"
    ],
    "parameters": [],
    "returns": "conversation",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/agent-builder/list-conversations"
  },
  {
    "name": "ab-list-tools",
    "displayName": "List Tools",
    "description": "List all Agent Builder tools",
    "version": "1.0.0",
    "author": "elastic",
    "category": "agent-builder",
    "tags": [
      "agent-builder"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/agent-builder/list-tools"
  },
  {
    "name": "ab-mcp-call",
    "displayName": "Mcp Call",
    "description": "Call Agent Builder MCP server endpoint",
    "version": "1.0.0",
    "author": "elastic",
    "category": "agent-builder",
    "tags": [
      "agent-builder"
    ],
    "parameters": [
      {
        "name": "method",
        "type": "STRING",
        "description": "MCP method: tools/list, tools/call, etc.",
        "required": true
      },
      {
        "name": "params",
        "type": "STRING",
        "description": "JSON parameters for the MCP call",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/agent-builder/mcp-call"
  },
  {
    "name": "ab-send-a2a-task",
    "displayName": "Send A2a Task",
    "description": "Send A2A task to an Agent Builder agent",
    "version": "1.0.0",
    "author": "elastic",
    "category": "agent-builder",
    "tags": [
      "agent-builder"
    ],
    "parameters": [
      {
        "name": "agent_id",
        "type": "STRING",
        "description": "Target agent ID",
        "required": true
      },
      {
        "name": "task",
        "type": "STRING",
        "description": "Task description to send",
        "required": true
      },
      {
        "name": "context",
        "type": "STRING",
        "description": "Additional context as JSON",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/agent-builder/send-a2a-task"
  },
  {
    "name": "acknowledge-alert",
    "displayName": "Acknowledge Alert",
    "description": "Acknowledge an active alert",
    "version": "1.0.0",
    "author": "elastic",
    "category": "alerting",
    "tags": [
      "alerting"
    ],
    "parameters": [
      {
        "name": "alert_id",
        "type": "STRING",
        "description": "Alert ID to acknowledge",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/alerting/acknowledge-alert"
  },
  {
    "name": "create-threshold-rule",
    "displayName": "Create Threshold Rule",
    "description": "Create a threshold-based alert rule",
    "version": "1.0.0",
    "author": "elastic",
    "category": "alerting",
    "tags": [
      "alerting"
    ],
    "parameters": [
      {
        "name": "name",
        "type": "STRING",
        "description": "Rule name",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/alerting/create-threshold-rule"
  },
  {
    "name": "get-active-alerts",
    "displayName": "Get Active Alerts",
    "description": "Get all currently active/firing alerts",
    "version": "1.0.0",
    "author": "elastic",
    "category": "alerting",
    "tags": [
      "alerting"
    ],
    "parameters": [
      {
        "name": "severity",
        "type": "STRING",
        "description": "Filter by severity: critical, high, medium, low",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/alerting/get-active-alerts"
  },
  {
    "name": "get-alert-history",
    "displayName": "Get Alert History",
    "description": "Get historical alerts for analysis",
    "version": "1.0.0",
    "author": "elastic",
    "category": "alerting",
    "tags": [
      "alerting"
    ],
    "parameters": [
      {
        "name": "rule_id",
        "type": "STRING",
        "description": "Filter by rule ID",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/alerting/get-alert-history"
  },
  {
    "name": "list-alert-rules",
    "displayName": "List Alert Rules",
    "description": "List all alerting rules with their status",
    "version": "1.0.0",
    "author": "elastic",
    "category": "alerting",
    "tags": [
      "alerting"
    ],
    "parameters": [
      {
        "name": "enabled",
        "type": "STRING",
        "description": "Filter: true, false, or null for all",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/alerting/list-alert-rules"
  },
  {
    "name": "list-connectors",
    "displayName": "List Connectors",
    "description": "List all configured alert connectors (Slack, PagerDuty, etc.)",
    "version": "1.0.0",
    "author": "elastic",
    "category": "alerting",
    "tags": [
      "alerting"
    ],
    "parameters": [
      {
        "name": "type",
        "type": "STRING",
        "description": "Filter by connector type",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/alerting/list-connectors"
  },
  {
    "name": "mute-alert",
    "displayName": "Mute Alert",
    "description": "Mute an alert temporarily",
    "version": "1.0.0",
    "author": "elastic",
    "category": "alerting",
    "tags": [
      "alerting"
    ],
    "parameters": [
      {
        "name": "alert_id",
        "type": "STRING",
        "description": "Alert ID to mute",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/alerting/mute-alert"
  },
  {
    "name": "test-connector",
    "displayName": "Test Connector",
    "description": "Test an alerting connector",
    "version": "1.0.0",
    "author": "elastic",
    "category": "alerting",
    "tags": [
      "alerting"
    ],
    "parameters": [
      {
        "name": "connector_id",
        "type": "STRING",
        "description": "Connector ID to test",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/alerting/test-connector"
  },
  {
    "name": "analyze-database-queries",
    "displayName": "Analyze Database Queries",
    "description": "Analyze slow database queries",
    "version": "1.0.0",
    "author": "elastic",
    "category": "apm",
    "tags": [
      "apm"
    ],
    "parameters": [
      {
        "name": "threshold_ms",
        "type": "INT",
        "description": "Minimum query duration in ms",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/apm/analyze-database-queries"
  },
  {
    "name": "get-error-groups",
    "displayName": "Get Error Groups",
    "description": "Get errors grouped by type/message for a service",
    "version": "1.0.0",
    "author": "elastic",
    "category": "apm",
    "tags": [
      "apm"
    ],
    "parameters": [
      {
        "name": "service",
        "type": "STRING",
        "description": "Service name to analyze",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/apm/get-error-groups"
  },
  {
    "name": "get-failed-transactions",
    "displayName": "Get Failed Transactions",
    "description": "Get failed/errored transactions",
    "version": "1.0.0",
    "author": "elastic",
    "category": "apm",
    "tags": [
      "apm"
    ],
    "parameters": [
      {
        "name": "service",
        "type": "STRING",
        "description": "Service name",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/apm/get-failed-transactions"
  },
  {
    "name": "get-latency-percentiles",
    "displayName": "Get Latency Percentiles",
    "description": "Get latency percentiles (p50, p95, p99) for a service",
    "version": "1.0.0",
    "author": "elastic",
    "category": "apm",
    "tags": [
      "apm"
    ],
    "parameters": [
      {
        "name": "service",
        "type": "STRING",
        "description": "Service name to analyze",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/apm/get-latency-percentiles"
  },
  {
    "name": "get-service-dependencies",
    "displayName": "Get Service Dependencies",
    "description": "Get upstream and downstream service dependencies",
    "version": "1.0.0",
    "author": "elastic",
    "category": "apm",
    "tags": [
      "apm"
    ],
    "parameters": [
      {
        "name": "service",
        "type": "STRING",
        "description": "Service name to analyze",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/apm/get-service-dependencies"
  },
  {
    "name": "get-service-health",
    "displayName": "Get Service Health",
    "description": "Get health metrics for a specific service including error rate and latency",
    "version": "1.0.0",
    "author": "elastic",
    "category": "apm",
    "tags": [
      "apm"
    ],
    "parameters": [
      {
        "name": "service",
        "type": "STRING",
        "description": "Service name to check",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/apm/get-service-health"
  },
  {
    "name": "get-service-map",
    "displayName": "Get Service Map",
    "description": "Get service dependency map showing all connections",
    "version": "1.0.0",
    "author": "elastic",
    "category": "apm",
    "tags": [
      "apm"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/apm/get-service-map"
  },
  {
    "name": "get-slow-transactions",
    "displayName": "Get Slow Transactions",
    "description": "Find the slowest transactions for a service or across all services",
    "version": "1.0.0",
    "author": "elastic",
    "category": "apm",
    "tags": [
      "apm"
    ],
    "parameters": [
      {
        "name": "service",
        "type": "STRING",
        "description": "Service name (optional)",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/apm/get-slow-transactions"
  },
  {
    "name": "get-throughput",
    "displayName": "Get Throughput",
    "description": "Get request throughput for a service over time",
    "version": "1.0.0",
    "author": "elastic",
    "category": "apm",
    "tags": [
      "apm"
    ],
    "parameters": [
      {
        "name": "service",
        "type": "STRING",
        "description": "Service name",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/apm/get-throughput"
  },
  {
    "name": "get-trace",
    "displayName": "Get Trace",
    "description": "Get full distributed trace by trace ID",
    "version": "1.0.0",
    "author": "elastic",
    "category": "apm",
    "tags": [
      "apm"
    ],
    "parameters": [
      {
        "name": "trace_id",
        "type": "STRING",
        "description": "Trace ID to retrieve",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/apm/get-trace"
  },
  {
    "name": "list-services",
    "displayName": "List Services",
    "description": "List all APM-monitored services with their health status",
    "version": "1.0.0",
    "author": "elastic",
    "category": "apm",
    "tags": [
      "apm"
    ],
    "parameters": [
      {
        "name": "environment",
        "type": "STRING",
        "description": "Filter by environment",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/apm/list-services"
  },
  {
    "name": "analyze-my-cluster",
    "displayName": "Analyze My Cluster",
    "description": "Analyze your Elasticsearch cluster and get actionable insights",
    "version": "1.0.0",
    "author": "elastic",
    "category": "cluster",
    "tags": [
      "cluster"
    ],
    "parameters": [],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/cluster/analyze-my-cluster"
  },
  {
    "name": "explain-allocation",
    "displayName": "Explain Allocation",
    "description": "Explain why a shard is assigned or unassigned",
    "version": "1.0.0",
    "author": "elastic",
    "category": "cluster",
    "tags": [
      "cluster"
    ],
    "parameters": [
      {
        "name": "index",
        "type": "STRING",
        "description": "Index name",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/cluster/explain-allocation"
  },
  {
    "name": "get-cluster-health",
    "displayName": "Get Cluster Health",
    "description": "Get Elasticsearch cluster health status and statistics",
    "version": "1.0.0",
    "author": "elastic",
    "category": "cluster",
    "tags": [
      "cluster"
    ],
    "parameters": [],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/cluster/get-cluster-health"
  },
  {
    "name": "get-hot-threads",
    "displayName": "Get Hot Threads",
    "description": "Get hot threads from nodes for debugging",
    "version": "1.0.0",
    "author": "elastic",
    "category": "cluster",
    "tags": [
      "cluster"
    ],
    "parameters": [
      {
        "name": "node",
        "type": "STRING",
        "description": "Node name or _all",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/cluster/get-hot-threads"
  },
  {
    "name": "get-node-stats",
    "displayName": "Get Node Stats",
    "description": "Get detailed statistics for a specific node",
    "version": "1.0.0",
    "author": "elastic",
    "category": "cluster",
    "tags": [
      "cluster"
    ],
    "parameters": [
      {
        "name": "node_name",
        "type": "STRING",
        "description": "Node name to get stats for",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/cluster/get-node-stats"
  },
  {
    "name": "get-pending-tasks",
    "displayName": "Get Pending Tasks",
    "description": "Get pending cluster tasks",
    "version": "1.0.0",
    "author": "elastic",
    "category": "cluster",
    "tags": [
      "cluster"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/cluster/get-pending-tasks"
  },
  {
    "name": "get-shard-allocation",
    "displayName": "Get Shard Allocation",
    "description": "Get shard allocation across nodes",
    "version": "1.0.0",
    "author": "elastic",
    "category": "cluster",
    "tags": [
      "cluster"
    ],
    "parameters": [
      {
        "name": "index",
        "type": "STRING",
        "description": "Index to check allocation for",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/cluster/get-shard-allocation"
  },
  {
    "name": "get-unassigned-shards",
    "displayName": "Get Unassigned Shards",
    "description": "Get list of unassigned shards and reasons",
    "version": "1.0.0",
    "author": "elastic",
    "category": "cluster",
    "tags": [
      "cluster"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/cluster/get-unassigned-shards"
  },
  {
    "name": "list-nodes",
    "displayName": "List Nodes",
    "description": "List all nodes in the Elasticsearch cluster",
    "version": "1.0.0",
    "author": "elastic",
    "category": "cluster",
    "tags": [
      "cluster"
    ],
    "parameters": [
      {
        "name": "role",
        "type": "STRING",
        "description": "Filter by role: master, data, ingest",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/cluster/list-nodes"
  },
  {
    "name": "list-running-tasks",
    "displayName": "List Running Tasks",
    "description": "List currently running cluster tasks",
    "version": "1.0.0",
    "author": "elastic",
    "category": "cluster",
    "tags": [
      "cluster"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/cluster/list-running-tasks"
  },
  {
    "name": "list-snapshots",
    "displayName": "List Snapshots",
    "description": "List snapshots in a repository",
    "version": "1.0.0",
    "author": "elastic",
    "category": "cluster",
    "tags": [
      "cluster"
    ],
    "parameters": [
      {
        "name": "repository",
        "type": "STRING",
        "description": "Repository name",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/cluster/list-snapshots"
  },
  {
    "name": "get-no-results-queries",
    "displayName": "Get No Results Queries",
    "description": "Get queries that returned no results",
    "version": "1.0.0",
    "author": "elastic",
    "category": "enterprise-search",
    "tags": [
      "enterprise-search"
    ],
    "parameters": [
      {
        "name": "app_name",
        "type": "STRING",
        "description": "Application name",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/enterprise-search/get-no-results-queries"
  },
  {
    "name": "get-search-analytics",
    "displayName": "Get Search Analytics",
    "description": "Get search analytics for an application",
    "version": "1.0.0",
    "author": "elastic",
    "category": "enterprise-search",
    "tags": [
      "enterprise-search"
    ],
    "parameters": [
      {
        "name": "app_name",
        "type": "STRING",
        "description": "Application name",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/enterprise-search/get-search-analytics"
  },
  {
    "name": "get-top-queries",
    "displayName": "Get Top Queries",
    "description": "Get most popular search queries",
    "version": "1.0.0",
    "author": "elastic",
    "category": "enterprise-search",
    "tags": [
      "enterprise-search"
    ],
    "parameters": [
      {
        "name": "app_name",
        "type": "STRING",
        "description": "Application name",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/enterprise-search/get-top-queries"
  },
  {
    "name": "list-search-apps",
    "displayName": "List Search Apps",
    "description": "List all Enterprise Search applications",
    "version": "1.0.0",
    "author": "elastic",
    "category": "enterprise-search",
    "tags": [
      "enterprise-search"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/enterprise-search/list-search-apps"
  },
  {
    "name": "create_index_cmd",
    "displayName": "Create Index",
    "description": "Create an index with mappings using the first-class CREATE INDEX command",
    "version": "1.0.0",
    "author": "moltler",
    "category": "first-class-commands",
    "tags": [
      "elasticsearch"
    ],
    "parameters": [
      {
        "name": "index_name",
        "type": "STRING",
        "description": "Name for the new index",
        "required": true
      },
      {
        "name": "mappings",
        "type": "DOCUMENT",
        "description": "Index mappings definition",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/first-class-commands/create-index"
  },
  {
    "name": "delete_document_cmd",
    "displayName": "Delete Document",
    "description": "Delete a document by ID using the first-class DELETE command",
    "version": "1.0.0",
    "author": "moltler",
    "category": "first-class-commands",
    "tags": [
      "elasticsearch"
    ],
    "parameters": [
      {
        "name": "index_name",
        "type": "STRING",
        "description": "Index containing the document",
        "required": true
      },
      {
        "name": "doc_id",
        "type": "STRING",
        "description": "Document ID to delete",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/first-class-commands/delete-document"
  },
  {
    "name": "index_document_cmd",
    "displayName": "Index Document",
    "description": "Index a document using the first-class INDEX command",
    "version": "1.0.0",
    "author": "moltler",
    "category": "first-class-commands",
    "tags": [
      "elasticsearch"
    ],
    "parameters": [
      {
        "name": "index_name",
        "type": "STRING",
        "description": "Target index name",
        "required": true
      },
      {
        "name": "document",
        "type": "DOCUMENT",
        "description": "Document to index",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/first-class-commands/index-document"
  },
  {
    "name": "refresh_index_cmd",
    "displayName": "Refresh Index",
    "description": "Refresh an index using the first-class REFRESH command",
    "version": "1.0.0",
    "author": "moltler",
    "category": "first-class-commands",
    "tags": [
      "elasticsearch"
    ],
    "parameters": [
      {
        "name": "index_name",
        "type": "STRING",
        "description": "Index name or pattern to refresh",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/first-class-commands/refresh-index"
  },
  {
    "name": "search_documents_cmd",
    "displayName": "Search Documents",
    "description": "Search documents using the first-class SEARCH command",
    "version": "1.0.0",
    "author": "moltler",
    "category": "first-class-commands",
    "tags": [
      "elasticsearch"
    ],
    "parameters": [
      {
        "name": "index_name",
        "type": "STRING",
        "description": "Index pattern to search",
        "required": true
      },
      {
        "name": "query",
        "type": "DOCUMENT",
        "description": "Elasticsearch Query DSL document",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/first-class-commands/search-documents"
  },
  {
    "name": "get-agent-logs",
    "displayName": "Get Agent Logs",
    "description": "Get logs from a specific agent",
    "version": "1.0.0",
    "author": "elastic",
    "category": "fleet",
    "tags": [
      "fleet"
    ],
    "parameters": [
      {
        "name": "agent_id",
        "type": "STRING",
        "description": "Agent ID",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/fleet/get-agent-logs"
  },
  {
    "name": "get-agent-status",
    "displayName": "Get Agent Status",
    "description": "Get status and health of a specific agent",
    "version": "1.0.0",
    "author": "elastic",
    "category": "fleet",
    "tags": [
      "fleet"
    ],
    "parameters": [
      {
        "name": "agent_id",
        "type": "STRING",
        "description": "Agent ID to check",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/fleet/get-agent-status"
  },
  {
    "name": "get-enrollment-tokens",
    "displayName": "Get Enrollment Tokens",
    "description": "List enrollment tokens for agent onboarding",
    "version": "1.0.0",
    "author": "elastic",
    "category": "fleet",
    "tags": [
      "fleet"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/fleet/get-enrollment-tokens"
  },
  {
    "name": "list-agent-policies",
    "displayName": "List Agent Policies",
    "description": "List all agent policies",
    "version": "1.0.0",
    "author": "elastic",
    "category": "fleet",
    "tags": [
      "fleet"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/fleet/list-agent-policies"
  },
  {
    "name": "list-agents",
    "displayName": "List Agents",
    "description": "List all Elastic Agents",
    "version": "1.0.0",
    "author": "elastic",
    "category": "fleet",
    "tags": [
      "fleet"
    ],
    "parameters": [
      {
        "name": "status",
        "type": "STRING",
        "description": "Filter by status: online, offline, updating",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/fleet/list-agents"
  },
  {
    "name": "list-integrations",
    "displayName": "List Integrations",
    "description": "List available Fleet integrations",
    "version": "1.0.0",
    "author": "elastic",
    "category": "fleet",
    "tags": [
      "fleet"
    ],
    "parameters": [
      {
        "name": "category",
        "type": "STRING",
        "description": "Filter by category",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/fleet/list-integrations"
  },
  {
    "name": "create-jira-issue",
    "displayName": "Create Jira Issue",
    "description": "Create a Jira issue/ticket",
    "version": "1.0.0",
    "author": "elastic",
    "category": "integrations",
    "tags": [
      "integrations"
    ],
    "parameters": [
      {
        "name": "project",
        "type": "STRING",
        "description": "Jira project key",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/integrations/create-jira-issue"
  },
  {
    "name": "create-servicenow-incident",
    "displayName": "Create Servicenow Incident",
    "description": "Create a ServiceNow incident",
    "version": "1.0.0",
    "author": "elastic",
    "category": "integrations",
    "tags": [
      "integrations"
    ],
    "parameters": [
      {
        "name": "short_description",
        "type": "STRING",
        "description": "Incident title",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/integrations/create-servicenow-incident"
  },
  {
    "name": "invoke-aws-lambda",
    "displayName": "Invoke Aws Lambda",
    "description": "Invoke an AWS Lambda function",
    "version": "1.0.0",
    "author": "elastic",
    "category": "integrations",
    "tags": [
      "integrations"
    ],
    "parameters": [
      {
        "name": "function_name",
        "type": "STRING",
        "description": "Lambda function name",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/integrations/invoke-aws-lambda"
  },
  {
    "name": "send-email",
    "displayName": "Send Email",
    "description": "Send an email notification",
    "version": "1.0.0",
    "author": "elastic",
    "category": "integrations",
    "tags": [
      "integrations"
    ],
    "parameters": [
      {
        "name": "to",
        "type": "STRING",
        "description": "Recipient email address",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/integrations/send-email"
  },
  {
    "name": "send-opsgenie-alert",
    "displayName": "Send Opsgenie Alert",
    "description": "Create an OpsGenie alert",
    "version": "1.0.0",
    "author": "elastic",
    "category": "integrations",
    "tags": [
      "integrations"
    ],
    "parameters": [
      {
        "name": "message",
        "type": "STRING",
        "description": "Alert message",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/integrations/send-opsgenie-alert"
  },
  {
    "name": "send-slack-message",
    "displayName": "Send Slack Message",
    "description": "Send a message to a Slack channel",
    "version": "1.0.0",
    "author": "elastic",
    "category": "integrations",
    "tags": [
      "integrations"
    ],
    "parameters": [
      {
        "name": "channel",
        "type": "STRING",
        "description": "Slack channel name or ID",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/integrations/send-slack-message"
  },
  {
    "name": "send-teams-message",
    "displayName": "Send Teams Message",
    "description": "Send a Microsoft Teams message",
    "version": "1.0.0",
    "author": "elastic",
    "category": "integrations",
    "tags": [
      "integrations"
    ],
    "parameters": [
      {
        "name": "webhook_url",
        "type": "STRING",
        "description": "Teams webhook URL",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/integrations/send-teams-message"
  },
  {
    "name": "send-webhook",
    "displayName": "Send Webhook",
    "description": "Send a webhook POST request",
    "version": "1.0.0",
    "author": "elastic",
    "category": "integrations",
    "tags": [
      "integrations"
    ],
    "parameters": [
      {
        "name": "url",
        "type": "STRING",
        "description": "Webhook URL",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/integrations/send-webhook"
  },
  {
    "name": "trigger-github-workflow",
    "displayName": "Trigger Github Workflow",
    "description": "Trigger a GitHub Actions workflow",
    "version": "1.0.0",
    "author": "elastic",
    "category": "integrations",
    "tags": [
      "integrations"
    ],
    "parameters": [
      {
        "name": "repo",
        "type": "STRING",
        "description": "Repository owner/name",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/integrations/trigger-github-workflow"
  },
  {
    "name": "trigger-pagerduty",
    "displayName": "Trigger Pagerduty",
    "description": "Trigger a PagerDuty incident",
    "version": "1.0.0",
    "author": "elastic",
    "category": "integrations",
    "tags": [
      "integrations"
    ],
    "parameters": [
      {
        "name": "service_key",
        "type": "STRING",
        "description": "PagerDuty service key",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/integrations/trigger-pagerduty"
  },
  {
    "name": "alerting",
    "displayName": "Alerting",
    "description": "Alerting skill for kibana",
    "version": "1.0.0",
    "author": "moltler",
    "category": "kibana",
    "tags": [
      "kibana"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/kibana/alerting"
  },
  {
    "name": "apm",
    "displayName": "Apm",
    "description": "Apm skill for kibana",
    "version": "1.0.0",
    "author": "moltler",
    "category": "kibana",
    "tags": [
      "kibana"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/kibana/apm"
  },
  {
    "name": "cases",
    "displayName": "Cases",
    "description": "Cases skill for kibana",
    "version": "1.0.0",
    "author": "moltler",
    "category": "kibana",
    "tags": [
      "kibana"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/kibana/cases"
  },
  {
    "name": "connectors",
    "displayName": "Connectors",
    "description": "Connectors skill for kibana",
    "version": "1.0.0",
    "author": "moltler",
    "category": "kibana",
    "tags": [
      "kibana"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/kibana/connectors"
  },
  {
    "name": "dashboards",
    "displayName": "Dashboards",
    "description": "Dashboards skill for kibana",
    "version": "1.0.0",
    "author": "moltler",
    "category": "kibana",
    "tags": [
      "kibana"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/kibana/dashboards"
  },
  {
    "name": "data-views",
    "displayName": "Data Views",
    "description": "Data Views skill for kibana",
    "version": "1.0.0",
    "author": "moltler",
    "category": "kibana",
    "tags": [
      "kibana"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/kibana/data-views"
  },
  {
    "name": "detections",
    "displayName": "Detections",
    "description": "Detections skill for kibana",
    "version": "1.0.0",
    "author": "moltler",
    "category": "kibana",
    "tags": [
      "kibana"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/kibana/detections"
  },
  {
    "name": "entity-store",
    "displayName": "Entity Store",
    "description": "Entity Store skill for kibana",
    "version": "1.0.0",
    "author": "moltler",
    "category": "kibana",
    "tags": [
      "kibana"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/kibana/entity-store"
  },
  {
    "name": "fleet",
    "displayName": "Fleet",
    "description": "Fleet skill for kibana",
    "version": "1.0.0",
    "author": "moltler",
    "category": "kibana",
    "tags": [
      "kibana"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/kibana/fleet"
  },
  {
    "name": "ml",
    "displayName": "Ml",
    "description": "Ml skill for kibana",
    "version": "1.0.0",
    "author": "moltler",
    "category": "kibana",
    "tags": [
      "kibana"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/kibana/ml"
  },
  {
    "name": "saved-objects",
    "displayName": "Saved Objects",
    "description": "Saved Objects skill for kibana",
    "version": "1.0.0",
    "author": "moltler",
    "category": "kibana",
    "tags": [
      "kibana"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/kibana/saved-objects"
  },
  {
    "name": "slo",
    "displayName": "Slo",
    "description": "Slo skill for kibana",
    "version": "1.0.0",
    "author": "moltler",
    "category": "kibana",
    "tags": [
      "kibana"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/kibana/slo"
  },
  {
    "name": "spaces",
    "displayName": "Spaces",
    "description": "Spaces skill for kibana",
    "version": "1.0.0",
    "author": "moltler",
    "category": "kibana",
    "tags": [
      "kibana"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/kibana/spaces"
  },
  {
    "name": "synthetics",
    "displayName": "Synthetics",
    "description": "Synthetics skill for kibana",
    "version": "1.0.0",
    "author": "moltler",
    "category": "kibana",
    "tags": [
      "kibana"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/kibana/synthetics"
  },
  {
    "name": "explain-skill",
    "displayName": "Explain Skill",
    "description": "Get detailed explanation of a specific skill",
    "version": "1.0.0",
    "author": "elastic",
    "category": "meta",
    "tags": [
      "discovery"
    ],
    "parameters": [
      {
        "name": "skill_name",
        "type": "STRING",
        "description": "Name of the skill to explain (e.g., count_logs_by_level)",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/meta/explain-skill"
  },
  {
    "name": "generate-skill",
    "displayName": "Generate Skill",
    "description": "Generate a new skill from natural language using Elasticsearch Inference API",
    "version": "1.0.0",
    "author": "elastic",
    "category": "meta",
    "tags": [
      "meta"
    ],
    "parameters": [
      {
        "name": "description",
        "type": "STRING",
        "description": "Natural language description of what you want the skill to do",
        "required": true
      },
      {
        "name": "skill_name",
        "type": "STRING",
        "description": "Name for the generated skill (auto-generated if not provided)",
        "required": true
      },
      {
        "name": "model_id",
        "type": "STRING",
        "description": "Inference endpoint to use",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/meta/generate-skill"
  },
  {
    "name": "get-related-skills",
    "displayName": "Get Related Skills",
    "description": "Find skills related to a given skill",
    "version": "1.0.0",
    "author": "elastic",
    "category": "meta",
    "tags": [
      "discovery"
    ],
    "parameters": [
      {
        "name": "skill_name",
        "type": "STRING",
        "description": "Name of the skill to find related skills for",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/meta/get-related-skills"
  },
  {
    "name": "list-all-skills",
    "displayName": "List All Skills",
    "description": "List all available skills with descriptions",
    "version": "1.0.0",
    "author": "elastic",
    "category": "meta",
    "tags": [
      "discovery"
    ],
    "parameters": [
      {
        "name": "category",
        "type": "STRING",
        "description": "Filter by category: meta, observability, search, security, ml, management. Leave empty for all categories.",
        "required": true
      },
      {
        "name": "include_details",
        "type": "BOOLEAN",
        "description": "Include parameter details for each skill",
        "required": true
      }
    ],
    "returns": "a",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/meta/list-all-skills"
  },
  {
    "name": "recommend-skills",
    "displayName": "Recommend Skills",
    "description": "Get skill recommendations based on context or goal",
    "version": "1.0.0",
    "author": "elastic",
    "category": "meta",
    "tags": [
      "discovery"
    ],
    "parameters": [
      {
        "name": "goal",
        "type": "STRING",
        "description": "Describe your goal or what you are investigating (e.g., investigate production incident, analyze user behavior)",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/meta/recommend-skills"
  },
  {
    "name": "search-skills",
    "displayName": "Search Skills",
    "description": "Search for skills by keyword or capability",
    "version": "1.0.0",
    "author": "elastic",
    "category": "meta",
    "tags": [
      "discovery"
    ],
    "parameters": [
      {
        "name": "query",
        "type": "STRING",
        "description": "Search query - describe what you want to do (e.g., analyze logs, check security)",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/meta/search-skills"
  },
  {
    "name": "validate-syntax",
    "displayName": "Validate Syntax",
    "description": "Validate skill syntax before installation",
    "version": "1.0.0",
    "author": "elastic",
    "category": "meta",
    "tags": [
      "meta"
    ],
    "parameters": [
      {
        "name": "skill_code",
        "type": "STRING",
        "description": "The complete skill code to validate (CREATE SKILL ... END SKILL)",
        "required": true
      }
    ],
    "returns": "detailed",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/meta/validate-syntax"
  },
  {
    "name": "what-can-i-do",
    "displayName": "What Can I Do",
    "description": "Discover what Moltler skills are available for your data",
    "version": "1.0.0",
    "author": "elastic",
    "category": "meta",
    "tags": [
      "meta"
    ],
    "parameters": [],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/meta/what-can-i-do"
  },
  {
    "name": "get-container-metrics",
    "displayName": "Get Container Metrics",
    "description": "Get metrics for Docker/Kubernetes containers",
    "version": "1.0.0",
    "author": "elastic",
    "category": "metrics",
    "tags": [
      "metrics"
    ],
    "parameters": [
      {
        "name": "container_id",
        "type": "STRING",
        "description": "Container ID or name",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/metrics/get-container-metrics"
  },
  {
    "name": "get-disk-usage",
    "displayName": "Get Disk Usage",
    "description": "Get disk usage for hosts showing available and used space",
    "version": "1.0.0",
    "author": "elastic",
    "category": "metrics",
    "tags": [
      "metrics"
    ],
    "parameters": [
      {
        "name": "hostname",
        "type": "STRING",
        "description": "Host name (optional)",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/metrics/get-disk-usage"
  },
  {
    "name": "get-host-metrics",
    "displayName": "Get Host Metrics",
    "description": "Get current CPU, memory, and disk metrics for a host",
    "version": "1.0.0",
    "author": "elastic",
    "category": "metrics",
    "tags": [
      "metrics"
    ],
    "parameters": [
      {
        "name": "hostname",
        "type": "STRING",
        "description": "Host name to check",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/metrics/get-host-metrics"
  },
  {
    "name": "get-memory-pressure",
    "displayName": "Get Memory Pressure",
    "description": "Find hosts with high memory pressure",
    "version": "1.0.0",
    "author": "elastic",
    "category": "metrics",
    "tags": [
      "metrics"
    ],
    "parameters": [
      {
        "name": "threshold",
        "type": "INT",
        "description": "Memory usage threshold percent",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/metrics/get-memory-pressure"
  },
  {
    "name": "get-network-metrics",
    "displayName": "Get Network Metrics",
    "description": "Get network throughput metrics (bytes in/out)",
    "version": "1.0.0",
    "author": "elastic",
    "category": "metrics",
    "tags": [
      "metrics"
    ],
    "parameters": [
      {
        "name": "hostname",
        "type": "STRING",
        "description": "Host name (optional)",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/metrics/get-network-metrics"
  },
  {
    "name": "list-hosts",
    "displayName": "List Hosts",
    "description": "List all monitored hosts with their current status",
    "version": "1.0.0",
    "author": "elastic",
    "category": "metrics",
    "tags": [
      "metrics"
    ],
    "parameters": [
      {
        "name": "status",
        "type": "STRING",
        "description": "Filter by status",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/metrics/list-hosts"
  },
  {
    "name": "classify-text",
    "displayName": "Classify Text",
    "description": "Classify text using a trained classification model",
    "version": "1.0.0",
    "author": "elastic",
    "category": "ml",
    "tags": [
      "ml"
    ],
    "parameters": [
      {
        "name": "text",
        "type": "STRING",
        "description": "Text to classify",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/ml/classify-text"
  },
  {
    "name": "detect-anomalies-realtime",
    "displayName": "Detect Anomalies Realtime",
    "description": "Analyze data in real-time for anomalies using statistical methods",
    "version": "1.0.0",
    "author": "elastic",
    "category": "ml",
    "tags": [
      "ml"
    ],
    "parameters": [
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Index to analyze",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/ml/detect-anomalies-realtime"
  },
  {
    "name": "embed-text",
    "displayName": "Embed Text",
    "description": "Generate vector embeddings for text using a deployed embedding model",
    "version": "1.0.0",
    "author": "elastic",
    "category": "ml",
    "tags": [
      "ml"
    ],
    "parameters": [
      {
        "name": "text",
        "type": "STRING",
        "description": "Text to generate embeddings for",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/ml/embed-text"
  },
  {
    "name": "explain-anomaly",
    "displayName": "Explain Anomaly",
    "description": "Get explanation for a specific anomaly",
    "version": "1.0.0",
    "author": "elastic",
    "category": "ml",
    "tags": [
      "ml"
    ],
    "parameters": [
      {
        "name": "job_id",
        "type": "STRING",
        "description": "ML job ID",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/ml/explain-anomaly"
  },
  {
    "name": "extract-entities",
    "displayName": "Extract Entities",
    "description": "Extract named entities from text using NER",
    "version": "1.0.0",
    "author": "elastic",
    "category": "ml",
    "tags": [
      "ml"
    ],
    "parameters": [
      {
        "name": "text",
        "type": "STRING",
        "description": "Text to extract entities from",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/ml/extract-entities"
  },
  {
    "name": "get-anomalies",
    "displayName": "Get Anomalies",
    "description": "Get detected anomalies from ML jobs. Use for anomaly detection and alerting.",
    "version": "1.0.0",
    "author": "elastic",
    "category": "ml",
    "tags": [
      "ml"
    ],
    "parameters": [
      {
        "name": "job_id",
        "type": "STRING",
        "description": "ML job ID to get anomalies for",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/ml/get-anomalies"
  },
  {
    "name": "get-influencers",
    "displayName": "Get Influencers",
    "description": "Get top influencers contributing to anomalies",
    "version": "1.0.0",
    "author": "elastic",
    "category": "ml",
    "tags": [
      "ml"
    ],
    "parameters": [
      {
        "name": "job_id",
        "type": "STRING",
        "description": "ML job ID",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/ml/get-influencers"
  },
  {
    "name": "get-job-status",
    "displayName": "Get Job Status",
    "description": "Get detailed status of an ML job",
    "version": "1.0.0",
    "author": "elastic",
    "category": "ml",
    "tags": [
      "ml"
    ],
    "parameters": [
      {
        "name": "job_id",
        "type": "STRING",
        "description": "ML job ID",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/ml/get-job-status"
  },
  {
    "name": "list-ml-jobs",
    "displayName": "List Ml Jobs",
    "description": "List all machine learning anomaly detection jobs with their status",
    "version": "1.0.0",
    "author": "elastic",
    "category": "ml",
    "tags": [
      "ml"
    ],
    "parameters": [
      {
        "name": "status",
        "type": "STRING",
        "description": "Filter by status: started, stopped, closed",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/ml/list-ml-jobs"
  },
  {
    "name": "list-trained-models",
    "displayName": "List Trained Models",
    "description": "List all trained ML models including NLP models and custom models",
    "version": "1.0.0",
    "author": "elastic",
    "category": "ml",
    "tags": [
      "ml"
    ],
    "parameters": [
      {
        "name": "type",
        "type": "STRING",
        "description": "Filter by model type",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/ml/list-trained-models"
  },
  {
    "name": "run-inference",
    "displayName": "Run Inference",
    "description": "Run inference using a deployed ML model for text classification, embedding, or NER",
    "version": "1.0.0",
    "author": "elastic",
    "category": "ml",
    "tags": [
      "ml"
    ],
    "parameters": [
      {
        "name": "model_id",
        "type": "STRING",
        "description": "Model ID to use for inference",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/ml/run-inference"
  },
  {
    "name": "compare-time-periods",
    "displayName": "Compare Time Periods",
    "description": "Compare metrics between two time periods",
    "version": "1.0.0",
    "author": "elastic",
    "category": "observability",
    "tags": [
      "observability"
    ],
    "parameters": [
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Index to compare",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/observability/compare-time-periods"
  },
  {
    "name": "correlate-logs",
    "displayName": "Correlate Logs",
    "description": "Find correlated log events across services",
    "version": "1.0.0",
    "author": "elastic",
    "category": "observability",
    "tags": [
      "observability"
    ],
    "parameters": [
      {
        "name": "trace_id",
        "type": "STRING",
        "description": "Trace ID to correlate",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/observability/correlate-logs"
  },
  {
    "name": "count-logs-by-level",
    "displayName": "Count Logs By Level",
    "description": "Count logs grouped by severity level",
    "version": "1.0.0",
    "author": "elastic",
    "category": "observability",
    "tags": [
      "logs"
    ],
    "parameters": [
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Index pattern to search (e.g., logs-*, logs-production-*)",
        "required": true
      },
      {
        "name": "time_range",
        "type": "STRING",
        "description": "Time range to analyze (e.g., 1h, 24h, 7d)",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/observability/count-logs-by-level"
  },
  {
    "name": "error-rate",
    "displayName": "Error Rate",
    "description": "Calculate error rate as percentage of total logs",
    "version": "1.0.0",
    "author": "elastic",
    "category": "observability",
    "tags": [
      "logs"
    ],
    "parameters": [
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Index pattern to search",
        "required": true
      },
      {
        "name": "service",
        "type": "STRING",
        "description": "Filter by service name (optional)",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/observability/error-rate"
  },
  {
    "name": "get-availability",
    "displayName": "Get Availability",
    "description": "Get availability percentage for a monitor",
    "version": "1.0.0",
    "author": "elastic",
    "category": "observability",
    "tags": [
      "observability"
    ],
    "parameters": [
      {
        "name": "monitor_id",
        "type": "STRING",
        "description": "Monitor ID",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/observability/get-availability"
  },
  {
    "name": "get-error-context",
    "displayName": "Get Error Context",
    "description": "Get logs before and after an error for context",
    "version": "1.0.0",
    "author": "elastic",
    "category": "observability",
    "tags": [
      "observability"
    ],
    "parameters": [
      {
        "name": "timestamp",
        "type": "STRING",
        "description": "Error timestamp",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/observability/get-error-context"
  },
  {
    "name": "get-log-patterns",
    "displayName": "Get Log Patterns",
    "description": "Identify common log patterns using ML categorization",
    "version": "1.0.0",
    "author": "elastic",
    "category": "observability",
    "tags": [
      "observability"
    ],
    "parameters": [
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Index to analyze",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/observability/get-log-patterns"
  },
  {
    "name": "get-metrics-summary",
    "displayName": "Get Metrics Summary",
    "description": "Get summary statistics for system metrics",
    "version": "1.0.0",
    "author": "elastic",
    "category": "observability",
    "tags": [
      "metrics"
    ],
    "parameters": [
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Index pattern for metrics data",
        "required": true
      },
      {
        "name": "metric_name",
        "type": "STRING",
        "description": "Specific metric to analyze (cpu, memory, latency) or NULL for all",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/observability/get-metrics-summary"
  },
  {
    "name": "get-monitor-status",
    "displayName": "Get Monitor Status",
    "description": "Get current status of a monitor",
    "version": "1.0.0",
    "author": "elastic",
    "category": "observability",
    "tags": [
      "observability"
    ],
    "parameters": [
      {
        "name": "monitor_id",
        "type": "STRING",
        "description": "Monitor ID",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/observability/get-monitor-status"
  },
  {
    "name": "get-recent-errors",
    "displayName": "Get Recent Errors",
    "description": "Get recent error logs with details",
    "version": "1.0.0",
    "author": "elastic",
    "category": "observability",
    "tags": [
      "logs"
    ],
    "parameters": [
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Index pattern to search",
        "required": true
      },
      {
        "name": "limit",
        "type": "INT",
        "description": "Maximum number of errors to return",
        "required": true
      },
      {
        "name": "service",
        "type": "STRING",
        "description": "Filter by service name (optional)",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/observability/get-recent-errors"
  },
  {
    "name": "get-slo-status",
    "displayName": "Get Slo Status",
    "description": "Get SLO (Service Level Objective) status for a service",
    "version": "1.0.0",
    "author": "elastic",
    "category": "observability",
    "tags": [
      "observability"
    ],
    "parameters": [
      {
        "name": "service",
        "type": "STRING",
        "description": "Service name",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/observability/get-slo-status"
  },
  {
    "name": "get-ssl-status",
    "displayName": "Get Ssl Status",
    "description": "Check SSL certificate status and expiry",
    "version": "1.0.0",
    "author": "elastic",
    "category": "observability",
    "tags": [
      "observability"
    ],
    "parameters": [
      {
        "name": "monitor_id",
        "type": "STRING",
        "description": "Monitor ID",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/observability/get-ssl-status"
  },
  {
    "name": "high-cpu-hosts",
    "displayName": "High Cpu Hosts",
    "description": "Find hosts with high CPU usage",
    "version": "1.0.0",
    "author": "elastic",
    "category": "observability",
    "tags": [
      "metrics"
    ],
    "parameters": [
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Index pattern for metrics data",
        "required": true
      },
      {
        "name": "threshold",
        "type": "INT",
        "description": "CPU usage threshold percentage (0-100)",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/observability/high-cpu-hosts"
  },
  {
    "name": "list-monitors",
    "displayName": "List Monitors",
    "description": "List all uptime/synthetic monitors",
    "version": "1.0.0",
    "author": "elastic",
    "category": "observability",
    "tags": [
      "observability"
    ],
    "parameters": [
      {
        "name": "status",
        "type": "STRING",
        "description": "Filter by status: up, down",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/observability/list-monitors"
  },
  {
    "name": "logs-by-service",
    "displayName": "Logs By Service",
    "description": "Get log volume breakdown by service",
    "version": "1.0.0",
    "author": "elastic",
    "category": "observability",
    "tags": [
      "logs"
    ],
    "parameters": [
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Index pattern to search",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/observability/logs-by-service"
  },
  {
    "name": "search-logs",
    "displayName": "Search Logs",
    "description": "Search logs by keyword or phrase",
    "version": "1.0.0",
    "author": "elastic",
    "category": "observability",
    "tags": [
      "logs"
    ],
    "parameters": [
      {
        "name": "query",
        "type": "STRING",
        "description": "Search query - keyword or phrase to find in logs",
        "required": true
      },
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Index pattern to search",
        "required": true
      },
      {
        "name": "limit",
        "type": "INT",
        "description": "Maximum results to return",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/observability/search-logs"
  },
  {
    "name": "service-health",
    "displayName": "Service Health",
    "description": "Get health summary for a specific service",
    "version": "1.0.0",
    "author": "elastic",
    "category": "observability",
    "tags": [
      "apm"
    ],
    "parameters": [
      {
        "name": "service",
        "type": "STRING",
        "description": "Service name to check",
        "required": true
      },
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Index pattern to search",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/observability/service-health"
  },
  {
    "name": "slow-requests",
    "displayName": "Slow Requests",
    "description": "Find slow HTTP requests or API calls",
    "version": "1.0.0",
    "author": "elastic",
    "category": "observability",
    "tags": [
      "apm"
    ],
    "parameters": [
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Index pattern to search",
        "required": true
      },
      {
        "name": "threshold_ms",
        "type": "INT",
        "description": "Duration threshold in milliseconds",
        "required": true
      },
      {
        "name": "limit",
        "type": "INT",
        "description": "Maximum results",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/observability/slow-requests"
  },
  {
    "name": "top-error-messages",
    "displayName": "Top Error Messages",
    "description": "Get the most frequent error messages",
    "version": "1.0.0",
    "author": "elastic",
    "category": "observability",
    "tags": [
      "logs"
    ],
    "parameters": [
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Index pattern to search",
        "required": true
      },
      {
        "name": "limit",
        "type": "INT",
        "description": "Number of top errors to return",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/observability/top-error-messages"
  },
  {
    "name": "aggregate-by-field",
    "displayName": "Aggregate By Field",
    "description": "Aggregate documents by a specific field",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Index pattern to aggregate",
        "required": true
      },
      {
        "name": "field",
        "type": "STRING",
        "description": "Field name to group by",
        "required": true
      },
      {
        "name": "limit",
        "type": "INT",
        "description": "Maximum number of buckets",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/aggregate-by-field"
  },
  {
    "name": "bulk-index",
    "displayName": "Bulk Index",
    "description": "Index multiple documents in bulk",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "index_name",
        "type": "STRING",
        "description": "Index name",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/bulk-index"
  },
  {
    "name": "cluster-health",
    "displayName": "Cluster Health",
    "description": "Get Elasticsearch cluster health status",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "cluster"
    ],
    "parameters": [
      {
        "name": "dummy",
        "type": "STRING",
        "description": "Unused parameter",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/cluster-health"
  },
  {
    "name": "count-documents",
    "displayName": "Count Documents",
    "description": "Count documents in an index with optional filter",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Index pattern to count",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/count-documents"
  },
  {
    "name": "create-document",
    "displayName": "Create Document",
    "description": "Create a new document in an index",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "index_name",
        "type": "STRING",
        "description": "Index name",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/create-document"
  },
  {
    "name": "create-index",
    "displayName": "Create Index",
    "description": "Create a new index with settings and mappings",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "index_name",
        "type": "STRING",
        "description": "Index name",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/create-index"
  },
  {
    "name": "date-histogram",
    "displayName": "Date Histogram",
    "description": "Aggregate documents by time intervals",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Index to aggregate",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/date-histogram"
  },
  {
    "name": "delete-document",
    "displayName": "Delete Document",
    "description": "Delete a document by ID",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "index_name",
        "type": "STRING",
        "description": "Index name",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/delete-document"
  },
  {
    "name": "delete-index",
    "displayName": "Delete Index",
    "description": "Delete an index",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "index_name",
        "type": "STRING",
        "description": "Index name to delete",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/delete-index"
  },
  {
    "name": "fuzzy-search",
    "displayName": "Fuzzy Search",
    "description": "Search with typo tolerance using fuzzy matching",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "query",
        "type": "STRING",
        "description": "Search query",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/fuzzy-search"
  },
  {
    "name": "get-document",
    "displayName": "Get Document",
    "description": "Get a single document by ID from an index",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "index_name",
        "type": "STRING",
        "description": "Name of the index containing the document",
        "required": true
      },
      {
        "name": "doc_id",
        "type": "STRING",
        "description": "Document ID to retrieve",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/get-document"
  },
  {
    "name": "get-field-stats",
    "displayName": "Get Field Stats",
    "description": "Get statistics for a numeric field",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Index pattern to analyze",
        "required": true
      },
      {
        "name": "field",
        "type": "STRING",
        "description": "Numeric field to calculate stats for",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/get-field-stats"
  },
  {
    "name": "get-index-stats",
    "displayName": "Get Index Stats",
    "description": "Get detailed statistics for an index",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "index_name",
        "type": "STRING",
        "description": "Index name to get stats for",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/get-index-stats"
  },
  {
    "name": "get-mapping",
    "displayName": "Get Mapping",
    "description": "Get field mappings for an index",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "index_name",
        "type": "STRING",
        "description": "Index name",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/get-mapping"
  },
  {
    "name": "get-sample-documents",
    "displayName": "Get Sample Documents",
    "description": "Get sample documents from an index",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Index pattern to sample from",
        "required": true
      },
      {
        "name": "limit",
        "type": "INT",
        "description": "Number of sample documents",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/get-sample-documents"
  },
  {
    "name": "get-transform-status",
    "displayName": "Get Transform Status",
    "description": "Get status of a transform",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "transform_id",
        "type": "STRING",
        "description": "Transform ID",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/get-transform-status"
  },
  {
    "name": "get-unique-values",
    "displayName": "Get Unique Values",
    "description": "Get unique values for a field in an index",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Index pattern to analyze",
        "required": true
      },
      {
        "name": "field",
        "type": "STRING",
        "description": "Field to get unique values for",
        "required": true
      },
      {
        "name": "limit",
        "type": "INT",
        "description": "Maximum unique values to return",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/get-unique-values"
  },
  {
    "name": "list-all-indices",
    "displayName": "List All Indices",
    "description": "List all indices with size and document count",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "pattern",
        "type": "STRING",
        "description": "Index pattern to filter",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/list-all-indices"
  },
  {
    "name": "list-data-streams",
    "displayName": "List Data Streams",
    "description": "List all data streams",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/list-data-streams"
  },
  {
    "name": "list-ilm-policies",
    "displayName": "List Ilm Policies",
    "description": "List Index Lifecycle Management policies",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/list-ilm-policies"
  },
  {
    "name": "list-indices",
    "displayName": "List Indices",
    "description": "List all indices in the cluster",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "pattern",
        "type": "STRING",
        "description": "Index name pattern (informational only)",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/list-indices"
  },
  {
    "name": "list-ingest-pipelines",
    "displayName": "List Ingest Pipelines",
    "description": "List all ingest pipelines",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/list-ingest-pipelines"
  },
  {
    "name": "list-transforms",
    "displayName": "List Transforms",
    "description": "List all data transforms",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/list-transforms"
  },
  {
    "name": "multi-field-search",
    "displayName": "Multi Field Search",
    "description": "Search across multiple fields simultaneously",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "query",
        "type": "STRING",
        "description": "Search query",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/multi-field-search"
  },
  {
    "name": "percentiles",
    "displayName": "Percentiles",
    "description": "Calculate percentile distributions for a numeric field",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Index to analyze",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/percentiles"
  },
  {
    "name": "recent-documents",
    "displayName": "Recent Documents",
    "description": "Get the most recently indexed documents",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Index pattern to query",
        "required": true
      },
      {
        "name": "limit",
        "type": "INT",
        "description": "Number of documents to return",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/recent-documents"
  },
  {
    "name": "reindex",
    "displayName": "Reindex",
    "description": "Copy documents from one index to another",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "source_index",
        "type": "STRING",
        "description": "Source index",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/reindex"
  },
  {
    "name": "search-documents",
    "displayName": "Search Documents",
    "description": "Full-text search across any Elasticsearch index",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "query",
        "type": "STRING",
        "description": "Search query - keywords or phrase to search for",
        "required": true
      },
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Index pattern to search (e.g., products-*, users-*)",
        "required": true
      },
      {
        "name": "limit",
        "type": "INT",
        "description": "Maximum results to return",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/search-documents"
  },
  {
    "name": "semantic-search",
    "displayName": "Semantic Search",
    "description": "Semantic/vector search using embeddings",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "query",
        "type": "STRING",
        "description": "Natural language query",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/semantic-search"
  },
  {
    "name": "set-alias",
    "displayName": "Set Alias",
    "description": "Create or update an index alias",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "alias_name",
        "type": "STRING",
        "description": "Alias name",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/set-alias"
  },
  {
    "name": "test-ingest-pipeline",
    "displayName": "Test Ingest Pipeline",
    "description": "Test an ingest pipeline with sample data",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "pipeline_id",
        "type": "STRING",
        "description": "Pipeline ID",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/test-ingest-pipeline"
  },
  {
    "name": "top-values",
    "displayName": "Top Values",
    "description": "Get top N most common values for a field",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Index to analyze",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/top-values"
  },
  {
    "name": "update-document",
    "displayName": "Update Document",
    "description": "Update an existing document",
    "version": "1.0.0",
    "author": "elastic",
    "category": "search",
    "tags": [
      "search"
    ],
    "parameters": [
      {
        "name": "index_name",
        "type": "STRING",
        "description": "Index name",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/update-document"
  },
  {
    "name": "create-case",
    "displayName": "Create Case",
    "description": "Create a new security investigation case",
    "version": "1.0.0",
    "author": "elastic",
    "category": "security",
    "tags": [
      "security"
    ],
    "parameters": [
      {
        "name": "title",
        "type": "STRING",
        "description": "Case title",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/security/create-case"
  },
  {
    "name": "failed-logins",
    "displayName": "Failed Logins",
    "description": "Get failed login attempts grouped by user or IP",
    "version": "1.0.0",
    "author": "elastic",
    "category": "security",
    "tags": [
      "security"
    ],
    "parameters": [
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Security events index pattern",
        "required": true
      },
      {
        "name": "group_by",
        "type": "STRING",
        "description": "Group by: user, source_ip, or both",
        "required": true
      },
      {
        "name": "limit",
        "type": "INT",
        "description": "Maximum results",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/security/failed-logins"
  },
  {
    "name": "get-authentication-summary",
    "displayName": "Get Authentication Summary",
    "description": "Get authentication success/failure summary",
    "version": "1.0.0",
    "author": "elastic",
    "category": "security",
    "tags": [
      "security"
    ],
    "parameters": [
      {
        "name": "time_range",
        "type": "STRING",
        "description": "Time range to analyze",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/security/get-authentication-summary"
  },
  {
    "name": "get-dns-queries",
    "displayName": "Get Dns Queries",
    "description": "Get DNS query events",
    "version": "1.0.0",
    "author": "elastic",
    "category": "security",
    "tags": [
      "security"
    ],
    "parameters": [
      {
        "name": "domain",
        "type": "STRING",
        "description": "Domain filter",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/security/get-dns-queries"
  },
  {
    "name": "get-file-events",
    "displayName": "Get File Events",
    "description": "Get file system events",
    "version": "1.0.0",
    "author": "elastic",
    "category": "security",
    "tags": [
      "security"
    ],
    "parameters": [
      {
        "name": "hostname",
        "type": "STRING",
        "description": "Host to analyze",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/security/get-file-events"
  },
  {
    "name": "get-host-risk-score",
    "displayName": "Get Host Risk Score",
    "description": "Get risk score for a host based on security events",
    "version": "1.0.0",
    "author": "elastic",
    "category": "security",
    "tags": [
      "security"
    ],
    "parameters": [
      {
        "name": "hostname",
        "type": "STRING",
        "description": "Hostname to check",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/security/get-host-risk-score"
  },
  {
    "name": "get-network-events",
    "displayName": "Get Network Events",
    "description": "Get network connection events",
    "version": "1.0.0",
    "author": "elastic",
    "category": "security",
    "tags": [
      "security"
    ],
    "parameters": [
      {
        "name": "source_ip",
        "type": "STRING",
        "description": "Source IP filter",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/security/get-network-events"
  },
  {
    "name": "get-process-events",
    "displayName": "Get Process Events",
    "description": "Get process execution events for threat hunting",
    "version": "1.0.0",
    "author": "elastic",
    "category": "security",
    "tags": [
      "security"
    ],
    "parameters": [
      {
        "name": "hostname",
        "type": "STRING",
        "description": "Host to analyze",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/security/get-process-events"
  },
  {
    "name": "get-risky-hosts",
    "displayName": "Get Risky Hosts",
    "description": "Get hosts with highest risk scores",
    "version": "1.0.0",
    "author": "elastic",
    "category": "security",
    "tags": [
      "security"
    ],
    "parameters": [
      {
        "name": "min_score",
        "type": "INT",
        "description": "Minimum risk score",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/security/get-risky-hosts"
  },
  {
    "name": "get-risky-users",
    "displayName": "Get Risky Users",
    "description": "Get users with highest risk scores",
    "version": "1.0.0",
    "author": "elastic",
    "category": "security",
    "tags": [
      "security"
    ],
    "parameters": [
      {
        "name": "min_score",
        "type": "INT",
        "description": "Minimum risk score",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/security/get-risky-users"
  },
  {
    "name": "get-security-alerts",
    "displayName": "Get Security Alerts",
    "description": "Get recent security alerts and detections",
    "version": "1.0.0",
    "author": "elastic",
    "category": "security",
    "tags": [
      "security"
    ],
    "parameters": [
      {
        "name": "severity",
        "type": "STRING",
        "description": "Filter by severity: critical, high, medium, low",
        "required": true
      },
      {
        "name": "limit",
        "type": "INT",
        "description": "Maximum alerts to return",
        "required": true
      },
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Security events index pattern",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/security/get-security-alerts"
  },
  {
    "name": "get-user-risk-score",
    "displayName": "Get User Risk Score",
    "description": "Get risk score for a user based on their activity",
    "version": "1.0.0",
    "author": "elastic",
    "category": "security",
    "tags": [
      "security"
    ],
    "parameters": [
      {
        "name": "username",
        "type": "STRING",
        "description": "Username to check",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/security/get-user-risk-score"
  },
  {
    "name": "hunt-ioc",
    "displayName": "Hunt Ioc",
    "description": "Hunt for an Indicator of Compromise (IP, hash, domain)",
    "version": "1.0.0",
    "author": "elastic",
    "category": "security",
    "tags": [
      "security"
    ],
    "parameters": [
      {
        "name": "ioc",
        "type": "STRING",
        "description": "IOC value to hunt for (IP, hash, domain)",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/security/hunt-ioc"
  },
  {
    "name": "list-cases",
    "displayName": "List Cases",
    "description": "List security investigation cases",
    "version": "1.0.0",
    "author": "elastic",
    "category": "security",
    "tags": [
      "security"
    ],
    "parameters": [
      {
        "name": "status",
        "type": "STRING",
        "description": "Filter by status: open, closed, in-progress",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/security/list-cases"
  },
  {
    "name": "list-detection-rules",
    "displayName": "List Detection Rules",
    "description": "List all security detection rules",
    "version": "1.0.0",
    "author": "elastic",
    "category": "security",
    "tags": [
      "security"
    ],
    "parameters": [
      {
        "name": "enabled",
        "type": "STRING",
        "description": "Filter: true or false",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/security/list-detection-rules"
  },
  {
    "name": "search-security-events",
    "displayName": "Search Security Events",
    "description": "Search security events with full-text query",
    "version": "1.0.0",
    "author": "elastic",
    "category": "security",
    "tags": [
      "security"
    ],
    "parameters": [
      {
        "name": "query",
        "type": "STRING",
        "description": "Search query",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/security/search-security-events"
  },
  {
    "name": "suspicious-activity",
    "displayName": "Suspicious Activity",
    "description": "Get recent suspicious or anomalous activity",
    "version": "1.0.0",
    "author": "elastic",
    "category": "security",
    "tags": [
      "security"
    ],
    "parameters": [
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Security events index pattern",
        "required": true
      },
      {
        "name": "limit",
        "type": "INT",
        "description": "Maximum events to return",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/security/suspicious-activity"
  },
  {
    "name": "threat-summary",
    "displayName": "Threat Summary",
    "description": "Get a summary of threats and security posture",
    "version": "1.0.0",
    "author": "elastic",
    "category": "security",
    "tags": [
      "security"
    ],
    "parameters": [
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Security events index pattern",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/security/threat-summary"
  },
  {
    "name": "user-activity",
    "displayName": "User Activity",
    "description": "Get activity timeline for a specific user",
    "version": "1.0.0",
    "author": "elastic",
    "category": "security",
    "tags": [
      "security"
    ],
    "parameters": [
      {
        "name": "username",
        "type": "STRING",
        "description": "Username to investigate",
        "required": true
      },
      {
        "name": "index_pattern",
        "type": "STRING",
        "description": "Security events index pattern",
        "required": true
      },
      {
        "name": "limit",
        "type": "INT",
        "description": "Maximum events to return",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/security/user-activity"
  },
  {
    "name": "create_elasticsearch_project",
    "displayName": "Create Es Project",
    "description": "Creates a new Elasticsearch serverless project and returns credentials",
    "version": "1.0.0",
    "author": "elastic",
    "category": "serverless",
    "tags": [
      "serverless"
    ],
    "parameters": [
      {
        "name": "api_key",
        "type": "STRING",
        "description": "",
        "required": true
      },
      {
        "name": "project_name",
        "type": "STRING",
        "description": "",
        "required": true
      },
      {
        "name": "region_id",
        "type": "STRING",
        "default": "aws-us-east-1",
        "description": "",
        "required": false
      },
      {
        "name": "alias",
        "type": "STRING",
        "default": "",
        "description": "",
        "required": false
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/serverless/create-es-project"
  },
  {
    "name": "create_security_project",
    "displayName": "Create Security Project",
    "description": "Creates a new Elastic Security serverless project for SIEM/threat detection",
    "version": "1.0.0",
    "author": "elastic",
    "category": "serverless",
    "tags": [
      "serverless"
    ],
    "parameters": [
      {
        "name": "api_key",
        "type": "STRING",
        "description": "",
        "required": true
      },
      {
        "name": "project_name",
        "type": "STRING",
        "description": "",
        "required": true
      },
      {
        "name": "region_id",
        "type": "STRING",
        "default": "aws-us-east-1",
        "description": "",
        "required": false
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/serverless/create-security-project"
  },
  {
    "name": "create_ip_traffic_filter",
    "displayName": "Create Traffic Filter",
    "description": "Creates an IP-based traffic filter to restrict access to serverless projects",
    "version": "1.0.0",
    "author": "elastic",
    "category": "serverless",
    "tags": [
      "serverless"
    ],
    "parameters": [
      {
        "name": "api_key",
        "type": "STRING",
        "description": "",
        "required": true
      },
      {
        "name": "filter_name",
        "type": "STRING",
        "description": "",
        "required": true
      },
      {
        "name": "allowed_ips",
        "type": "ARRAY",
        "description": "",
        "required": true
      },
      {
        "name": "region_id",
        "type": "STRING",
        "default": "aws-us-east-1",
        "description": "",
        "required": false
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/serverless/create-traffic-filter"
  },
  {
    "name": "get_serverless_project_status",
    "displayName": "Get Project Status",
    "description": "Gets the current status of a serverless project",
    "version": "1.0.0",
    "author": "elastic",
    "category": "serverless",
    "tags": [
      "serverless"
    ],
    "parameters": [
      {
        "name": "api_key",
        "type": "STRING",
        "description": "",
        "required": true
      },
      {
        "name": "project_id",
        "type": "STRING",
        "description": "",
        "required": true
      },
      {
        "name": "project_type",
        "type": "STRING",
        "default": "elasticsearch",
        "description": "",
        "required": false
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/serverless/get-project-status"
  },
  {
    "name": "list_serverless_projects",
    "displayName": "List Projects",
    "description": "Lists all serverless projects across Elasticsearch, Observability, and Security types",
    "version": "1.0.0",
    "author": "elastic",
    "category": "serverless",
    "tags": [
      "serverless"
    ],
    "parameters": [
      {
        "name": "api_key",
        "type": "STRING",
        "description": "",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/serverless/list-projects"
  },
  {
    "name": "list_serverless_regions",
    "displayName": "List Regions",
    "description": "Lists all available regions for serverless projects with cloud provider details",
    "version": "1.0.0",
    "author": "elastic",
    "category": "serverless",
    "tags": [
      "serverless"
    ],
    "parameters": [
      {
        "name": "api_key",
        "type": "STRING",
        "description": "",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/serverless/list-regions"
  },
  {
    "name": "serverless_health_check",
    "displayName": "Project Health Check",
    "description": "Performs a health check on all serverless projects and returns status summary",
    "version": "1.0.0",
    "author": "elastic",
    "category": "serverless",
    "tags": [
      "serverless"
    ],
    "parameters": [
      {
        "name": "api_key",
        "type": "STRING",
        "description": "",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/serverless/project-health-check"
  },
  {
    "name": "reset_project_credentials",
    "displayName": "Reset Credentials",
    "description": "Resets and returns new credentials for a serverless project",
    "version": "1.0.0",
    "author": "elastic",
    "category": "serverless",
    "tags": [
      "serverless"
    ],
    "parameters": [
      {
        "name": "api_key",
        "type": "STRING",
        "description": "",
        "required": true
      },
      {
        "name": "project_id",
        "type": "STRING",
        "description": "",
        "required": true
      },
      {
        "name": "project_type",
        "type": "STRING",
        "default": "elasticsearch",
        "description": "",
        "required": false
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/serverless/reset-credentials"
  },
  {
    "name": "install-esql-report-workflow",
    "displayName": "Install Esql Report Workflow",
    "description": "Install a scheduled ES|QL reporting workflow",
    "version": "1.0.0",
    "author": "elastic",
    "category": "workflows",
    "tags": [
      "workflows"
    ],
    "parameters": [
      {
        "name": "workflow_name",
        "type": "STRING",
        "description": "Name for this report workflow",
        "required": true
      },
      {
        "name": "esql_query",
        "type": "STRING",
        "description": "ES|QL query to run",
        "required": true
      },
      {
        "name": "schedule",
        "type": "STRING",
        "description": "Schedule interval (e.g., 1d, 6h, 30m)",
        "required": true
      },
      {
        "name": "kibana_url",
        "type": "STRING",
        "description": "Kibana URL",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/workflows/install-esql-report-workflow"
  },
  {
    "name": "install-ip-reputation-workflow",
    "displayName": "Install Ip Reputation Workflow",
    "description": "Install the IP Reputation Check workflow (checks IPs against AbuseIPDB)",
    "version": "1.0.0",
    "author": "elastic",
    "category": "workflows",
    "tags": [
      "workflows"
    ],
    "parameters": [
      {
        "name": "abuseipdb_api_key",
        "type": "STRING",
        "description": "Your AbuseIPDB API key (get from abuseipdb.com)",
        "required": true
      },
      {
        "name": "kibana_url",
        "type": "STRING",
        "description": "Kibana URL",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/workflows/install-ip-reputation-workflow"
  },
  {
    "name": "install-slack-alert-workflow",
    "displayName": "Install Slack Alert Workflow",
    "description": "Install a Slack notification workflow for alerts",
    "version": "1.0.0",
    "author": "elastic",
    "category": "workflows",
    "tags": [
      "workflows"
    ],
    "parameters": [
      {
        "name": "slack_webhook_url",
        "type": "STRING",
        "description": "Slack webhook URL (get from Slack app settings)",
        "required": true
      },
      {
        "name": "default_channel",
        "type": "STRING",
        "description": "Default Slack channel",
        "required": true
      },
      {
        "name": "kibana_url",
        "type": "STRING",
        "description": "Kibana URL",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/workflows/install-slack-alert-workflow"
  },
  {
    "name": "install-workflow",
    "displayName": "Install Workflow",
    "description": "Install a new Elastic Workflow from YAML definition",
    "version": "1.0.0",
    "author": "elastic",
    "category": "workflows",
    "tags": [
      "workflows"
    ],
    "parameters": [
      {
        "name": "yaml",
        "type": "STRING",
        "description": "Workflow YAML definition",
        "required": true
      },
      {
        "name": "kibana_url",
        "type": "STRING",
        "description": "Kibana URL (defaults to KIBANA_URL env var or localhost:5601)",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/workflows/install-workflow"
  },
  {
    "name": "list-workflows",
    "displayName": "List Workflows",
    "description": "List all available Elastic Workflows in Kibana",
    "version": "1.0.0",
    "author": "elastic",
    "category": "workflows",
    "tags": [
      "workflows"
    ],
    "parameters": [
      {
        "name": "kibana_url",
        "type": "STRING",
        "description": "Kibana URL (defaults to KIBANA_URL env var or localhost:5601)",
        "required": true
      }
    ],
    "returns": "workflow",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/workflows/list-workflows"
  },
  {
    "name": "trigger-workflow",
    "displayName": "Trigger Workflow",
    "description": "Trigger an Elastic Workflow by ID or name",
    "version": "1.0.0",
    "author": "elastic",
    "category": "workflows",
    "tags": [
      "workflows"
    ],
    "parameters": [
      {
        "name": "workflow_id",
        "type": "STRING",
        "description": "Workflow ID or name to trigger",
        "required": true
      },
      {
        "name": "inputs",
        "type": "DOCUMENT",
        "description": "Input parameters to pass to the workflow",
        "required": true
      },
      {
        "name": "kibana_url",
        "type": "STRING",
        "description": "Kibana URL (defaults to KIBANA_URL env var or localhost:5601)",
        "required": true
      }
    ],
    "returns": "execution",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/workflows/trigger-workflow"
  },
  {
    "name": "workflow-status",
    "displayName": "Workflow Status",
    "description": "Get the execution status of a workflow run",
    "version": "1.0.0",
    "author": "elastic",
    "category": "workflows",
    "tags": [
      "workflows"
    ],
    "parameters": [
      {
        "name": "execution_id",
        "type": "STRING",
        "description": "Workflow execution ID returned from trigger_workflow",
        "required": true
      },
      {
        "name": "kibana_url",
        "type": "STRING",
        "description": "Kibana URL (defaults to KIBANA_URL env var or localhost:5601)",
        "required": true
      }
    ],
    "returns": "step",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/workflows/workflow-status"
  },
  {
    "name": "sfdc_alert_stale_deals",
    "displayName": "Alert Stale Deals",
    "description": "Create a workflow that monitors for stale opportunities and sends alerts",
    "version": "1.0.0",
    "author": "moltler",
    "category": "sfdc",
    "tags": [
      "sfdc"
    ],
    "parameters": [
      {
        "name": "stale_days",
        "type": "INT",
        "description": "",
        "required": true
      },
      {
        "name": "slack_channel",
        "type": "STRING",
        "description": "Slack channel for alerts",
        "required": true
      },
      {
        "name": "min_amount",
        "type": "INT",
        "description": "",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/sfdc/alert-stale-deals"
  },
  {
    "name": "sfdc_case_volume_trends",
    "displayName": "Case Volume Trends",
    "description": "Analyze support case volume trends over time",
    "version": "1.0.0",
    "author": "moltler",
    "category": "sfdc",
    "tags": [
      "sfdc"
    ],
    "parameters": [
      {
        "name": "time_period",
        "type": "STRING",
        "description": "Time period: last_7_days, last_30_days, last_90_days",
        "required": true
      },
      {
        "name": "group_by",
        "type": "STRING",
        "description": "Time grouping: day, week, month",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/sfdc/case-volume-trends"
  },
  {
    "name": "sfdc_closing_this_month",
    "displayName": "Closing This Month",
    "description": "Get all opportunities expected to close this month",
    "version": "1.0.0",
    "author": "moltler",
    "category": "sfdc",
    "tags": [
      "sfdc"
    ],
    "parameters": [
      {
        "name": "owner",
        "type": "STRING",
        "description": "Filter by opportunity owner name",
        "required": true
      },
      {
        "name": "min_probability",
        "type": "INT",
        "description": "",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/sfdc/closing-this-month"
  },
  {
    "name": "sfdc_escalated_cases",
    "displayName": "Escalated Cases",
    "description": "Get all currently escalated support cases",
    "version": "1.0.0",
    "author": "moltler",
    "category": "sfdc",
    "tags": [
      "sfdc"
    ],
    "parameters": [
      {
        "name": "include_closed",
        "type": "BOOLEAN",
        "description": "Include recently closed escalated cases",
        "required": true
      },
      {
        "name": "days_back",
        "type": "INT",
        "description": "",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/sfdc/escalated-cases"
  },
  {
    "name": "sfdc_find_account",
    "displayName": "Find Account",
    "description": "Find Salesforce accounts by name, domain, or ID",
    "version": "1.0.0",
    "author": "moltler",
    "category": "sfdc",
    "tags": [
      "sfdc"
    ],
    "parameters": [
      {
        "name": "query",
        "type": "STRING",
        "description": "Search query - account name, domain, or Salesforce ID",
        "required": true
      },
      {
        "name": "limit",
        "type": "INT",
        "description": "",
        "required": true
      }
    ],
    "returns": "matching",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/sfdc/find-account"
  },
  {
    "name": "sfdc_find_case",
    "displayName": "Find Case",
    "description": "Find Salesforce cases by case number, subject, account, or ID",
    "version": "1.0.0",
    "author": "moltler",
    "category": "sfdc",
    "tags": [
      "sfdc"
    ],
    "parameters": [
      {
        "name": "query",
        "type": "STRING",
        "description": "Search query - case number, subject keywords, account name, or Salesforce ID",
        "required": true
      },
      {
        "name": "status",
        "type": "STRING",
        "description": "Filter by status (e.g., ",
        "required": true
      },
      {
        "name": "priority",
        "type": "STRING",
        "description": "Filter by priority (e.g., ",
        "required": true
      },
      {
        "name": "limit",
        "type": "INT",
        "description": "",
        "required": true
      }
    ],
    "returns": "matching",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/sfdc/find-case"
  },
  {
    "name": "sfdc_find_opportunity",
    "displayName": "Find Opportunity",
    "description": "Find Salesforce opportunities by name, account, stage, or ID",
    "version": "1.0.0",
    "author": "moltler",
    "category": "sfdc",
    "tags": [
      "sfdc"
    ],
    "parameters": [
      {
        "name": "query",
        "type": "STRING",
        "description": "Search query - opportunity name, account name, or Salesforce ID",
        "required": true
      },
      {
        "name": "stage",
        "type": "STRING",
        "description": "Filter by stage (e.g., ",
        "required": true
      },
      {
        "name": "limit",
        "type": "INT",
        "description": "",
        "required": true
      }
    ],
    "returns": "matching",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/sfdc/find-opportunity"
  },
  {
    "name": "sfdc_notify_deal_closed",
    "displayName": "Notify Deal Closed",
    "description": "Send celebratory Slack notification when a deal is marked as Closed Won",
    "version": "1.0.0",
    "author": "moltler",
    "category": "sfdc",
    "tags": [
      "sfdc"
    ],
    "parameters": [
      {
        "name": "opportunity_id",
        "type": "STRING",
        "description": "Salesforce Opportunity ID that was just closed",
        "required": true
      },
      {
        "name": "slack_channel",
        "type": "STRING",
        "description": "Slack channel for celebration",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/sfdc/notify-deal-closed"
  },
  {
    "name": "sfdc_pipeline_summary",
    "displayName": "Pipeline Summary",
    "description": "Get current sales pipeline summary grouped by stage",
    "version": "1.0.0",
    "author": "moltler",
    "category": "sfdc",
    "tags": [
      "sfdc"
    ],
    "parameters": [
      {
        "name": "owner",
        "type": "STRING",
        "description": "Filter by opportunity owner name",
        "required": true
      },
      {
        "name": "min_amount",
        "type": "INT",
        "description": "",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/sfdc/pipeline-summary"
  },
  {
    "name": "sfdc_similar_accounts",
    "displayName": "Similar Accounts",
    "description": "Find accounts similar to a reference account using vector similarity",
    "version": "1.0.0",
    "author": "moltler",
    "category": "sfdc",
    "tags": [
      "sfdc"
    ],
    "parameters": [
      {
        "name": "account_id",
        "type": "STRING",
        "description": "Reference Salesforce Account ID to find similar accounts",
        "required": true
      },
      {
        "name": "limit",
        "type": "INT",
        "description": "",
        "required": true
      },
      {
        "name": "exclude_customers",
        "type": "BOOLEAN",
        "description": "Exclude existing customers (Type = Customer)",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/sfdc/similar-accounts"
  },
  {
    "name": "sfdc_stale_opportunities",
    "displayName": "Stale Opportunities",
    "description": "Find opportunities with no activity in N days",
    "version": "1.0.0",
    "author": "moltler",
    "category": "sfdc",
    "tags": [
      "sfdc"
    ],
    "parameters": [
      {
        "name": "stale_days",
        "type": "INT",
        "description": "",
        "required": true
      },
      {
        "name": "min_amount",
        "type": "INT",
        "description": "",
        "required": true
      },
      {
        "name": "owner",
        "type": "STRING",
        "description": "Filter by opportunity owner name",
        "required": true
      }
    ],
    "returns": "ARRAY",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/sfdc/stale-opportunities"
  },
  {
    "name": "sfdc_summarize_account",
    "displayName": "Summarize Account",
    "description": "Generate an AI-powered summary of an account relationship",
    "version": "1.0.0",
    "author": "moltler",
    "category": "sfdc",
    "tags": [
      "sfdc"
    ],
    "parameters": [
      {
        "name": "account_id",
        "type": "STRING",
        "description": "Salesforce Account ID",
        "required": true
      },
      {
        "name": "model_id",
        "type": "STRING",
        "description": "Inference endpoint for summarization",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/sfdc/summarize-account"
  },
  {
    "name": "sfdc_win_rate_analysis",
    "displayName": "Win Rate Analysis",
    "description": "Analyze win/loss rates by owner, lead source, or industry",
    "version": "1.0.0",
    "author": "moltler",
    "category": "sfdc",
    "tags": [
      "sfdc"
    ],
    "parameters": [
      {
        "name": "group_by",
        "type": "STRING",
        "description": "Group results by: owner, lead_source, industry, type",
        "required": true
      },
      {
        "name": "time_period",
        "type": "STRING",
        "description": "Time period: this_month, this_quarter, this_year, last_90_days",
        "required": true
      }
    ],
    "returns": "DOCUMENT",
    "sourceUrl": "https://github.com/bahaaldine/moltler/tree/main/hub/skills/sfdc/win-rate-analysis"
  }
];

export const SKILL_COUNT = 206;
