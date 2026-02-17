# Moltler Skills Manager

A modern web UI for managing elastic-script skills (procedures and functions).

## Features

- **Skills Table**: View all skills with sorting, filtering, and pagination
- **Monaco Editor**: Full-featured code editor with syntax highlighting and autocomplete for elastic-script
- **Flyout Panels**: View skill details and edit code in slide-out panels
- **Dark/Light Mode**: Toggle between themes
- **Real-time Execution**: Run skills directly from the UI

## Tech Stack

- React 19 + TypeScript
- Vite (fast dev server and build)
- Tailwind CSS 4 + Radix UI components
- TanStack Table (powerful data tables)
- Monaco Editor (VS Code's editor)
- React Query (data fetching)

## Quick Start

### Using quick-start.sh (recommended)

```bash
# Full setup: Elasticsearch + demo data + Skills Manager UI
./scripts/quick-start.sh --moltler

# Or just start the UI (requires ES to be running)
./scripts/quick-start.sh --ui
```

### Manual Setup

```bash
# Navigate to the UI directory
cd moltler-ui

# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build
```

## Development

The UI runs on `http://localhost:3000` and proxies API requests to Elasticsearch on port 9200.

### API Endpoints

- `GET /api/skills` - List all skills
- `GET /api/skills/{name}` - Get skill details
- `POST /api/skills/{name}/_invoke` - Execute a skill
- `POST /api` - Execute raw elastic-script code

### Project Structure

```
moltler-ui/
├── src/
│   ├── api/              # API client
│   ├── components/
│   │   ├── skills/       # Skill-specific components
│   │   └── ui/           # Reusable UI components
│   ├── lib/              # Utilities
│   ├── App.tsx           # Main application
│   └── index.css         # Tailwind styles
├── index.html
├── vite.config.ts
└── package.json
```

## Screenshots

The Skills Manager provides:

1. **Table View**: Browse all skills with search and filters
2. **Detail Panel**: View skill information with syntax-highlighted code
3. **Editor**: Full Monaco editor with elastic-script language support
