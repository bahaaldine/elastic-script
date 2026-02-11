# VSCode Extension

The Moltler extension for Visual Studio Code.

## Installation

1. Open VSCode
2. Go to Extensions (Ctrl+Shift+X)
3. Search for "Moltler"
4. Click Install

Or install from command line:

```bash
code --install-extension moltler.moltler-vscode
```

---

## Features

### Syntax Highlighting

Full syntax highlighting for elastic-script files (`.es`, `.escript`).

### IntelliSense

- **Auto-completion** for keywords, functions, and skills
- **Parameter hints** when calling skills
- **Documentation on hover**

### Diagnostics

- **Real-time syntax checking**
- **Type checking**
- **Linting and best practices**

### Code Navigation

- **Go to Definition** for skills and functions
- **Find References** across files
- **Symbol search** (Ctrl+T)

---

## Commands

Open Command Palette (Ctrl+Shift+P):

| Command | Description |
|---------|-------------|
| `Moltler: Run Current File` | Execute the current file |
| `Moltler: Run Selection` | Execute selected code |
| `Moltler: Create Skill` | Create a new skill from template |
| `Moltler: Test Skill` | Run tests for the current skill |
| `Moltler: Generate Skill` | Generate skill with AI |
| `Moltler: Show Skills` | List all skills |
| `Moltler: Connect` | Connect to Elasticsearch |

---

## Configuration

Open settings (Ctrl+,) and search for "Moltler":

```json
{
  "moltler.connection.host": "localhost:9200",
  "moltler.connection.username": "elastic-admin",
  "moltler.connection.password": "",
  "moltler.format.indentSize": 4,
  "moltler.lint.enabled": true,
  "moltler.ai.model": "gpt-4"
}
```

---

## Snippets

Type these prefixes and press Tab:

| Prefix | Snippet |
|--------|---------|
| `skill` | Create skill template |
| `proc` | Create procedure template |
| `func` | Create function template |
| `if` | If statement |
| `for` | For loop |
| `try` | Try/catch block |
| `cursor` | Cursor declaration |

Example:

```
skill<Tab>
```

Expands to:

```sql
CREATE SKILL ${1:skill_name}
VERSION '${2:1.0.0}'
DESCRIPTION '${3:Description}'
PARAMETERS (
    ${4:param} ${5:STRING}
)
RETURNS ${6:DOCUMENT}
BEGIN
    ${0:-- Implementation}
END SKILL;
```

---

## Debugging

### Set Breakpoints

Click in the gutter next to line numbers to set breakpoints.

### Debug Panel

1. Open Run and Debug (Ctrl+Shift+D)
2. Select "Moltler: Debug Skill"
3. Press F5 to start debugging

### Debug Features

- **Step Over** (F10)
- **Step Into** (F11)
- **Continue** (F5)
- **Watch variables**
- **Call stack**

---

## Testing

### Run Tests

Right-click on a skill file → "Run Skill Tests"

Or use the Test Explorer panel.

### Test Results

```
✓ basic functionality (12ms)
✓ handles empty input (8ms)
✗ handles special characters (15ms)
  Expected: status = 'success'
  Actual: status = 'error'
```

---

## AI Features

### Generate Skill

1. Open Command Palette
2. Run "Moltler: Generate Skill"
3. Enter description
4. Review and edit generated code

### Improve Skill

Right-click on skill → "Improve with AI"

Options:

- Add error handling
- Add tests
- Optimize performance
- Add documentation

### Explain Code

Select code → Right-click → "Explain Code"

---

## Workspace Features

### Skill Explorer

View all skills in the sidebar:

```
SKILLS
├── observability/
│   ├── check_health.es
│   ├── analyze_logs.es
│   └── detect_anomalies.es
├── integrations/
│   ├── github_sync.es
│   └── jira_create.es
└── agents/
    ├── incident_responder.es
    └── cost_optimizer.es
```

### Connector Browser

Browse connected data sources:

```
CONNECTORS
├── github_org
│   ├── issues
│   ├── pulls
│   └── repos
├── jira_project
│   ├── issues
│   └── sprints
└── datadog_prod
    ├── monitors
    └── events
```

Right-click to query or sync.

---

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+Enter` | Run selection/line |
| `Ctrl+Shift+Enter` | Run entire file |
| `F5` | Start debugging |
| `F12` | Go to definition |
| `Ctrl+Space` | Trigger IntelliSense |
| `Ctrl+Shift+O` | Go to symbol |

---

## What's Next?

<div class="grid cards" markdown>

-   :material-console:{ .lg .middle } __CLI Reference__

    Command-line interface.

    [:octicons-arrow-right-24: CLI](cli.md)

-   :material-book:{ .lg .middle } __Language Reference__

    Full elastic-script documentation.

    [:octicons-arrow-right-24: Language](../language/overview.md)

</div>
