# Scheduled Jobs

This notebook demonstrates how to create, manage, and monitor scheduled jobs in Elastic-Script.

## What are Scheduled Jobs?

Scheduled jobs allow you to run EScript procedures automatically based on cron-like schedules. This is useful for:

- **Periodic reports**: Generate and send daily/weekly summaries
- **Data cleanup**: Archive or delete old data on a schedule
- **Monitoring**: Run health checks at regular intervals
- **ETL processes**: Move and transform data between indices

## 1. Creating a Simple Job

Let's create a basic job that runs every minute and prints a message:

```sql
CREATE JOB hello_job
SCHEDULE '* * * * *'
AS BEGIN
    PRINT 'Hello from scheduled job at ' || NOW();
END JOB
```

## 2. Viewing Jobs

Use `SHOW JOBS` to list all scheduled jobs:

```sql
SHOW JOBS
```

View details of a specific job:

```sql
SHOW JOB hello_job
```

## 3. Schedule Formats

### Cron Expressions

Jobs use standard 5-field cron expressions:

```
┌───────────── minute (0 - 59)
│ ┌───────────── hour (0 - 23)
│ │ ┌───────────── day of month (1 - 31)
│ │ │ ┌───────────── month (1 - 12)
│ │ │ │ ┌───────────── day of week (0 - 6, Sunday = 0)
│ │ │ │ │
* * * * *
```

### Common Examples

| Expression | Description |
|------------|-------------|
| `* * * * *` | Every minute |
| `0 * * * *` | Every hour at :00 |
| `0 2 * * *` | Daily at 2:00 AM |
| `0 0 * * 0` | Weekly on Sunday at midnight |
| `0 0 1 * *` | Monthly on the 1st at midnight |
| `*/5 * * * *` | Every 5 minutes |
| `0 9-17 * * 1-5` | Every hour 9-5 on weekdays |

### Aliases

For convenience, you can use these aliases:

- `@hourly` - Run at the top of every hour
- `@daily` or `@midnight` - Run at midnight
- `@weekly` - Run at midnight on Sunday
- `@monthly` - Run at midnight on the 1st
- `@yearly` - Run at midnight on January 1st

```sql
-- Create a job that runs every hour
CREATE JOB hourly_report
SCHEDULE '@hourly'
DESCRIPTION 'Generates hourly system report'
AS BEGIN
    PRINT 'Running hourly report at ' || NOW();
END JOB
```

## 4. Jobs with Timezone Support

You can specify a timezone for your schedule. This is important for jobs that need to run at specific local times:

```sql
-- Run at 9 AM New York time on weekdays
CREATE JOB morning_briefing
SCHEDULE '0 9 * * 1-5'
TIMEZONE 'America/New_York'
DESCRIPTION 'Daily morning briefing'
AS BEGIN
    PRINT 'Good morning! Starting daily briefing...';
END JOB
```

## 5. Managing Jobs

### Disable a Job

Temporarily stop a job from running:

```sql
ALTER JOB hello_job DISABLE
```

### Enable a Job

```sql
ALTER JOB hello_job ENABLE
```

### Change Job Schedule

```sql
ALTER JOB hello_job SCHEDULE '*/2 * * * *'
```

## 6. Viewing Job History

See the execution history of a job:

```sql
SHOW JOB RUNS FOR hello_job
```

## 7. Cleanup

Delete jobs when no longer needed:

```sql
DROP JOB hello_job
```

## Summary

| Statement | Description |
|-----------|-------------|
| `CREATE JOB` | Define a new scheduled job with a cron schedule |
| `SHOW JOBS` | List all jobs or view specific job details |
| `ALTER JOB` | Enable, disable, or change schedule |
| `SHOW JOB RUNS` | View execution history |
| `DROP JOB` | Delete a job |

### Key Features

- **Cron expressions**: Full cron syntax with aliases
- **Timezone support**: Run jobs at specific local times
- **Complex logic**: Full EScript support in job body
- **Execution tracking**: Monitor job runs and status

---

[View the interactive notebook →](https://github.com/bahaaldine/elastic-script/blob/main/notebooks/06-scheduled-jobs.ipynb)
