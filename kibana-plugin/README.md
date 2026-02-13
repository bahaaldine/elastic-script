# Moltler Kibana Plugin

Visual management interface for Moltler Skills in Kibana.

## Features

- **Skills List**: View all skills with name, description, version, and author
- **Skill Details**: Full skill definition, parameters, and return type
- **Run Skills**: Execute skills directly from Kibana with parameter inputs
- **Search & Filter**: Find skills by name, description, or tags

## Quick Start

### Prerequisites

- Node.js 18+ 
- yarn
- Elasticsearch running with elastic-script plugin

### Installation

```bash
# From the elastic-script root directory
cd kibana-plugin/scripts
./install-kibana-plugin.sh install
```

This will:
1. Clone the Kibana repository (shallow clone, ~2GB)
2. Link the Moltler plugin
3. Bootstrap Kibana dependencies (5-10 minutes first time)

### Running

```bash
# Terminal 1: Start Elasticsearch with elastic-script
cd elastic-script
./scripts/quick-start.sh

# Terminal 2: Start Kibana
cd elastic-script/kibana-plugin/kibana
yarn start
```

Open http://localhost:5601 and find **Moltler** in the sidebar under Management.

## Development

### Watch Mode

For active development with hot reload:

```bash
# Terminal 1: Plugin watch
cd kibana-plugin/plugins/moltler
yarn dev --watch

# Terminal 2: Kibana
cd kibana-plugin/kibana
yarn start
```

### Project Structure

```
kibana-plugin/
├── kibana/                    # Kibana source (git clone)
├── plugins/
│   └── moltler/               # The Moltler plugin
│       ├── public/            # Browser-side React code
│       │   ├── components/    # EUI components
│       │   │   ├── MoltlerApp.tsx
│       │   │   ├── SkillsList.tsx
│       │   │   └── SkillDetail.tsx
│       │   ├── services/      # API client
│       │   ├── plugin.ts      # Plugin registration
│       │   └── application.tsx # React app entry
│       ├── server/            # Server-side code
│       │   ├── routes/        # API routes
│       │   └── plugin.ts      # Server plugin
│       ├── common/            # Shared types
│       ├── kibana.json        # Plugin manifest
│       └── package.json
├── scripts/
│   └── install-kibana-plugin.sh
└── README.md
```

## API Routes

The plugin creates these server routes:

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/moltler/skills` | List all skills |
| GET | `/api/moltler/skills/:name` | Get skill details |
| POST | `/api/moltler/skills/:name/run` | Execute a skill |
| GET | `/api/moltler/indices` | List available indices |

## Roadmap

### Phase 1 (Current): Skills Viewer
- [x] Skills list with table view
- [x] Skill detail panel
- [x] Run skill with results
- [x] Search and filter

### Phase 2: Skills Editor
- [ ] Monaco editor with elastic-script syntax
- [ ] Autocomplete for keywords and functions
- [ ] Inline validation
- [ ] Create new skills
- [ ] Save and version skills

### Phase 3: Context Browser
- [ ] Browse indices with field info
- [ ] View connected workflows
- [ ] Agent status dashboard
- [ ] Connector health status

## Troubleshooting

### "Plugin not showing in Kibana"
- Ensure the symlink exists: `ls -la kibana/plugins/moltler`
- Check Kibana logs for plugin registration

### "Cannot connect to Elasticsearch"
- Verify ES is running: `curl http://localhost:9200`
- Check credentials in Kibana config

### "Skills list is empty"
- Run `moltler demo` to create sample skills
- Check `.escript_skills` index exists

## Built With

- [Elastic EUI](https://elastic.github.io/eui/) - UI Framework
- [React](https://reactjs.org/) - Frontend library
- [TypeScript](https://www.typescriptlang.org/) - Type safety
- [Kibana Plugin API](https://www.elastic.co/guide/en/kibana/current/kibana-plugins.html)
