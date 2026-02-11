# GitHub Connector

Connect to GitHub to query repositories, issues, pull requests, and GitHub Actions.

## Setup

### Create Connector

```sql
CREATE CONNECTOR github_myorg
TYPE 'github'
CONFIG {
    "token": "{{secrets.github_token}}",
    "org": "mycompany"
};
```

### Required Permissions

Your GitHub token needs these scopes:

| Scope | Required For |
|-------|-------------|
| `repo` | Private repositories, issues, PRs |
| `read:org` | Organization info, teams |
| `actions` | GitHub Actions workflows |

### Configure Secret

```bash
moltler secrets set github_token ghp_xxxxxxxxxxxxxxxxxxxx
```

---

## Available Entities

### Repositories (`repos`)

```sql
SELECT name, full_name, private, stargazers_count, language
FROM github_myorg.repos
WHERE archived = false
ORDER BY updated_at DESC;
```

Fields:

| Field | Type | Description |
|-------|------|-------------|
| `id` | NUMBER | Repository ID |
| `name` | STRING | Repository name |
| `full_name` | STRING | Full name (org/repo) |
| `description` | STRING | Repository description |
| `private` | BOOLEAN | Is private |
| `archived` | BOOLEAN | Is archived |
| `language` | STRING | Primary language |
| `stargazers_count` | NUMBER | Star count |
| `forks_count` | NUMBER | Fork count |
| `created_at` | TIMESTAMP | Creation time |
| `updated_at` | TIMESTAMP | Last update time |
| `pushed_at` | TIMESTAMP | Last push time |

### Issues (`issues`)

```sql
SELECT number, title, state, labels, assignee, created_at
FROM github_myorg.issues
WHERE repo = 'myrepo'
  AND state = 'open'
  AND labels CONTAINS 'bug'
ORDER BY created_at DESC;
```

Fields:

| Field | Type | Description |
|-------|------|-------------|
| `number` | NUMBER | Issue number |
| `title` | STRING | Issue title |
| `body` | STRING | Issue body (markdown) |
| `state` | STRING | `open` or `closed` |
| `labels` | ARRAY | Label names |
| `assignee` | STRING | Assignee username |
| `assignees` | ARRAY | All assignees |
| `milestone` | STRING | Milestone title |
| `created_at` | TIMESTAMP | Creation time |
| `updated_at` | TIMESTAMP | Last update |
| `closed_at` | TIMESTAMP | Close time |
| `html_url` | STRING | Web URL |
| `repo` | STRING | Repository name |

### Pull Requests (`pulls`)

```sql
SELECT number, title, state, head_ref, base_ref, mergeable, author
FROM github_myorg.pulls
WHERE repo = 'myrepo'
  AND state = 'open'
ORDER BY created_at DESC;
```

Fields:

| Field | Type | Description |
|-------|------|-------------|
| `number` | NUMBER | PR number |
| `title` | STRING | PR title |
| `body` | STRING | PR description |
| `state` | STRING | `open`, `closed`, `merged` |
| `head_ref` | STRING | Source branch |
| `base_ref` | STRING | Target branch |
| `mergeable` | BOOLEAN | Can be merged |
| `draft` | BOOLEAN | Is draft |
| `author` | STRING | Author username |
| `reviewers` | ARRAY | Requested reviewers |
| `created_at` | TIMESTAMP | Creation time |
| `merged_at` | TIMESTAMP | Merge time |
| `html_url` | STRING | Web URL |

### GitHub Actions (`actions`)

```sql
SELECT name, status, conclusion, run_started_at, run_attempt
FROM github_myorg.actions
WHERE repo = 'myrepo'
  AND status = 'completed'
  AND conclusion = 'failure'
ORDER BY run_started_at DESC
LIMIT 10;
```

Fields:

| Field | Type | Description |
|-------|------|-------------|
| `id` | NUMBER | Run ID |
| `name` | STRING | Workflow name |
| `status` | STRING | `queued`, `in_progress`, `completed` |
| `conclusion` | STRING | `success`, `failure`, `cancelled` |
| `head_branch` | STRING | Branch |
| `head_sha` | STRING | Commit SHA |
| `run_number` | NUMBER | Run number |
| `run_attempt` | NUMBER | Attempt number |
| `run_started_at` | TIMESTAMP | Start time |
| `created_at` | TIMESTAMP | Creation time |
| `updated_at` | TIMESTAMP | Last update |

### Commits (`commits`)

```sql
SELECT sha, message, author_name, author_date
FROM github_myorg.commits
WHERE repo = 'myrepo'
  AND author_date > NOW() - INTERVAL '7d'
ORDER BY author_date DESC;
```

### Teams (`teams`)

```sql
SELECT name, slug, description, permission
FROM github_myorg.teams
WHERE permission = 'admin';
```

### Members (`members`)

```sql
SELECT login, name, email, role
FROM github_myorg.members
WHERE role = 'admin';
```

---

## Actions

### Create Issue

```sql
CONNECTOR_EXEC github_myorg.issues.create({
    "repo": "myrepo",
    "title": "New issue from Moltler",
    "body": "This issue was created automatically.",
    "labels": ["automated", "triage"]
});
```

### Update Issue

```sql
CONNECTOR_EXEC github_myorg.issues.update(
    repo => 'myrepo',
    issue_number => 123,
    data => {
        "state": "closed",
        "labels": ["resolved"]
    }
);
```

### Create Comment

```sql
CONNECTOR_EXEC github_myorg.issues.comment(
    repo => 'myrepo',
    issue_number => 123,
    body => 'Automated comment from Moltler'
);
```

### Trigger Workflow

```sql
CONNECTOR_EXEC github_myorg.actions.dispatch(
    repo => 'myrepo',
    workflow => 'deploy.yml',
    ref => 'main',
    inputs => {
        "environment": "staging"
    }
);
```

### Create PR

```sql
CONNECTOR_EXEC github_myorg.pulls.create({
    "repo": "myrepo",
    "title": "Automated PR",
    "head": "feature-branch",
    "base": "main",
    "body": "Created by Moltler"
});
```

### Merge PR

```sql
CONNECTOR_EXEC github_myorg.pulls.merge(
    repo => 'myrepo',
    pull_number => 456,
    merge_method => 'squash'
);
```

---

## Syncing

### Sync All Issues

```sql
SYNC CONNECTOR github_myorg.issues TO github-issues-*;
```

### Sync with Schedule

```sql
SYNC CONNECTOR github_myorg.issues TO github-issues-*
SCHEDULE '*/10 * * * *';  -- Every 10 minutes
```

### Sync Specific Repo

```sql
SYNC CONNECTOR github_myorg.issues TO github-issues-myrepo
WHERE repo = 'myrepo'
SCHEDULE '*/5 * * * *';
```

### Incremental Sync

```sql
SYNC CONNECTOR github_myorg.issues TO github-issues-*
INCREMENTAL ON updated_at
SCHEDULE '*/5 * * * *';
```

---

## Example Skills

### Skill: Find Stale PRs

```sql
CREATE SKILL find_stale_prs
VERSION '1.0.0'
DESCRIPTION 'Finds PRs that have been open for too long'
PARAMETERS (
    connector STRING,
    days_threshold NUMBER DEFAULT 7
)
RETURNS ARRAY
BEGIN
    DECLARE cutoff = NOW() - INTERVAL days_threshold || 'd';
    
    DECLARE stale_prs CURSOR FOR
        SELECT number, title, author, created_at, html_url
        FROM connector.pulls
        WHERE state = 'open'
          AND created_at < cutoff
        ORDER BY created_at;
    
    DECLARE results ARRAY = [];
    OPEN stale_prs;
    FOR pr IN stale_prs LOOP
        SET results = ARRAY_APPEND(results, {
            "number": pr.number,
            "title": pr.title,
            "author": pr.author,
            "age_days": DATE_DIFF('day', pr.created_at, NOW()),
            "url": pr.html_url
        });
    END LOOP;
    CLOSE stale_prs;
    
    RETURN results;
END SKILL;
```

### Skill: PR Review Reminder

```sql
CREATE SKILL remind_pr_reviews
VERSION '1.0.0'
DESCRIPTION 'Sends Slack reminders for PRs awaiting review'
PARAMETERS (
    github_connector STRING,
    slack_channel STRING
)
BEGIN
    DECLARE pending CURSOR FOR
        SELECT number, title, author, html_url, reviewers
        FROM github_connector.pulls
        WHERE state = 'open'
          AND draft = false
          AND ARRAY_LENGTH(reviewers) > 0;
    
    OPEN pending;
    FOR pr IN pending LOOP
        DECLARE msg = 'PR #' || pr.number || ' "' || pr.title || 
                      '" by @' || pr.author || ' needs review.\n' ||
                      pr.html_url;
        
        SLACK_SEND(slack_channel, msg);
    END LOOP;
    CLOSE pending;
END SKILL;
```

### Skill: Auto-Label Issues

```sql
CREATE SKILL auto_label_issues
VERSION '1.0.0'
DESCRIPTION 'Automatically labels new issues based on content'
PARAMETERS (
    github_connector STRING,
    repo STRING
)
BEGIN
    DECLARE unlabeled CURSOR FOR
        SELECT number, title, body
        FROM github_connector.issues
        WHERE repo = repo
          AND state = 'open'
          AND ARRAY_LENGTH(labels) = 0;
    
    OPEN unlabeled;
    FOR issue IN unlabeled LOOP
        DECLARE labels ARRAY = [];
        DECLARE content = LOWER(issue.title || ' ' || issue.body);
        
        IF content LIKE '%bug%' OR content LIKE '%error%' THEN
            SET labels = ARRAY_APPEND(labels, 'bug');
        END IF;
        
        IF content LIKE '%feature%' OR content LIKE '%enhancement%' THEN
            SET labels = ARRAY_APPEND(labels, 'enhancement');
        END IF;
        
        IF content LIKE '%documentation%' OR content LIKE '%docs%' THEN
            SET labels = ARRAY_APPEND(labels, 'documentation');
        END IF;
        
        IF ARRAY_LENGTH(labels) > 0 THEN
            CONNECTOR_EXEC github_connector.issues.update(
                repo => repo,
                issue_number => issue.number,
                data => {"labels": labels}
            );
        END IF;
    END LOOP;
    CLOSE unlabeled;
END SKILL;
```

---

## Configuration Options

```sql
CREATE CONNECTOR github_myorg
TYPE 'github'
CONFIG {
    "token": "{{secrets.github_token}}",
    "org": "mycompany",
    "enterprise_url": "https://github.mycompany.com/api/v3"  -- For GHE
}
OPTIONS {
    "rate_limit": 30,        -- Requests per minute
    "timeout_seconds": 30,   -- Request timeout
    "retry_count": 3,        -- Retry failed requests
    "cache_ttl": 300         -- Cache TTL in seconds
};
```

---

## Troubleshooting

### Rate Limiting

```
Error: GitHub API rate limit exceeded
```

Solution: Lower the rate limit or wait for reset:

```sql
-- Check rate limit status
SHOW CONNECTOR github_myorg STATUS;

-- Lower rate limit
ALTER CONNECTOR github_myorg
OPTIONS {"rate_limit": 10};
```

### Authentication Errors

```
Error: Bad credentials
```

Solution: Verify token and regenerate if needed:

```bash
moltler secrets set github_token NEW_TOKEN
```

### Missing Permissions

```
Error: Resource not accessible by integration
```

Solution: Update token scopes in GitHub settings.

---

## What's Next?

<div class="grid cards" markdown>

-   :material-jira:{ .lg .middle } __Jira Connector__

    Connect to Jira.

    [:octicons-arrow-right-24: Jira](jira.md)

-   :material-chart-line:{ .lg .middle } __Datadog Connector__

    Connect to Datadog.

    [:octicons-arrow-right-24: Datadog](datadog.md)

</div>
