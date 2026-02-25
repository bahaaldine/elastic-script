# Moltler Skill Strategy: Simple vs Advanced

## Overview

The Moltler skill hub intentionally maintains **two tiers** of skills for many capabilities:

| Tier | Naming | ESQL Features | Target User |
|------|--------|---------------|-------------|
| **Simple** | `error_rate`, `date_histogram` | Basic STATS, COUNT, AVG, WHERE | Beginners, older ES versions, learning |
| **Advanced** | `error_rate_detailed`, `time_bucket_analysis` | CHANGE_POINT, INLINESTATS, CATEGORIZE, FORK, BUCKET | Power users, ES 9.1+, production |

## Why Both?

1. **Learning path** — Simple skills teach ESQL fundamentals. A user reading `aggregate_by_field` sees a clean `FROM | STATS | SORT | LIMIT` pipeline. The advanced version adds `COUNT_DISTINCT`, `TOP`, and richer statistics, but builds on the same pattern.

2. **Compatibility** — Advanced features like `CHANGE_POINT` (9.1+), `CATEGORIZE` (platinum), `INLINESTATS` (9.2+), and `FORK` (9.1+) require specific ES versions and license tiers. Simple skills work everywhere.

3. **Composability** — Simple skills are easier to embed in agents and workflows. Advanced skills return richer data but may be overkill for simple automation.

## Naming Convention

Advanced skills use descriptive names that hint at the technique, rather than just appending `_advanced`:

| Simple Skill | Advanced Counterpart | Key Upgrade |
|-------------|---------------------|-------------|
| `detect_anomalies` | `detect_change_points` | CHANGE_POINT for spike/dip/trend detection |
| `get_log_patterns` | `categorize_log_messages` | CATEGORIZE() for auto-grouping |
| `date_histogram` | `time_bucket_analysis` | BUCKET() grouping function |
| `detect_performance_regression` | `metric_baseline_comparison` | INLINESTATS for inline baseline |
| `error_rate` | `error_rate_detailed` | Single-query with WHERE on aggregation |
| `aggregate_by_field` | `field_cardinality_report` | COUNT_DISTINCT + TOP + SAMPLE |

## Metadata Linking

Each advanced skill's `skill.yaml` includes a `related_skills` field pointing to its simple counterpart, and vice versa. This helps AI agents recommend the right skill for the user's context:

```yaml
# In advanced skill's skill.yaml
related_skills:
  - name: detect_anomalies
    relationship: simple_version
    note: "Use for basic anomaly detection on older ES versions or when CHANGE_POINT is unavailable"

# In simple skill's skill.yaml (if updated)
related_skills:
  - name: detect_change_points
    relationship: advanced_version
    note: "Use for production-grade change detection with statistical confidence (requires ES 9.1+, platinum)"
```

## For AI Agents / LLM Assistants

When recommending skills:
- **Default to the simple version** if the user's ES version or license tier is unknown.
- **Recommend the advanced version** when the user explicitly needs richer analysis, is on ES 9.1+, or asks for features like change detection, auto-categorization, or inline statistics.
- **Explain the tradeoff** — the advanced skill returns more data but requires newer ES features.

## For Project Maintainers

When deciding whether to **replace** a simple skill with its advanced counterpart:
- If the simple skill is **broken** (e.g., ignores parameters, returns wrong data), fix it — that's a bug, not a simplicity choice.
- If the simple skill **works correctly** but uses basic ESQL, keep it alongside the advanced version.
- If you want a single skill, consider keeping the advanced one and noting the minimum ES version in `requirements`.

## ESQL Feature Requirements

| Feature | Min ES Version | License | Used In |
|---------|---------------|---------|---------|
| `CHANGE_POINT` | 9.1.0 | Platinum | detect-change-points |
| `CATEGORIZE()` | 9.1.0 | Platinum | categorize-log-messages |
| `INLINESTATS` / `INLINE STATS` | 9.2.0 | Basic | statistical-outlier-detection, metric-baseline-comparison |
| `FORK` / `FUSE` | 9.1.0 | Basic | multi-aggregation-fork |
| `BUCKET()` | 8.11.0 | Basic | time-bucket-analysis |
| `STD_DEV()` | 8.11.0 | Basic | statistical-outlier-detection, service-latency-analysis |
| `COUNT_DISTINCT()` | 8.11.0 | Basic | field-cardinality-report |
| `TOP()` | 8.14.0 | Basic | field-cardinality-report, aggregate-field-detailed |
| `WEIGHTED_AVG()` | 8.11.0 | Basic | service-latency-analysis |
| `MATCH()` | 8.17.0 | Basic | semantic-search, hybrid-search |
| WHERE on aggregation | 9.1.0 | Basic | error-rate-detailed |
