---
layout: default
title: Skills Manager UI
---

# Moltler Skills Manager UI

A modern React-based web interface for managing elastic-script skills, procedures, and functions.

## Overview

The Skills Manager provides:
- **Browse** skills, procedures, and functions
- **View** implementation code and auto-generated documentation
- **Edit** with Monaco editor (VS Code-like experience)
- **Execute** skills directly from the UI

## Starting the UI

```bash
# As part of the full stack
./scripts/quick-start.sh --moltler

# Or standalone
cd moltler-ui
npm install
npm run dev
```

Access at: http://localhost:3000

## Features

### Tabbed Navigation

The UI organizes items into three tabs:
- **Skills** - AI-ready skills with MCP metadata
- **Procedures** - Reusable code blocks
- **Functions** - User-defined functions

### Skill Detail View

Click any item to open the detail flyout:

#### Implementation Tab
- Full source code in Monaco editor (read-only)
- Syntax highlighting for elastic-script
- Copy to clipboard

#### Documentation Tab
- Auto-generated markdown documentation
- Parameter tables
- Usage examples
- Rendered with proper formatting

### Editor

Create or edit items with:
- **Monaco Editor** - Full VS Code editing experience
- **Syntax Highlighting** - elastic-script keywords
- **Auto-completion** - Built-in functions and keywords
- **Code Templates** - Boilerplate for new items

### Execution

The **Invoke** button:
- Detects the item type from the code
- Generates appropriate CALL statement
- Executes and displays results
- Shows errors with helpful formatting

## Configuration

### Environment Variables

Create `.env.local`:

```bash
VITE_ES_URL=http://localhost:9200
VITE_ES_USERNAME=elastic-admin
VITE_ES_PASSWORD=elastic-password
```

### API Endpoints Used

| Endpoint | Purpose |
|----------|---------|
| `POST /_escript` | Execute elastic-script queries |
| `POST /_escript/mcp` | MCP tool operations |

## Development

### Stack

- **React 19** with TypeScript
- **Vite** for bundling
- **Tailwind CSS 4** for styling
- **Radix UI** for accessible components
- **TanStack Table** for data tables
- **Monaco Editor** for code editing
- **React Query** for data fetching

### Project Structure

```
moltler-ui/
├── src/
│   ├── api/          # API client
│   ├── components/
│   │   ├── skills/   # Skill-specific components
│   │   └── ui/       # Reusable UI components
│   └── App.tsx       # Main application
├── public/
└── package.json
```

### Building for Production

```bash
npm run build
# Output in dist/
```

### Key Components

#### SkillsTable

Data table with:
- Sortable columns
- Search/filter
- Click to view details

#### SkillDetail

Flyout panel showing:
- Header with name, type badge, description
- Parameters display
- Tabbed Implementation/Documentation view
- Action buttons (Delete, Execute, Edit)

#### SkillEditor

Full-screen editor with:
- Monaco code editor
- Create/Edit mode
- Execution with result display
- Templates for new items

## Screenshots

### Skills List
Browse all skills with their descriptions and metadata.

### Skill Detail
View implementation code and auto-generated documentation side by side.

### Editor
Edit skills with syntax highlighting and execute directly.

## Troubleshooting

### Empty Skills List
Verify Elasticsearch is running and has skills:
```bash
curl -u elastic-admin:elastic-password \
  http://localhost:9200/_escript -d '{"query": "SHOW SKILLS"}'
```

### Connection Errors
Check CORS settings if running on different port:
```yaml
http.cors.enabled: true
http.cors.allow-origin: "*"
```

### Execution Failures
- Check the underlying procedure exists
- Verify parameter types match
- Look for syntax errors in the output
