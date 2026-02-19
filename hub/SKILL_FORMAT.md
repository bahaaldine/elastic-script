# Skill Package Format

Every skill in MoltlerHub follows this standard format.

## Directory Structure

```
skill-name/
├── skill.yaml       # Required: Skill metadata
├── skill.sql        # Required: Skill implementation
├── README.md        # Required: Documentation
├── examples/        # Optional: Usage examples
│   └── example.sql
└── tests/           # Optional: Test cases
    └── test.sql
```

## skill.yaml Schema

```yaml
# Required fields
name: skill-name                    # Lowercase, hyphens allowed
version: 1.0.0                      # Semantic versioning
description: Short description      # One line, <100 chars
author: username                    # GitHub username or org

# Categorization
category: observability             # Primary category
tags:                               # Searchable tags
  - logs
  - analysis

# Optional metadata
license: Apache-2.0                 # SPDX license identifier
homepage: https://...               # Project homepage
repository: https://github.com/...  # Source repository

# Elasticsearch requirements
requirements:
  elasticsearch: ">=8.0.0"          # Minimum ES version
  features:                         # Required features
    - esql

# Dependencies on other skills
dependencies:
  "@elastic/esql-helpers": "^1.0.0"

# Entry point
main: skill.sql                     # Skill definition file
```

## Categories

| Category | Description |
|----------|-------------|
| `meta` | Skills about skills (discovery, recommendations) |
| `search` | Search and query operations |
| `observability` | Logs, metrics, APM, traces |
| `security` | SIEM, endpoint, threat detection |
| `ml` | Machine learning and anomaly detection |
| `management` | Cluster and index management |
| `integrations` | External service integrations |

## Example

### skill.yaml

```yaml
name: log-analyzer
version: 1.0.0
description: Analyze logs for errors and patterns
author: elastic
category: observability
tags:
  - logs
  - errors
  - analysis
license: Elastic-2.0
requirements:
  elasticsearch: ">=8.0.0"
  features:
    - esql
main: skill.sql
```

### skill.sql

```sql
CREATE SKILL log_analyzer
  VERSION '1.0.0'
  DESCRIPTION 'Analyze application logs for errors and patterns. Use this skill when investigating issues, checking error rates, or understanding log distribution.'
  AUTHOR 'elastic'
  TAGS ['observability', 'logs', 'errors', 'analysis']
  (
    index_pattern STRING DEFAULT 'logs-*' 
      DESCRIPTION 'Index pattern to search',
    time_range STRING DEFAULT '24h' 
      DESCRIPTION 'Time range to analyze (e.g., 1h, 24h, 7d)',
    min_level STRING DEFAULT 'WARN' 
      DESCRIPTION 'Minimum log level to include'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE error_count INT;
  DECLARE total_count INT;
  
  SET total_count = ESQL_QUERY('FROM ' || index_pattern || ' | STATS count=COUNT()')[0].count;
  SET error_count = ESQL_QUERY('FROM ' || index_pattern || ' | WHERE level == "ERROR" | STATS count=COUNT()')[0].count;
  
  RETURN {
    'total_logs': total_count,
    'error_count': error_count,
    'error_rate': ROUND((error_count * 100.0) / total_count, 2),
    'analyzed_at': CURRENT_TIMESTAMP()
  };
END SKILL;
```
