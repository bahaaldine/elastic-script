# Publishing Skills

Share your skills with your team or the community by publishing them.

## Publishing Options

| Destination | Visibility | Use Case |
|-------------|------------|----------|
| **Local** | Private | Personal development |
| **Team Registry** | Team | Internal sharing |
| **Organization** | Company | Enterprise deployment |
| **Marketplace** | Public | Community sharing |

---

## Prepare for Publishing

### 1. Complete Metadata

```sql
CREATE SKILL analyze_logs
VERSION '1.0.0'
DESCRIPTION 'Analyzes log patterns to detect anomalies'
AUTHOR 'sre-team'
TAGS ['observability', 'logs', 'analysis']
LICENSE 'MIT'
HOMEPAGE 'https://github.com/myorg/moltler-skills'
REPOSITORY 'https://github.com/myorg/moltler-skills'
BEGIN
    -- Implementation
END SKILL;
```

### 2. Write Documentation

```sql
CREATE SKILL analyze_logs
VERSION '1.0.0'
DOCUMENTATION '''
# Analyze Logs

This skill analyzes log patterns to detect anomalies in your data.

## Usage

```sql
CALL analyze_logs(
    index_pattern => 'logs-*',
    time_range => '24h'
);
```

## Parameters

| Name | Type | Default | Description |
|------|------|---------|-------------|
| index_pattern | STRING | 'logs-*' | Elasticsearch index pattern |
| time_range | STRING | '1h' | How far back to analyze |

## Examples

### Basic Usage
```sql
CALL analyze_logs();
```

### Custom Time Range
```sql
CALL analyze_logs(time_range => '7d');
```
'''
BEGIN
    -- Implementation
END SKILL;
```

### 3. Ensure Tests Pass

```sql
-- Run all tests
RUN TESTS FOR SKILL analyze_logs;

-- Check coverage
SHOW COVERAGE FOR SKILL analyze_logs;
```

### 4. Validate

```sql
VALIDATE SKILL analyze_logs FOR PUBLISHING;
```

Output:

```
Validating skill: analyze_logs v1.0.0

✓ Version is valid semver
✓ Description provided
✓ Author specified
✓ At least one tag
✓ All parameters documented
✓ Return type specified
✓ Documentation complete
✓ Tests pass (5/5)
✓ Coverage >= 80%
✓ No security issues detected

Skill is ready for publishing!
```

---

## Publish to Team Registry

### Configure Registry

```bash
moltler config set registry.team https://skills.mycompany.com
moltler config set registry.team.token $TEAM_TOKEN
```

### Publish

```sql
PUBLISH SKILL analyze_logs
TO 'team';
```

Or via CLI:

```bash
moltler publish analyze_logs --registry team
```

### Install from Team Registry

```sql
INSTALL SKILL analyze_logs FROM 'team';
```

---

## Publish to Marketplace

### Prerequisites

1. Create a Moltler account at https://moltler.dev
2. Generate an API token
3. Configure CLI:

```bash
moltler login
```

### Publish

```sql
PUBLISH SKILL analyze_logs
TO 'marketplace'
WITH visibility = 'public';
```

### Visibility Options

| Option | Description |
|--------|-------------|
| `public` | Anyone can discover and install |
| `unlisted` | Only accessible via direct link |
| `private` | Only your organization can access |

---

## Version Management

### Publish New Version

```sql
-- Create new version
CREATE SKILL analyze_logs
VERSION '1.1.0'  -- Bump version
DESCRIPTION 'Analyzes log patterns (now with AI!)'
BEGIN
    -- Updated implementation
END SKILL;

-- Publish
PUBLISH SKILL analyze_logs VERSION '1.1.0'
TO 'marketplace';
```

### Deprecate Old Versions

```sql
DEPRECATE SKILL analyze_logs VERSION '1.0.0'
MESSAGE 'Please upgrade to 1.1.0 for AI features';
```

### Unpublish

```sql
UNPUBLISH SKILL analyze_logs VERSION '1.0.0'
FROM 'marketplace';
```

---

## Skill Packs

Bundle related skills together:

```sql
CREATE SKILL PACK observability_essentials
VERSION '1.0.0'
DESCRIPTION 'Essential skills for observability workflows'
AUTHOR 'sre-team'
SKILLS [
    analyze_logs@1.0.0,
    detect_anomalies@2.0.0,
    create_alert@1.5.0,
    notify_oncall@1.0.0
];

PUBLISH SKILL PACK observability_essentials
TO 'marketplace';
```

Install a pack:

```sql
INSTALL SKILL PACK observability_essentials FROM 'marketplace';
```

---

## Marketplace Discovery

### Search Skills

```bash
moltler search "log analysis"
```

Output:

```
Found 12 skills matching "log analysis":

  analyze_logs (v1.1.0) by @sre-team
    Analyzes log patterns to detect anomalies
    ★★★★★ (4.8) | 1,234 installs | MIT
    
  log_aggregator (v2.0.0) by @observability-co
    Aggregates logs from multiple sources
    ★★★★☆ (4.2) | 567 installs | Apache-2.0
    
  smart_log_parser (v1.0.0) by @ai-ops
    AI-powered log parsing and classification
    ★★★★★ (4.9) | 890 installs | MIT

Use 'moltler show <skill>' for details.
```

### View Skill Details

```bash
moltler show analyze_logs
```

Output:

```
analyze_logs v1.1.0
by @sre-team

Analyzes log patterns to detect anomalies

Rating:   ★★★★★ (4.8 from 156 reviews)
Installs: 1,234
License:  MIT

Tags: observability, logs, analysis

Parameters:
  index_pattern  STRING  'logs-*'   Elasticsearch index pattern
  time_range     STRING  '1h'       Time range to analyze

Dependencies:
  - detect_anomalies@^2.0.0

Install:
  INSTALL SKILL analyze_logs FROM 'marketplace';
```

---

## Revenue Sharing

For premium skills:

```sql
PUBLISH SKILL enterprise_analytics
TO 'marketplace'
WITH visibility = 'public',
     pricing = 'paid',
     price_monthly = 9.99,
     price_yearly = 99.00;
```

Moltler takes a 20% platform fee; you receive 80%.

---

## Best Practices

### 1. Semantic Versioning

Follow semver strictly:

- **Major** (2.0.0) - Breaking changes
- **Minor** (1.1.0) - New features, backward compatible
- **Patch** (1.0.1) - Bug fixes only

### 2. Changelog

```sql
CREATE SKILL my_skill
VERSION '1.1.0'
CHANGELOG '''
## [1.1.0] - 2026-01-22
### Added
- AI-powered analysis option
- Support for multiple indices

### Fixed
- Timeout handling for large datasets

## [1.0.0] - 2026-01-01
### Added
- Initial release
'''
BEGIN
    -- Implementation
END SKILL;
```

### 3. Migration Guides

When making breaking changes:

```sql
CREATE SKILL my_skill
VERSION '2.0.0'
MIGRATION_NOTES '''
## Migrating from 1.x to 2.0

### Breaking Changes

1. Parameter `timeout` renamed to `timeout_seconds`
   - Before: `CALL my_skill(timeout => 30)`
   - After: `CALL my_skill(timeout_seconds => 30)`

2. Return type changed from STRING to DOCUMENT
   - Before: `"success"`
   - After: `{"status": "success", "details": {...}}`
'''
BEGIN
    -- Implementation
END SKILL;
```

### 4. Security Review

Before publishing:

- [ ] No hardcoded secrets
- [ ] Uses `ENV()` for sensitive config
- [ ] Validates all inputs
- [ ] Handles errors gracefully
- [ ] No SQL injection vulnerabilities

---

## What's Next?

<div class="grid cards" markdown>

-   :material-package-variant-closed:{ .lg .middle } __Skill Packs__

    Bundle skills together.

    [:octicons-arrow-right-24: Skill Packs](skill-packs.md)

-   :material-connection:{ .lg .middle } __Connectors__

    Add data sources to your skills.

    [:octicons-arrow-right-24: Connectors](../connectors/overview.md)

</div>
