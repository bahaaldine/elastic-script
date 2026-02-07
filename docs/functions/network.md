# Network Functions

## Quick Reference

| Function | Description |
|----------|-------------|
| [`SLACK_LIST_CHANNELS`](#slack-list-channels) | Lists public channels in the Slack workspace. Useful for dis... |
| [`SLACK_REACT`](#slack-react) | Adds an emoji reaction to a message in Slack. |
| [`SLACK_SEND`](#slack-send) | Sends a message to a Slack channel using the Slack API.  |
| [`SLACK_SEND_BLOCKS`](#slack-send-blocks) | Sends a rich message with Block Kit blocks to a Slack channe... |
| [`SLACK_WEBHOOK`](#slack-webhook) | Sends a message to Slack via an incoming webhook URL.  |

---

## Function Details

### SLACK_LIST_CHANNELS

```
SLACK_LIST_CHANNELS(limit NUMBER, token STRING) -> ARRAY OF DOCUMENT
```

Lists public channels in the Slack workspace. Useful for discovering channel IDs.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `limit` | NUMBER | Maximum number of channels to return (default: 100) |
| `token` | STRING | Slack Bot token (optional) |

**Returns:** `ARRAY OF DOCUMENT`
 - Array of channel objects with id, name, and other metadata


**Examples:**

```sql
SLACK_LIST_CHANNELS()
SLACK_LIST_CHANNELS(50)
```

---

### SLACK_REACT

```
SLACK_REACT(channel STRING, timestamp STRING, emoji STRING, token STRING) -> BOOLEAN
```

Adds an emoji reaction to a message in Slack.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `channel` | STRING | Channel ID containing the message |
| `timestamp` | STRING | Message timestamp to react to |
| `emoji` | STRING | Emoji name without colons (e.g., 'thumbsup') |
| `token` | STRING | Slack Bot token (optional) |

**Returns:** `BOOLEAN`
 - true if reaction was added successfully


**Examples:**

```sql
SLACK_REACT('C1234567890', '1234567890.123456', 'white_check_mark')
```

---

### SLACK_SEND

```
SLACK_SEND(channel STRING, message STRING, thread_ts STRING, token STRING) -> DOCUMENT
```

Sends a message to a Slack channel using the Slack API. 

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `channel` | STRING | Channel ID or name (e.g., '#general' or 'C1234567890') |
| `message` | STRING | The message text to send |
| `thread_ts` | STRING | Optional thread timestamp to reply to a thread |
| `token` | STRING | Slack Bot token (optional, uses env var if not provided) |

**Returns:** `DOCUMENT`


**Examples:**

```sql
SLACK_SEND('#alerts', 'New error detected in production')
SLACK_SEND('C1234567890', 'Reply to thread', '1234567890.123456')
```

---

### SLACK_SEND_BLOCKS

```
SLACK_SEND_BLOCKS(channel STRING, blocks ARRAY OF DOCUMENT, fallback_text STRING, token STRING) -> DOCUMENT
```

Sends a rich message with Block Kit blocks to a Slack channel. 

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `channel` | STRING | Channel ID or name |
| `blocks` | ARRAY OF DOCUMENT | Array of Block Kit block objects |
| `fallback_text` | STRING | Fallback text for notifications |
| `token` | STRING | Slack Bot token (optional) |

**Returns:** `DOCUMENT`
 - Response containing ts and channel


**Examples:**

```sql
SLACK_SEND_BLOCKS('#alerts', [{\
: \
, \
: {\
: \
, \
: \
```

---

### SLACK_WEBHOOK

```
SLACK_WEBHOOK(webhook_url STRING, message STRING, username STRING, icon_emoji STRING) -> BOOLEAN
```

Sends a message to Slack via an incoming webhook URL. 

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `webhook_url` | STRING | The Slack incoming webhook URL |
| `message` | STRING | The message text to send |
| `username` | STRING | Optional display name for the message (default: webhook default) |
| `icon_emoji` | STRING | Optional emoji icon like ':robot_face:' (default: webhook default) |

**Returns:** `BOOLEAN`
 - true if message was sent successfully


**Examples:**

```sql
SLACK_WEBHOOK('https://hooks.slack.com/services/XXX', 'Alert: Server is down!')
SLACK_WEBHOOK(webhook_url, 'Deployment complete', 'DeployBot', ':rocket:')
```

---
