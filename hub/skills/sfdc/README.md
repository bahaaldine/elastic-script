# Salesforce Skills Pack

**The definitive skills pack for Salesforce operations on Elasticsearch.**

Leverage your Salesforce data synced via the Elasticsearch Salesforce Connector with 25+ skills for sales, support, analytics, and AI-powered insights.

## Prerequisites

1. **Elasticsearch Salesforce Connector** configured and syncing data
2. **Moltler plugin** installed on your cluster
3. **Inference endpoint** (optional, for AI skills)

## Index Patterns

This pack assumes the following index patterns (configurable):

| SFDC Object | ES Index Pattern |
|-------------|------------------|
| Account | `sfdc-accounts-*` |
| Contact | `sfdc-contacts-*` |
| Opportunity | `sfdc-opportunities-*` |
| Case | `sfdc-cases-*` |
| Lead | `sfdc-leads-*` |
| Task | `sfdc-tasks-*` |
| Event | `sfdc-events-*` |
| Campaign | `sfdc-campaigns-*` |

## Skills Overview

### 🔍 Entity Lookup (5 skills)
| Skill | Description |
|-------|-------------|
| `sfdc_find_account` | Find account by name, domain, or ID |
| `sfdc_find_contact` | Find contact by name, email, or phone |
| `sfdc_find_opportunity` | Find opportunity by name, stage, or account |
| `sfdc_find_case` | Find case by number, subject, or account |
| `sfdc_find_lead` | Find lead by name, company, or email |

### 📊 Analytics (7 skills)
| Skill | Description |
|-------|-------------|
| `sfdc_pipeline_summary` | Current pipeline by stage with amounts |
| `sfdc_win_rate_analysis` | Win/loss rates by rep, product, or region |
| `sfdc_forecast_accuracy` | Compare forecasts to actuals |
| `sfdc_sales_velocity` | Average deal cycle time |
| `sfdc_top_accounts` | Highest revenue accounts |
| `sfdc_case_volume_trends` | Support case trends over time |
| `sfdc_lead_conversion_rate` | Lead to opportunity conversion |

### 🎯 Sales Operations (5 skills)
| Skill | Description |
|-------|-------------|
| `sfdc_stale_opportunities` | Opps with no activity in N days |
| `sfdc_closing_this_month` | Deals expected to close this month |
| `sfdc_at_risk_deals` | Deals past close date or stalled |
| `sfdc_rep_activity_summary` | Tasks/calls/meetings per rep |
| `sfdc_quota_attainment` | Rep performance vs quota |

### 🎫 Support Operations (4 skills)
| Skill | Description |
|-------|-------------|
| `sfdc_open_cases_by_priority` | Open cases grouped by priority |
| `sfdc_case_aging` | Cases by age bucket |
| `sfdc_escalated_cases` | Currently escalated cases |
| `sfdc_customer_case_history` | All cases for an account |

### 🤖 AI-Powered (4 skills)
| Skill | Description |
|-------|-------------|
| `sfdc_summarize_account` | AI summary of account relationship |
| `sfdc_deal_risk_score` | ML-based deal risk assessment |
| `sfdc_next_best_action` | AI-recommended next steps |
| `sfdc_similar_accounts` | Find similar accounts (vector search) |

### ⚡ Workflows (3 skills)
| Skill | Description |
|-------|-------------|
| `sfdc_alert_stale_deals` | Trigger alert for stale opportunities |
| `sfdc_escalate_case` | Create escalation workflow |
| `sfdc_notify_deal_closed` | Slack notification on closed won |

## Installation

```bash
cd hub
./moltler-cli.sh install --pack sfdc
```

## Quick Start

```sql
-- Find an account
RUN SKILL sfdc_find_account('Acme Corp');

-- Get pipeline summary
RUN SKILL sfdc_pipeline_summary();

-- Find at-risk deals
RUN SKILL sfdc_at_risk_deals(stale_days => 14);

-- AI account summary
RUN SKILL sfdc_summarize_account('0015000000ABC123');
```

## Configuration

Set these environment variables for customization:

| Variable | Default | Description |
|----------|---------|-------------|
| `SFDC_ACCOUNT_INDEX` | `sfdc-accounts-*` | Account index pattern |
| `SFDC_OPPORTUNITY_INDEX` | `sfdc-opportunities-*` | Opportunity index pattern |
| `SFDC_CASE_INDEX` | `sfdc-cases-*` | Case index pattern |
| `SFDC_INFERENCE_ENDPOINT` | `.elser-2` | Inference endpoint for AI skills |
