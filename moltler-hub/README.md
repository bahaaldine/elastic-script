# MoltlerHub

**The Skills Marketplace for Elasticsearch.**

MoltlerHub is a web application for discovering, browsing, and sharing Moltler skills.

## Features

- 📦 **155+ Skills** - Browse all available skills
- 🔍 **Search** - Find skills by name, description, or tags
- 📂 **Categories** - Filter by Observability, Security, Search, and more
- 📄 **Skill Details** - Full documentation for each skill
- 🌙 **Dark Theme** - Modern, eye-friendly design

## Development

```bash
# Install dependencies
npm install

# Generate skills data from hub
node -e "$(cat scripts/generate-skills.ts)"

# Start dev server
npm run dev

# Build for production
npm run build
```

## Deployment

### Vercel (Recommended)

```bash
# Install Vercel CLI
npm i -g vercel

# Deploy
cd moltler-hub
vercel
```

### Static Export

```bash
# Add to next.config.ts:
# output: 'export'

npm run build
# Static files in ./out directory
```

### GitHub Pages

The app can be deployed to GitHub Pages using the workflow in `.github/workflows/`.

## Stack

- **Next.js 16** - React framework with App Router
- **TailwindCSS** - Styling
- **TypeScript** - Type safety
- **Static Generation** - All pages pre-rendered

## Data

Skills are loaded from `hub/skills/elastic/` at build time. Run the generate script to update:

```bash
npm run generate-skills
```

## Links

- [MoltlerHub Live](https://hub.moltler.dev) (coming soon)
- [Moltler Documentation](https://bahaaldine.github.io/moltler/)
- [GitHub Repository](https://github.com/bahaaldine/moltler)
